/*
 * Copyright (c) 2017 The Android Open Source Project
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

package android.telephony.ims.aidl;

import android.os.Message;
import android.telephony.ims.aidl.IImsMmTelListener;
import android.telephony.ims.aidl.IImsSmsListener;
import android.telephony.ims.aidl.IImsCapabilityCallback;
import android.telephony.ims.aidl.ISrvccStartedCallback;
import android.telephony.ims.feature.CapabilityChangeRequest;
import android.telephony.ims.MediaQualityStatus;
import android.telephony.ims.MediaThreshold;
import android.telephony.ims.RtpHeaderExtensionType;

import android.telephony.ims.ImsCallProfile;
import com.android.ims.internal.IImsCallSession;
import com.android.ims.internal.IImsEcbm;
import com.android.ims.internal.IImsMultiEndpoint;
import com.android.ims.internal.IImsRegistrationListener;
import com.android.ims.internal.IImsUt;

import java.util.List;

/**
 * See MmTelFeature for more information.
 * {@hide}
 */
interface IImsMmTelFeature {
    void setListener(IImsMmTelListener l);
    int getFeatureState();
    ImsCallProfile createCallProfile(int callSessionType, int callType);
    void changeOfferedRtpHeaderExtensionTypes(in List<RtpHeaderExtensionType> types);
    IImsCallSession createCallSession(in ImsCallProfile profile);
    int shouldProcessCall(in String[] uris);
    IImsUt getUtInterface();
    IImsEcbm getEcbmInterface();
    void setUiTtyMode(int uiTtyMode, in Message onCompleteMessage);
    IImsMultiEndpoint getMultiEndpointInterface();
    int queryCapabilityStatus();
    void setTerminalBasedCallWaitingStatus(boolean enabled);
    oneway void addCapabilityCallback(IImsCapabilityCallback c);
    oneway void removeCapabilityCallback(IImsCapabilityCallback c);
    oneway void changeCapabilitiesConfiguration(in CapabilityChangeRequest request,
            IImsCapabilityCallback c);
    oneway void queryCapabilityConfiguration(int capability, int radioTech,
            IImsCapabilityCallback c);
    oneway void notifySrvccStarted(in ISrvccStartedCallback cb);
    oneway void notifySrvccCompleted();
    oneway void notifySrvccFailed();
    oneway void notifySrvccCanceled();
    oneway void setMediaQualityThreshold(int mediaSessionType, in MediaThreshold threshold);
    MediaQualityStatus queryMediaQualityStatus(int mediaSessionType);

    // SMS APIs
    void setSmsListener(IImsSmsListener l);
    oneway void sendSms(in int token, int messageRef, String format, String smsc, boolean retry,
            in byte[] pdu);
    oneway void onMemoryAvailable(int token);
    oneway void acknowledgeSms(int token, int messageRef, int result);
    oneway void acknowledgeSmsWithPdu(int token, int messageRef, int result, in byte[] pdu);
    oneway void acknowledgeSmsReport(int token, int messageRef, int result);
    String getSmsFormat();
    oneway void onSmsReady();
}
