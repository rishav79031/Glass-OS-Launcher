package com.glasslauncher.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Apple Taptic Engine curve simulation for Android:
 * Provides crisp, nuanced mechanical feedback for interactions (light ticks, firm clicks, selection detents).
 */
object AppleTaptic {
    fun light(context: Context) {
        vibratePredefined(context, VibrationEffect.EFFECT_TICK, 10)
    }

    fun medium(context: Context) {
        vibratePredefined(context, VibrationEffect.EFFECT_CLICK, 22)
    }

    fun heavy(context: Context) {
        vibratePredefined(context, VibrationEffect.EFFECT_HEAVY_CLICK, 40)
    }

    fun selection(context: Context) {
        vibratePredefined(context, VibrationEffect.EFFECT_TICK, 6)
    }

    private fun vibratePredefined(context: Context, effectId: Int, fallbackMs: Long) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator ?: (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
            } else {
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return

            if (!vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(effectId))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(fallbackMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(fallbackMs)
            }
        } catch (_: Exception) {}
    }
}
