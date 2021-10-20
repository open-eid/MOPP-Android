/*
 * app
 * Copyright 2017 - 2021 Riigi Infosüsteemi Amet
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

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.ArrayList;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.model.smartid.SmartIdMessageException;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.configuration.ConfigurationProvider;
import ee.ria.DigiDoc.smartid.dto.request.SmartIDSignatureRequest;
import ee.ria.DigiDoc.smartid.dto.response.SessionStatusResponse;
import ee.ria.DigiDoc.smartid.dto.response.SmartIDServiceResponse;
import ee.ria.DigiDoc.smartid.service.SmartSignService;
import ee.ria.DigiDoc.sign.SignedContainer;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;

import static ee.ria.DigiDoc.smartid.service.SmartSignConstants.CERTIFICATE_CERT_BUNDLE;
import static ee.ria.DigiDoc.smartid.service.SmartSignConstants.CREATE_SIGNATURE_CHALLENGE;
import static ee.ria.DigiDoc.smartid.service.SmartSignConstants.CREATE_SIGNATURE_DEVICE;
import static ee.ria.DigiDoc.smartid.service.SmartSignConstants.CREATE_SIGNATURE_REQUEST;
import static ee.ria.DigiDoc.smartid.service.SmartSignConstants.CREATE_SIGNATURE_STATUS;
import static ee.ria.DigiDoc.smartid.service.SmartSignConstants.SID_BROADCAST_ACTION;
import static ee.ria.DigiDoc.smartid.service.SmartSignConstants.SID_BROADCAST_TYPE_KEY;
import static ee.ria.DigiDoc.smartid.service.SmartSignConstants.SERVICE_FAULT;

public final class SmartIdOnSubscribe implements ObservableOnSubscribe<SmartIdResponse> {

    private static final String NOTIFICATION_CHANNEL = "SMART_ID_CHANNEL";
    private final Navigator navigator;
    private final SignedContainer container;
    private final LocalBroadcastManager broadcastManager;
    private final String uuid;
    private final String personalCode;
    private final String country;

    public SmartIdOnSubscribe(Navigator navigator, SignedContainer container, String uuid,
                              String personalCode, String country) {
        this.navigator = navigator;
        this.container = container;
        this.broadcastManager = LocalBroadcastManager.getInstance(navigator.activity());
        this.uuid = uuid;
        this.personalCode = personalCode;
        this.country = country;
    }

    @Override
    public void subscribe(ObservableEmitter<SmartIdResponse> emitter) {
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getStringExtra(SID_BROADCAST_TYPE_KEY)) {
                    case SERVICE_FAULT: {
                        NotificationManagerCompat.from(navigator.activity()).cancelAll();
                        SessionStatusResponse.ProcessStatus status =
                                (SessionStatusResponse.ProcessStatus) intent.getSerializableExtra(SERVICE_FAULT);
                        emitter.onError(SmartIdMessageException
                                .create(navigator.activity(), status));
                        break;
                    }
                    case CREATE_SIGNATURE_DEVICE:
                        emitter.onNext(SmartIdResponse.selectDevice(true));
                        break;
                    case CREATE_SIGNATURE_CHALLENGE:
                        String challenge =
                                intent.getStringExtra(CREATE_SIGNATURE_CHALLENGE);
                        emitter.onNext(SmartIdResponse.challenge(challenge));

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL,
                                    NOTIFICATION_CHANNEL + "_NAME", NotificationManager.IMPORTANCE_HIGH);
                            NotificationManager systemService = navigator.activity().getSystemService(NotificationManager.class);
                            if (systemService != null) {
                                systemService.createNotificationChannel(channel);
                            }
                        }
                        NotificationCompat.Builder notification = new NotificationCompat
                                .Builder(navigator.activity(), NOTIFICATION_CHANNEL)
                                .setSmallIcon(R.mipmap.ic_launcher)
                                .setContentText(challenge)
                                .setContentTitle(navigator.activity().getString(R.string.smart_id_challenge))
                                .setPriority(NotificationCompat.PRIORITY_HIGH)
                                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                                .setAutoCancel(true);
                        NotificationManagerCompat.from(navigator.activity())
                                .notify(Integer.parseInt(challenge), notification.build());
                        break;
                    case CREATE_SIGNATURE_STATUS:
                        NotificationManagerCompat.from(navigator.activity()).cancelAll();
                        SmartIDServiceResponse status =
                                SmartIDServiceResponse.fromJson(
                                        intent.getStringExtra(CREATE_SIGNATURE_STATUS));
                        if (status.getStatus() == SessionStatusResponse.ProcessStatus.OK) {
                            emitter.onNext(SmartIdResponse.success(container));
                            emitter.onComplete();
                        } else {
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
                .create(container, uuid, configurationProvider.getSidRestUrl(),
                        configurationProvider.getSidSkRestUrl(), country, personalCode, displayMessage);

        android.content.Intent intent = new Intent(navigator.activity(), SmartSignService.class);
        intent.putExtra(CREATE_SIGNATURE_REQUEST, request);
        intent.putStringArrayListExtra(CERTIFICATE_CERT_BUNDLE,
                new ArrayList<>(configurationProvider.getCertBundle()));
        navigator.activity().startService(intent);
    }
}
