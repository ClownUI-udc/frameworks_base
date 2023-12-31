/*
 * Copyright (C) 2016 The Android Open Source Project
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

package android.perftests;

import android.perftests.utils.BenchmarkState;
import android.perftests.utils.PerfStatusReporter;

import androidx.test.filters.LargeTest;
import androidx.test.runner.AndroidJUnit4;

import dalvik.annotation.optimization.CriticalNative;
import dalvik.annotation.optimization.FastNative;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class SystemPerfTest {

    static {
        System.loadLibrary("perftestscore_jni");
    }

    @Rule
    public PerfStatusReporter mPerfStatusReporter = new PerfStatusReporter();

    @Test
    public void testNanoTimePerf() {
        BenchmarkState state = mPerfStatusReporter.getBenchmarkState();
        while (state.keepRunning()) {
            System.nanoTime();
        }
    }

    @Test
    public void testBenchmarkOverhead() {
        BenchmarkState state = mPerfStatusReporter.getBenchmarkState();
        while (state.keepRunning()) {}
    }

    void spinBlock(long durationNs) {
        long start = System.nanoTime();
        while (System.nanoTime() - start < durationNs) {}
    }

    @Test
    public void testBenchmarkPauseResumeOverhead() {
        BenchmarkState state = mPerfStatusReporter.getBenchmarkState();
        while (state.keepRunning()) {
            state.pauseTiming();
            spinBlock(TimeUnit.MICROSECONDS.toNanos(5));
            state.resumeTiming();
        }
    }

    @Test
    public void testJniArrayNoop() {
        BenchmarkState state = mPerfStatusReporter.getBenchmarkState();
        final int[] data = new int[450];
        while (state.keepRunning()) {
            jintarrayArgumentNoop(data, data.length);
        }
    }

    @Test
    public void testJniArrayGetLength() {
        BenchmarkState state = mPerfStatusReporter.getBenchmarkState();
        final int[] data = new int[450];
        while (state.keepRunning()) {
            jintarrayGetLength(data);
        }
    }

    @Test
    public void testJniArrayCriticalAccess() {
        BenchmarkState state = mPerfStatusReporter.getBenchmarkState();
        final int[] data = new int[450];
        while (state.keepRunning()) {
            jintarrayCriticalAccess(data, 50);
        }
    }

    @Test
    public void testJniArrayBasicAccess() {
        BenchmarkState state = mPerfStatusReporter.getBenchmarkState();
        final int[] data = new int[450];
        while (state.keepRunning()) {
            jintarrayBasicAccess(data, 50);
        }
    }

    /** this result should be compared with {@link #testJniCriticalNativeAccess()}. */
    @Test
    public void testJniFastNativeAccess() {
        BenchmarkState state = mPerfStatusReporter.getBenchmarkState();
        while (state.keepRunning()) {
            jintFastNativeAccess(50);
        }
    }

    /**
     * This result should be compared with {@link #testJniFastNativeAccess()}.
     *
     * <p>In theory, the result should be better than {@link #testJniFastNativeAccess()}.
     */
    @Test
    public void testJniCriticalNativeAccess() {
        BenchmarkState state = mPerfStatusReporter.getBenchmarkState();
        while (state.keepRunning()) {
            jintCriticalNativeAccess(50);
        }
    }

    /** The result should be compared with {@link #testJniCriticalNativeCheckNullPointer()}. */
    @Test
    public void testJniFastNativeCheckNullPointer() {
        BenchmarkState state = mPerfStatusReporter.getBenchmarkState();
        while (state.keepRunning()) {
            final int echoNumber = jintFastNativeCheckNullPointer(50);
        }
    }

    /**
     * The result should be compared with {@link #testJniFastNativeCheckNullPointer()}.
     *
     * <p>CriticalNative can't reference JavaEnv in native layer. It means it should check the null
     * pointer in java layer. It's a comparison between native layer and java layer.
     */
    @Test
    public void testJniCriticalNativeCheckNullPointer() {
        BenchmarkState state = mPerfStatusReporter.getBenchmarkState();
        while (state.keepRunning()) {
            final int echoNumber = jintCriticalNativeCheckNullPointer(50);
            if (echoNumber == -1) {
                Assert.fail("It shouldn't be here");
            }
        }
    }

    /**
     * The result should be compared with {@link #testJniCriticalNativeThrowNullPointerException()}.
     */
    @Test
    public void testJniFastNativeThrowNullPointerException() {
        BenchmarkState state = mPerfStatusReporter.getBenchmarkState();
        while (state.keepRunning()) {
            try {
                jintFastNativeCheckNullPointer(0);
            } catch (NullPointerException e) {
                continue;
            }
            Assert.fail("It shouldn't be here");
        }
    }

    /**
     * The result should be compared with {@link #testJniFastNativeThrowNullPointerException()}.
     *
     * <p>CriticalNative can't reference JavaEnv in native layer. It means it should check the null
     * pointer in java layer. It's a comparison between native layer and java layer.
     */
    @Test
    public void testJniCriticalNativeThrowNullPointerException() {
        BenchmarkState state = mPerfStatusReporter.getBenchmarkState();
        while (state.keepRunning()) {
            try {
                final int echoNumber = jintCriticalNativeCheckNullPointer(0);
                if (echoNumber == -1) {
                    throw new NullPointerException();
                }
            } catch (NullPointerException e) {
                continue;
            }
            Assert.fail("It shouldn't be here");
        }
    }

    // ----------- @FastNative ------------------
    @FastNative
    private static native void jintarrayArgumentNoop(int[] array, int length);
    @FastNative
    private static native int jintarrayGetLength(int[] array);
    @FastNative
    private static native int jintarrayCriticalAccess(int[] array, int index);
    @FastNative
    private static native int jintarrayBasicAccess(int[] array, int index);

    @FastNative
    private static native int jintFastNativeAccess(int echoNumber);

    @FastNative
    private static native int jintFastNativeCheckNullPointer(int echoNumber);

    // ----------- @CriticalNative ------------------
    @CriticalNative
    private static native int jintCriticalNativeAccess(int echoNumber);

    @CriticalNative
    private static native int jintCriticalNativeCheckNullPointer(int echoNumber);
}
