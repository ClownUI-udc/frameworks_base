/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.server.biometrics.sensors.face.aidl;

import static android.hardware.biometrics.BiometricFaceConstants.FACE_ERROR_LOCKOUT;
import static android.hardware.biometrics.BiometricFaceConstants.FACE_ERROR_LOCKOUT_PERMANENT;
import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.ActivityManager;
import android.app.ActivityTaskManager;
import android.content.ComponentName;
import android.hardware.biometrics.common.AuthenticateReason;
import android.hardware.biometrics.common.ICancellationSignal;
import android.hardware.biometrics.common.OperationContext;
import android.hardware.biometrics.common.WakeReason;
import android.hardware.biometrics.face.ISession;
import android.hardware.face.Face;
import android.hardware.face.FaceAuthenticateOptions;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.platform.test.annotations.Presubmit;
import android.testing.TestableContext;

import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.log.BiometricLogger;
import com.android.server.biometrics.log.OperationContextExt;
import com.android.server.biometrics.sensors.AuthSessionCoordinator;
import com.android.server.biometrics.sensors.ClientMonitorCallback;
import com.android.server.biometrics.sensors.ClientMonitorCallbackConverter;
import com.android.server.biometrics.sensors.face.UsageStats;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Presubmit
@SmallTest
public class FaceAuthenticationClientTest {

    private static final int USER_ID = 12;
    private static final long OP_ID = 32;
    private static final int WAKE_REASON = WakeReason.LIFT;
    private static final int AUTH_REASON = AuthenticateReason.Face.ASSISTANT_VISIBLE;

    @Rule
    public final TestableContext mContext = new TestableContext(
            InstrumentationRegistry.getInstrumentation().getTargetContext(), null);

    @Mock
    private ISession mHal;
    @Mock
    private IBinder mToken;
    @Mock
    private ClientMonitorCallbackConverter mClientMonitorCallbackConverter;
    @Mock
    private BiometricLogger mBiometricLogger;
    @Mock
    private BiometricContext mBiometricContext;
    @Mock
    private UsageStats mUsageStats;
    @Mock
    private ClientMonitorCallback mCallback;
    @Mock
    private Sensor.HalSessionCallback mHalSessionCallback;
    @Mock
    private ActivityTaskManager mActivityTaskManager;
    @Mock
    private ICancellationSignal mCancellationSignal;
    @Mock
    private AuthSessionCoordinator mAuthSessionCoordinator;
    @Captor
    private ArgumentCaptor<OperationContextExt> mOperationContextCaptor;
    @Captor
    private ArgumentCaptor<Consumer<OperationContext>> mContextInjector;

    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();

    @Before
    public void setup() {
        when(mBiometricContext.updateContext(any(), anyBoolean())).thenAnswer(
                i -> i.getArgument(0));
        when(mBiometricContext.getAuthSessionCoordinator()).thenReturn(mAuthSessionCoordinator);
    }

    @Test
    public void authNoContext_v1() throws RemoteException {
        final FaceAuthenticationClient client = createClient(1);
        client.start(mCallback);

        verify(mHal).authenticate(eq(OP_ID));
        verify(mHal, never()).authenticateWithContext(anyLong(), any());
    }

    @Test
    public void authWithContext_v2() throws RemoteException {
        final FaceAuthenticationClient client = createClient(2);
        client.start(mCallback);

        InOrder order = inOrder(mHal, mBiometricContext);
        order.verify(mBiometricContext).updateContext(
                mOperationContextCaptor.capture(), anyBoolean());

        final OperationContext aidlContext = mOperationContextCaptor.getValue().toAidlContext();
        order.verify(mHal).authenticateWithContext(eq(OP_ID), same(aidlContext));
        assertThat(aidlContext.wakeReason).isEqualTo(WAKE_REASON);
        assertThat(aidlContext.authenticateReason.getFaceAuthenticateReason())
                .isEqualTo(AUTH_REASON);

        verify(mHal, never()).authenticate(anyLong());
    }

    @Test
    public void testLockoutEndsOperation() throws RemoteException {
        final FaceAuthenticationClient client = createClient(2);
        client.start(mCallback);
        client.onLockoutPermanent();

        verify(mClientMonitorCallbackConverter).onError(anyInt(), anyInt(),
                eq(FACE_ERROR_LOCKOUT_PERMANENT), anyInt());
        verify(mCallback).onClientFinished(client, false);
    }

    @Test
    public void testTemporaryLockoutEndsOperation() throws RemoteException {
        final FaceAuthenticationClient client = createClient(2);
        client.start(mCallback);
        client.onLockoutTimed(1000);

        verify(mClientMonitorCallbackConverter).onError(anyInt(), anyInt(),
                eq(FACE_ERROR_LOCKOUT), anyInt());
        verify(mCallback).onClientFinished(client, false);
    }

    @Test
    public void notifyHalWhenContextChanges() throws RemoteException {
        final FaceAuthenticationClient client = createClient();
        client.start(mCallback);

        final ArgumentCaptor<OperationContext> captor =
                ArgumentCaptor.forClass(OperationContext.class);
        verify(mHal).authenticateWithContext(eq(OP_ID), captor.capture());
        OperationContext opContext = captor.getValue();

        // fake an update to the context
        verify(mBiometricContext).subscribe(
                mOperationContextCaptor.capture(), mContextInjector.capture());
        assertThat(opContext).isSameInstanceAs(
                mOperationContextCaptor.getValue().toAidlContext());
        mContextInjector.getValue().accept(opContext);
        verify(mHal).onContextChanged(same(opContext));

        client.stopHalOperation();
        verify(mBiometricContext).unsubscribe(same(mOperationContextCaptor.getValue()));
    }

    @Test
    public void cancelsAuthWhenNotInForeground() throws Exception {
        final ActivityManager.RunningTaskInfo topTask = new ActivityManager.RunningTaskInfo();
        topTask.topActivity = new ComponentName("other", "thing");
        when(mActivityTaskManager.getTasks(anyInt())).thenReturn(List.of(topTask));
        when(mHal.authenticateWithContext(anyLong(), any())).thenReturn(mCancellationSignal);

        final FaceAuthenticationClient client = createClient();
        client.start(mCallback);
        client.onAuthenticated(new Face("friendly", 1 /* faceId */, 2 /* deviceId */),
                true /* authenticated */, new ArrayList<>());

        verify(mCancellationSignal).cancel();
    }

    private FaceAuthenticationClient createClient() throws RemoteException {
        return createClient(2 /* version */);
    }

    private FaceAuthenticationClient createClient(int version) throws RemoteException {
        when(mHal.getInterfaceVersion()).thenReturn(version);

        final AidlSession aidl = new AidlSession(version, mHal, USER_ID, mHalSessionCallback);
        final FaceAuthenticateOptions options = new FaceAuthenticateOptions.Builder()
                .setOpPackageName("test-owner")
                .setUserId(USER_ID)
                .setSensorId(9)
                .setWakeReason(PowerManager.WAKE_REASON_LIFT)
                .setAuthenticateReason(
                        FaceAuthenticateOptions.AUTHENTICATE_REASON_ASSISTANT_VISIBLE)
                .build();
        return new FaceAuthenticationClient(mContext, () -> aidl, mToken,
                2 /* requestId */, mClientMonitorCallbackConverter, OP_ID,
                false /* restricted */, options, 4 /* cookie */,
                false /* requireConfirmation */,
                mBiometricLogger, mBiometricContext, true /* isStrongBiometric */,
                mUsageStats, null /* mLockoutCache */, false /* allowBackgroundAuthentication */,
                null /* sensorPrivacyManager */, 0 /* biometricStrength */) {
            @Override
            protected ActivityTaskManager getActivityTaskManager() {
                return mActivityTaskManager;
            }
        };
    }
}
