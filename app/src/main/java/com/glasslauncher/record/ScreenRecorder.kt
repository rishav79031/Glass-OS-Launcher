package com.glasslauncher.record

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

/**
 * Screen recording for Control Centre. Android doesn't let apps start the phone's own recorder, so the launcher
 * records itself through MediaProjection: Android asks for consent each time, a notification shows while it runs,
 * and the video (screen only, no audio) is saved to Movies/Glass Launcher.
 */
object ScreenRecording {
    private val state = MutableStateFlow(false)
    val recording: StateFlow<Boolean> = state.asStateFlow()

    internal fun set(on: Boolean) {
        state.value = on
    }

    /** Starts (asking Android for consent first) or stops a recording. */
    fun toggle(context: Context) {
        if (state.value) {
            context.startService(Intent(context, ScreenRecordService::class.java).setAction(ScreenRecordService.ACTION_STOP))
        } else {
            context.startActivity(Intent(context, ScreenRecordConsentActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}

/** A see-through screen that only shows Android's "Start recording?" dialog, then hands the answer to the service. */
class ScreenRecordConsentActivity : ComponentActivity() {
    private val consent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val start = Intent(this, ScreenRecordService::class.java)
                .putExtra(ScreenRecordService.EXTRA_CODE, result.resultCode)
                .putExtra(ScreenRecordService.EXTRA_DATA, result.data)
            startForegroundService(start)
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) consent.launch(
            getSystemService(MediaProjectionManager::class.java).let { manager ->
                // Android 14+: ask for the whole screen directly, so the dialog is about recording, not casting one app.
                if (android.os.Build.VERSION.SDK_INT >= 34) manager.createScreenCaptureIntent(android.media.projection.MediaProjectionConfig.createConfigForDefaultDisplay())
                else manager.createScreenCaptureIntent()
            },
        )
    }
}

class ScreenRecordService : Service() {
    private var projection: MediaProjection? = null
    private var recorder: MediaRecorder? = null
    private var display: VirtualDisplay? = null
    private var output: Uri? = null
    private var file: ParcelFileDescriptor? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopRecording(saved = true)
            return START_NOT_STICKY
        }
        val code = intent?.getIntExtra(EXTRA_CODE, 0) ?: 0
        @Suppress("DEPRECATION")
        val data = intent?.getParcelableExtra<Intent>(EXTRA_DATA)
        if (data == null || recorder != null) return START_NOT_STICKY
        // Android requires the foreground notification before the projection is created.
        startForeground(NOTIFICATION_ID, notification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        if (!startRecording(code, data)) {
            Toast.makeText(this, "Couldn't start screen recording", Toast.LENGTH_SHORT).show()
            stopRecording(saved = false)
        }
        return START_NOT_STICKY
    }

    private fun startRecording(code: Int, data: Intent): Boolean = runCatching {
        val metrics = resources.displayMetrics
        // Up to 1080 pixels on the short side; encoders want even sizes.
        val scale = minOf(1f, 1080f / minOf(metrics.widthPixels, metrics.heightPixels))
        val width = (metrics.widthPixels * scale).roundToInt() / 2 * 2
        val height = (metrics.heightPixels * scale).roundToInt() / 2 * 2
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "Screen Recording ${System.currentTimeMillis()}.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/Glass Launcher")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return false
        output = uri
        val fd = contentResolver.openFileDescriptor(uri, "w") ?: return false
        file = fd
        val media = MediaRecorder(this).apply {
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setVideoSize(width, height)
            setVideoFrameRate(30)
            setVideoEncodingBitRate(8_000_000)
            setOutputFile(fd.fileDescriptor)
            prepare()
        }
        recorder = media
        val manager = getSystemService(MediaProjectionManager::class.java)
        val mediaProjection = manager.getMediaProjection(code, data) ?: return false
        projection = mediaProjection
        mediaProjection.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() = stopRecording(saved = true)
        }, Handler(Looper.getMainLooper()))
        display = mediaProjection.createVirtualDisplay(
            "Glass screen recording", width, height, metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, media.surface, null, null,
        )
        media.start()
        ScreenRecording.set(true)
        true
    }.getOrDefault(false)

    private fun stopRecording(saved: Boolean) {
        val wasRecording = recorder != null
        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        recorder = null
        runCatching { display?.release() }
        display = null
        val active = projection
        projection = null
        runCatching { active?.stop() }
        runCatching { file?.close() }
        file = null
        output?.let { uri ->
            if (saved && wasRecording) {
                contentResolver.update(uri, ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }, null, null)
                Toast.makeText(this, "Screen recording saved to Movies", Toast.LENGTH_SHORT).show()
            } else {
                runCatching { contentResolver.delete(uri, null, null) }
            }
        }
        output = null
        ScreenRecording.set(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun notification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Screen recording", NotificationManager.IMPORTANCE_LOW))
        val stop = PendingIntent.getService(
            this, 1, Intent(this, ScreenRecordService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.presence_video_online)
            .setContentTitle("Recording screen")
            .setContentText("Tap Stop to save it to Movies")
            .setOngoing(true)
            .addAction(Notification.Action.Builder(null, "Stop", stop).build())
            .setContentIntent(stop)
            .build()
    }

    companion object {
        const val ACTION_STOP = "com.glasslauncher.record.STOP"
        const val EXTRA_CODE = "code"
        const val EXTRA_DATA = "data"
        private const val CHANNEL = "screen_recording"
        private const val NOTIFICATION_ID = 7
    }
}
