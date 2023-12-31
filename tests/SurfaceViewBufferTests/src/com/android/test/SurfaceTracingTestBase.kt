/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.test

import android.annotation.ColorInt
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.util.Log
import androidx.test.ext.junit.rules.ActivityScenarioRule
import android.tools.common.flicker.subject.layers.LayerSubject
import android.tools.common.traces.surfaceflinger.LayersTrace
import android.tools.device.traces.io.ResultWriter
import android.tools.device.traces.monitors.surfaceflinger.LayersTraceMonitor
import android.tools.device.traces.monitors.withSFTracing
import junit.framework.Assert
import org.junit.After
import org.junit.Before
import org.junit.Rule
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.CountDownLatch

open class SurfaceTracingTestBase(useBlastAdapter: Boolean) :
        SurfaceViewBufferTestBase(useBlastAdapter) {
    @get:Rule
    var scenarioRule: ActivityScenarioRule<MainActivity> =
            ActivityScenarioRule<MainActivity>(MainActivity::class.java)

    @Before
    override fun setup() {
        super.setup()
        stopLayerTrace()
        addSurfaceView()
    }

    @After
    override fun teardown() {
        super.teardown()
        scenarioRule.getScenario().close()
    }

    fun withTrace(predicate: (it: MainActivity) -> Unit): LayersTrace {
        return withSFTracing(TRACE_FLAGS) {
            scenarioRule.getScenario().onActivity {
                predicate(it)
            }
        }
    }

    fun withTrace(predicate: () -> Unit): LayersTrace {
        return withSFTracing(TRACE_FLAGS) {
                predicate()
        }
    }

    fun runOnUiThread(predicate: (it: MainActivity) -> Unit) {
        scenarioRule.getScenario().onActivity {
            predicate(it)
        }
    }

    private fun addSurfaceView() {
        lateinit var surfaceReadyLatch: CountDownLatch
        scenarioRule.getScenario().onActivity {
            surfaceReadyLatch = it.addSurfaceView(defaultBufferSize)
        }
        surfaceReadyLatch.await()
        // sleep to finish animations
        instrumentation.waitForIdleSync()
    }

    private fun stopLayerTrace() {
        LayersTraceMonitor().stop(ResultWriter())
    }

    fun checkPixels(bounds: Rect, @ColorInt color: Int) {
        val screenshot = instrumentation.getUiAutomation().takeScreenshot()
        val pixels = IntArray(screenshot.width * screenshot.height)
        screenshot.getPixels(pixels, 0, screenshot.width, 0, 0, screenshot.width, screenshot.height)
        for (i in bounds.left + 10..bounds.right - 10) {
            for (j in bounds.top + 10..bounds.bottom - 10) {
                val actualColor = pixels[j * screenshot.width + i]
                if (actualColor != color) {
                    val screenshotPath = instrumentation.targetContext
                            .getExternalFilesDir(null)?.resolve("screenshot.png")
                    try {
                        FileOutputStream(screenshotPath).use { out ->
                            screenshot.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                        Log.e("SurfaceViewBufferTests", "Bitmap written to $screenshotPath")
                    } catch (e: IOException) {
                        Log.e("SurfaceViewBufferTests", "Error writing bitmap to file", e)
                    }
                }
                Assert.assertEquals("Checking $bounds found mismatch $i,$j",
                        Color.valueOf(color), Color.valueOf(actualColor))
            }
        }
    }

    private companion object {
        private const val TRACE_FLAGS =
                (1 shl 0) or (1 shl 5) or (1 shl 6) // TRACE_CRITICAL | TRACE_BUFFERS | TRACE_SYNC
    }
}
