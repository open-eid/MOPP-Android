/*
 * app
 * Copyright 2017 - 2023 Riigi Infosüsteemi Amet
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */

package ee.ria.DigiDoc.android.signature.update.smartid;

import static ee.ria.DigiDoc.smartid.dto.response.SessionStatusResponse.ProcessStatus.NO_RESPONSE;
import static ee.ria.DigiDoc.smartid.service.SmartSignConstants.CERTIFICATE_CERT_BUNDLE;
import static ee.ria.DigiDoc.smartid.service.SmartSignConstants.CREATE_SIGNATURE_CHALLENGE;
import static ee.ria.DigiDoc.smartid.service.SmartSignConstants.CREATE_SIGNATURE_DEVICE;
import static ee.ria.DigiDoc.smartid.service.SmartSignConstants.CREATE_SIGNATURE_REQUEST;
import static ee.ria.DigiDoc.smartid.service.SmartSignConstants.CREATE_SIGNATURE_STATUS;
import static ee.ria.DigiDoc.smartid.service.SmartSignConstants.SERVICE_FAULT;
import static ee.ria.DigiDoc.smartid.service.SmartSignConstants.SID_BROADCAST_ACTION;
import static ee.ria.DigiDoc.smartid.service.SmartSignConstants.SID_BROADCAST_TYPE_KEY;
import static ee.ria.DigiDoc.smartid.service.SmartSignConstants.SIGNING_ROLE_DATA;

import android.Manifest;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.ArrayList;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.model.smartid.SmartIdMessageException;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.common.NotificationUtil;
import ee.ria.DigiDoc.common.PowerUtil;
import ee.ria.DigiDoc.common.RoleData;
import ee.ria.DigiDoc.configuration.ConfigurationProvider;
import ee.ria.DigiDoc.sign.SignedContainer;
import ee.ria.DigiDoc.smartid.dto.request.SmartIDSignatureRequest;
import ee.ria.DigiDoc.smartid.dto.response.ServiceFault;
import ee.ria.DigiDoc.smartid.dto.response.SessionStatusResponse;
import ee.ria.DigiDoc.smartid.dto.response.SmartIDServiceResponse;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import timber.log.Timber;

public final class SmartIdOnSubscribe implements ObservableOnSubscribe<SmartIdResponse> {

    private static final String NOTIFICATION_CHANNEL = "SMART_ID_CHANNEL";
    private final Navigator navigator;
    private final SignedContainer container;
    private final LocalBroadcastManager broadcastManager;
    private final String uuid;
    private final String personalCode;
    private final String country;
    @Nullable RoleData roleData;

    Intent intent;

    public SmartIdOnSubscribe(Navigator navigator, Intent intent, SignedContainer container, String uuid,
                              String personalCode, String country, @Nullable RoleData roleData) {
        this.navigator = navigator;
        this.container = container;
        this.broadcastManager = LocalBroadcastManager.getInstance(navigator.activity());
        this.uuid = uuid;
        this.personalCode = personalCode;
        this.country = country;

        this.intent = intent;
        this.roleData = roleData;
    }

    @Override
    public void subscribe(ObservableEmitter<SmartIdResponse> emitter) {
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (navigator.activity() == null) {
                    Timber.log(Log.ERROR, "Activity is null");
                    IllegalStateException ise = new IllegalStateException("Activity not found. Please try again after restarting application");
                    emitter.onError(ise);
                }
                switch (intent.getStringExtra(SID_BROADCAST_TYPE_KEY)) {
                    case SERVICE_FAULT: {
                        NotificationManagerCompat.from(navigator.activity()).cancelAll();
                        ServiceFault serviceFault =
                                ServiceFault.fromJson(intent.getStringExtra(SERVICE_FAULT));
                        Timber.log(Log.DEBUG, "Got SERVICE_FAULT status: %s", serviceFault.getStatus());
                        if (serviceFault.getStatus() == NO_RESPONSE) {
                            emitter.onError(SmartIdMessageException
                                    .create(navigator.activity(), serviceFault.getStatus()));
                        } else {
                            emitter.onError(SmartIdMessageException
                                    .create(navigator.activity(), serviceFault.getStatus(), serviceFault.getDetailMessage()));
                        }
                        break;
                    }
                    case CREATE_SIGNATURE_DEVICE:
                        Timber.log(Log.DEBUG, "Selecting device (CREATE_SIGNATURE_DEVICE)");
                        emitter.onNext(SmartIdResponse.selectDevice(true));
                        break;
                    case CREATE_SIGNATURE_CHALLENGE:
                        Timber.log(Log.DEBUG, "Signature challenge (CREATE_SIGNATURE_CHALLENGE)");
                        String challenge =
                                intent.getStringExtra(CREATE_SIGNATURE_CHALLENGE);
                        emitter.onNext(SmartIdResponse.challenge(challenge));

                        if (!PowerUtil.isPowerSavingMode(navigator.activity())) {
                            Timber.log(Log.DEBUG, "Creating notification channel");
                            NotificationUtil.createNotificationChannel(navigator.activity(),
                                    NOTIFICATION_CHANNEL, navigator.activity()
                                            .getResources()
                                            .getString(R.string.signature_update_signature_add_method_smart_id));
                        }

                        String challengeTitle = navigator.activity()
                                .getResources().getString(R.string.smart_id_challenge);
                        Notification notification = NotificationUtil.createNotification(navigator.activity(), NOTIFICATION_CHANNEL,
                                R.mipmap.ic_launcher, challengeTitle, challenge,
                                NotificationCompat.PRIORITY_HIGH, false);

                        sendNotification(navigator.activity(), challenge, notification);

                        break;
                    case CREATE_SIGNATURE_STATUS:
                        NotificationManagerCompat.from(navigator.activity()).cancelAll();
                        SmartIDServiceResponse status =
                                SmartIDServiceResponse.fromJson(
                                        intent.getStringExtra(CREATE_SIGNATURE_STATUS));
                        if (status.getStatus() == SessionStatusResponse.ProcessStatus.OK) {
                            Timber.log(Log.DEBUG, "Got CREATE_SIGNATURE_STATUS success status: %s", status.getStatus());
                            emitter.onNext(SmartIdResponse.success(container));
                            emitter.onComplete();
                        } else {
                            Timber.log(Log.DEBUG, "Got CREATE_SIGNATURE_STATUS error status: %s", status.getStatus());
                            emitter.onError(SmartIdMessageException
                                    .create(navigator.activity(), status.getStatus()));
                        }
                        break;
                }
            }
        };

        broadcastManager.registerReceiver(receiver, new IntentFilter(SID_BROADCAST_ACTION));
        emitter.setCancellable(() -> broadcastManager.unregisterReceiver(receiver));

        ConfigurationProvider configurationProvider =
                ((Application) navigator.activity().getApplication()).getConfigurationProvider();
        String displayMessage = navigator.activity()
                .getString(R.string.signature_update_mobile_id_display_message);
        SmartIDSignatureRequest request = SmartCreateSignatureRequestHelper
                .create(container, uuid, configurationProvider.getSidV2RestUrl(),
                        configurationProvider.getSidV2SkRestUrl(), country,
                        personalCode, displayMessage);

        intent.putExtra(CREATE_SIGNATURE_REQUEST, request);
        intent.putStringArrayListExtra(CERTIFICATE_CERT_BUNDLE,
                new ArrayList<>(configurationProvider.getCertBundle()));
        intent.putExtra(SIGNING_ROLE_DATA, roleData);
        navigator.activity().startService(intent);
    }

    private void sendNotification(Context context, String challenge, Notification notification) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2 || ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(navigator.activity())
                    .notify(Integer.parseInt(challenge), notification);
        }
    }
}