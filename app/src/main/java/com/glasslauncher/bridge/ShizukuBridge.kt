package com.glasslauncher.bridge

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

data class CommandResult(val exitCode: Int, val output: String, val error: String)

/**
 * Privileged ADB / Shizuku bridge for controlling system toggles that Android blocks for standard apps:
 * - Airplane mode
 * - Wi-Fi
 * - Mobile data (Cellular Internet)
 * - Bluetooth
 * - Personal Hotspot
 */
object ShizukuBridge {
    private const val TAG = "ShizukuBridge"
    private const val REQUEST_CODE = 4040

    private val _isShizukuActive = MutableStateFlow(false)
    val isShizukuActive: StateFlow<Boolean> = _isShizukuActive.asStateFlow()

    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        checkStatus()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        _isShizukuActive.value = false
        _hasPermission.value = false
    }

    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == REQUEST_CODE) {
            _hasPermission.value = grantResult == PackageManager.PERMISSION_GRANTED
        }
    }

    private var initialized = false

    fun init() {
        if (initialized) return
        initialized = true
        runCatching {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
            checkStatus()
        }
    }

    fun checkStatus() {
        runCatching {
            val alive = Shizuku.pingBinder()
            _isShizukuActive.value = alive
            if (alive) {
                _hasPermission.value = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            } else {
                _hasPermission.value = false
            }
        }.onFailure {
            _isShizukuActive.value = false
            _hasPermission.value = false
        }
    }

    fun requestPermission() {
        runCatching {
            if (Shizuku.pingBinder()) {
                if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                    Shizuku.requestPermission(REQUEST_CODE)
                } else {
                    _hasPermission.value = true
                }
            }
        }
    }

    suspend fun runShell(command: String): CommandResult = withContext(Dispatchers.IO) {
        // Attempt Shizuku first if granted
        if (_hasPermission.value && Shizuku.pingBinder()) {
            return@withContext runShizukuProcess(command)
        }

        // Fallback to local process (works if rooted or app has debug permissions)
        runCatching {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val output = process.inputStream.bufferedReader().use(BufferedReader::readText)
            val error = process.errorStream.bufferedReader().use(BufferedReader::readText)
            val exitCode = process.waitFor()
            CommandResult(exitCode, output, error)
        }.getOrElse {
            CommandResult(-1, "", it.message ?: "Failed to execute")
        }
    }

    private fun runShizukuProcess(command: String): CommandResult {
        return try {
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java,
            ).apply { isAccessible = true }
            val process = method.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
            val output = process.inputStream.bufferedReader().use(BufferedReader::readText)
            val error = process.errorStream.bufferedReader().use(BufferedReader::readText)
            val exit = process.waitFor()
            CommandResult(exit, output, error)
        } catch (e: Exception) {
            Log.e(TAG, "Error executing Shizuku command: $command", e)
            CommandResult(-1, "", e.message ?: "Shizuku exec error")
        }
    }

    suspend fun setAirplaneMode(enable: Boolean): Boolean {
        val state = if (enable) "enable" else "disable"
        val flag = if (enable) "1" else "0"
        val bool = if (enable) "true" else "false"
        val cmd = "cmd connectivity airplane-mode $state; settings put global airplane_mode_on $flag; am broadcast -a android.intent.action.AIRPLANE_MODE --ez state $bool"
        val res = runShell(cmd)
        return res.exitCode == 0
    }

    suspend fun setWifi(enable: Boolean): Boolean {
        val state = if (enable) "enabled" else "disabled"
        val cmd = "cmd wifi set-wifi-enabled $state || svc wifi $state"
        val res = runShell(cmd)
        return res.exitCode == 0
    }

    suspend fun setMobileData(enable: Boolean): Boolean {
        val state = if (enable) "enable" else "disable"
        val cmd = "cmd phone data $state || svc data $state"
        val res = runShell(cmd)
        return res.exitCode == 0
    }

    suspend fun setBluetooth(enable: Boolean): Boolean {
        val state = if (enable) "enable" else "disable"
        val cmd = "cmd bluetooth $state || svc bluetooth $state"
        val res = runShell(cmd)
        return res.exitCode == 0
    }

    suspend fun setHotspot(enable: Boolean): Boolean {
        val cmd = if (enable) {
            "cmd connectivity start-tethering 0"
        } else {
            "cmd connectivity stop-tethering 0"
        }
        val res = runShell(cmd)
        return res.exitCode == 0
    }
}
