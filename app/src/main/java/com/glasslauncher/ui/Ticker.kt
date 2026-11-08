package com.glasslauncher.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

/**
 * A value refreshed every [periodMs], aligned to the period, but only while the launcher is visible: when you're
 * in another app or the screen is off, clocks and widgets stop updating, so they cost no battery.
 */
@Composable
fun <T> rememberTicker(periodMs: Long, read: () -> T): State<T> {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val state = remember { mutableStateOf(read()) }
    LaunchedEffect(lifecycle, periodMs) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                state.value = read()
                delay(periodMs - System.currentTimeMillis() % periodMs)
            }
        }
    }
    return state
}
