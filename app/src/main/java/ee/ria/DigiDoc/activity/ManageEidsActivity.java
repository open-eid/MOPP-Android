/*
 * Copyright 2017 Riigi Infosüsteemide Amet
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

package ee.ria.DigiDoc.activity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.SparseArray;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.security.cert.CertificateParsingException;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.certificate.X509Cert;
import ee.ria.DigiDoc.util.DateUtils;
import ee.ria.DigiDoc.util.NotificationUtil;
import ee.ria.scardcomlibrary.impl.ACS;
import ee.ria.token.tokenservice.TokenService;
import ee.ria.token.tokenservice.callback.CertCallback;
import ee.ria.token.tokenservice.callback.PersonalFileCallback;
import ee.ria.token.tokenservice.callback.UseCounterCallback;
import ee.ria.token.tokenservice.callback.RetryCounterCallback;
import ee.ria.tokenlibrary.Token;
import timber.log.Timber;

public class ManageEidsActivity extends AppCompatActivity {

    private static final String TAG = ManageEidsActivity.class.getName();

    @BindView(R.id.givenNames) TextView givenNames;
    @BindView(R.id.surname) TextView surnameView;
    @BindView(R.id.document_number) TextView documentNumberView;
    @BindView(R.id.card_validity) TextView cardValidity;
    @BindView(R.id.signing_cert_validity) TextView signCertValidity;
    @BindView(R.id.sign_cert_valid_value) TextView signCertValidityTime;
    @BindView(R.id.sign_cert_used) TextView signCertUsedView;
    @BindView(R.id.auth_cert_validity) TextView authCertValidity;
    @BindView(R.id.auth_cert_valid_value) TextView authCertValidityTime;
    @BindView(R.id.auth_cert_used) TextView authCertUsedView;
    @BindView(R.id.person_id) TextView personIdCode;
    @BindView(R.id.date_of_birth) TextView dateOfBirth;
    @BindView(R.id.nationality) TextView nationalityView;
    @BindView(R.id.email) TextView emailView;
    @BindView(R.id.info) TextView info;
    @BindView(R.id.my_eid) ScrollView myEidView;

    private TokenService tokenService;
    private boolean serviceBound;
    private BroadcastReceiver cardPresentReceiver;
    private BroadcastReceiver cardAbsentReciever;

    private NotificationUtil notificationUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_eids);
        ButterKnife.bind(this);
        notificationUtil = new NotificationUtil(this);
        info.setText(Html.fromHtml(getString(R.string.eid_info)));
        info.setMovementMethod(LinkMovementMethod.getInstance());
        notificationUtil.showWarningMessage(getText(R.string.insert_card_wait));
        cardPresentReceiver = new CardPresentReciever();
        cardAbsentReciever = new CardAbsentReciever();
        Timber.tag(TAG);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(cardPresentReceiver, new IntentFilter(ACS.CARD_PRESENT_INTENT));
        registerReceiver(cardAbsentReciever, new IntentFilter(ACS.CARD_ABSENT_INTENT));
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cardPresentReceiver != null) {
            unregisterReceiver(cardPresentReceiver);
        }
        if (cardAbsentReciever != null) {
            unregisterReceiver(cardAbsentReciever);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, TokenService.class);
        bindService(intent, tokenServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (serviceBound) {
            unbindService(tokenServiceConnection);
            serviceBound = false;
        }
    }

    private ServiceConnection tokenServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            TokenService.LocalBinder binder = (TokenService.LocalBinder) service;
            tokenService = binder.getService();
            serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    class CardPresentReciever extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            readPersonalData();
            readCertInfo();
            info.setVisibility(View.GONE);
            myEidView.setVisibility(View.VISIBLE);
            notificationUtil.clearMessages();
        }

    }

    class CardAbsentReciever extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            info.setVisibility(View.VISIBLE);
            myEidView.setVisibility(View.GONE);
            notificationUtil.showWarningMessage(getText(R.string.insert_card_wait));

            String empty = "";
            givenNames.setText(empty);
            givenNames.setText(empty);
            surnameView.setText(empty);
            documentNumberView.setText(empty);
            signCertValidityTime.setText(empty);
            personIdCode.setText(empty);
            dateOfBirth.setText(empty);
            nationalityView.setText(empty);
            cardValidity.setText(empty);
            emailView.setText(empty);
            signCertValidity.setText(empty);
            signCertUsedView.setText(empty);
            authCertValidityTime.setText(empty);
            authCertValidity.setText(empty);
            authCertUsedView.setText(empty);            
        }
    }

    public void readPersonalData() {
        PersonalFileCallback callback = new PersonalFileCallback() {
            @Override
            public void onPersonalFileResponse(SparseArray<String> result) {
                String expiryDate = result.get(9);

                surnameView.setText(result.get(1));
                givenNames.setText(result.get(2));
                givenNames.append(result.get(3));
                nationalityView.setText(result.get(5));
                dateOfBirth.setText(result.get(6));
                personIdCode.setText(result.get(7));
                documentNumberView.setText(result.get(8));

                try {
                    Date expiry = DateUtils.DATE_FORMAT.parse(expiryDate);
                    if (!expiry.before(new Date()) && expiry.after(new Date())) {
                        cardValidity.setText(getText(R.string.eid_valid));
                        cardValidity.setTextColor(Color.GREEN);
                    } else {
                        cardValidity.setText(getText(R.string.eid_invalid));
                        cardValidity.setTextColor(Color.RED);
                    }
                } catch (ParseException e) {
                    Timber.e(e, "Error parsing expiry date");
                }
            }

            @Override
            public void onPersonalFileError(String msg) {
                Timber.d("Unable to read personal file: %s", msg);
            }
        };
        tokenService.readPersonalFile(callback);
    }

    public void readCertInfo() {
        CertCallback callback = new SignCertificateCallback();
        tokenService.readCert(Token.CertType.CertSign, callback);

        CertCallback authCertificateCallback = new AuthCertificateCallback();
        tokenService.readCert(Token.CertType.CertAuth, authCertificateCallback);
    }

    private class SignCertUseCounterTaskCallback implements UseCounterCallback {
        @Override
        public void onCounterRead(int counterByte) {
            signCertUsedView.setText(String.format(getText(R.string.eid_cert_used).toString(), String.valueOf(counterByte)));
        }

    }

    private class AuthCertUseCounterTaskCallback implements UseCounterCallback {
        @Override
        public void onCounterRead(int counterByte) {
            authCertUsedView.setText(String.format(getText(R.string.eid_cert_used).toString(), String.valueOf(counterByte)));
        }

    }

    class SignCertificateCallback implements CertCallback {

        @Override
        public void onCertificateResponse(byte[] cert) {
            X509Cert x509Cert = new X509Cert(cert);

            tokenService.readUseCounter(Token.CertType.CertSign, new SignCertUseCounterTaskCallback());

            signCertValidityTime.setText(DateUtils.DATE_FORMAT.format(x509Cert.validUntil()));

            if (x509Cert.isValid()) {
                signCertValidity.setText(getText(R.string.eid_valid));
                signCertValidity.setTextColor(Color.GREEN);
                signCertValidityTime.setTextColor(Color.GREEN);
            } else {
                signCertValidity.setText(getText(R.string.eid_invalid));
                signCertValidity.setTextColor(Color.RED);
                signCertValidityTime.setTextColor(Color.RED);
            }

            Pin2RetryCountCallback pin2CounterCallback = new Pin2RetryCountCallback();
            tokenService.readRetryCounter(Token.PinType.PIN2, pin2CounterCallback);
        }

        @Override
        public void onCertificateError(Exception e) {
            Toast.makeText(ManageEidsActivity.this, getText(R.string.cert_read_failed) + " " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

    }

    class AuthCertificateCallback implements CertCallback {

        @Override
        public void onCertificateResponse(byte[] cert) {
            X509Cert x509Cert = new X509Cert(cert);

            tokenService.readUseCounter(Token.CertType.CertAuth, new AuthCertUseCounterTaskCallback());

            authCertValidityTime.setText(DateUtils.DATE_FORMAT.format(x509Cert.validUntil()));

            if (x509Cert.isValid()) {
                authCertValidity.setText(getText(R.string.eid_valid));
                authCertValidity.setTextColor(Color.GREEN);
                authCertValidityTime.setTextColor(Color.GREEN);
            } else {
                authCertValidity.setText(getText(R.string.eid_invalid));
                authCertValidity.setTextColor(Color.RED);
                authCertValidityTime.setTextColor(Color.RED);
            }

            Pin1RetryCountCallback pin1CounterCallback = new Pin1RetryCountCallback();
            tokenService.readRetryCounter(Token.PinType.PIN1, pin1CounterCallback);

            try {
                Collection<List<?>> subjectAlternativeNames = x509Cert.getCertificate().getSubjectAlternativeNames();
                if (subjectAlternativeNames == null) {
                    Timber.d("Couldn't read email address from certificate");
                    return;
                }
                for (List subjectAlternativeName : subjectAlternativeNames) {
                    if ((Integer) subjectAlternativeName.get(0) == 1) {
                        emailView.setText((CharSequence) subjectAlternativeName.get(1));
                    }
                }
            } catch (CertificateParsingException e) {
                Timber.e(e, "Error parsing certificate");
            }

        }

        @Override
        public void onCertificateError(Exception e) {
            Toast.makeText(ManageEidsActivity.this, getText(R.string.cert_read_failed) + " " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

    }

    class Pin1RetryCountCallback implements RetryCounterCallback {

        @Override
        public void onCounterRead(byte counterByte) {
            if (counterByte == 0) {
                authCertValidity.setText(String.format(getText(R.string.eid_cert_blocked).toString(), "PIN1"));
                authCertValidity.setTextColor(Color.RED);
                authCertValidityTime.setTextColor(Color.RED);
            }
        }

    }

    class Pin2RetryCountCallback implements RetryCounterCallback {

        @Override
        public void onCounterRead(byte counterByte) {
            if (counterByte == 0) {
                signCertValidity.setText(String.format(getText(R.string.eid_cert_blocked).toString(), "PIN2"));
                signCertValidity.setTextColor(Color.RED);
                signCertValidityTime.setTextColor(Color.RED);
            }
        }

    }
}
