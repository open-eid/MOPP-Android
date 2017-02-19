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

package ee.ria.EstEIDUtility.activity;

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

import ee.ria.EstEIDUtility.R;
import ee.ria.EstEIDUtility.certificate.X509Cert;
import ee.ria.EstEIDUtility.util.DateUtils;
import ee.ria.EstEIDUtility.util.NotificationUtil;
import ee.ria.scardcomlibrary.impl.ACS;
import ee.ria.token.tokenservice.TokenService;
import ee.ria.token.tokenservice.callback.CertCallback;
import ee.ria.token.tokenservice.callback.PersonalFileCallback;
import ee.ria.token.tokenservice.callback.UseCounterCallback;
import ee.ria.tokenlibrary.Token;
import timber.log.Timber;

public class ManageEidsActivity extends AppCompatActivity {

    private static final String TAG = ManageEidsActivity.class.getName();

    private TextView givenNames;
    private TextView surnameView;
    private TextView documentNumberView;
    private TextView cardValidity;
    private TextView cardValidityTime;
    private TextView certValidity;
    private TextView certValidityTime;
    private TextView certUsedView;
    private TextView personIdCode;
    private TextView dateOfBirth;
    private TextView nationalityView;
    private TextView emailView;
    private TextView info;
    private ScrollView myEidView;

    private TokenService tokenService;
    private boolean serviceBound;
    private BroadcastReceiver cardPresentReceiver;
    private BroadcastReceiver cardAbsentReciever;

    private NotificationUtil notificationUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_eids);

        notificationUtil = new NotificationUtil(this);
        info = (TextView) findViewById(R.id.info);
        info.setText(Html.fromHtml(getString(R.string.eid_info)));
        info.setMovementMethod(LinkMovementMethod.getInstance());

        myEidView = (ScrollView) findViewById(R.id.my_eid);
        notificationUtil.showWarningMessage(getText(R.string.insert_card_wait));

        givenNames = (TextView) findViewById(R.id.givenNames);
        surnameView = (TextView) findViewById(R.id.surname);
        documentNumberView = (TextView) findViewById(R.id.document_number);
        cardValidity = (TextView) findViewById(R.id.card_validity);
        cardValidityTime = (TextView) findViewById(R.id.card_valid_value);

        certValidity = (TextView) findViewById(R.id.cert_validity);
        certValidityTime = (TextView) findViewById(R.id.cert_valid_value);
        certUsedView = (TextView) findViewById(R.id.cert_used);

        personIdCode = (TextView) findViewById(R.id.person_id);
        dateOfBirth = (TextView) findViewById(R.id.date_of_birth);
        nationalityView = (TextView) findViewById(R.id.nationality);
        emailView = (TextView) findViewById(R.id.email);

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
            certValidityTime.setText(empty);
            cardValidityTime.setText(empty);
            personIdCode.setText(empty);
            dateOfBirth.setText(empty);
            nationalityView.setText(empty);
            cardValidity.setText(empty);
            emailView.setText(empty);
            certValidity.setText(empty);
            certUsedView.setText(empty);
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
                certValidityTime.setText(expiryDate);
                cardValidityTime.setText(expiryDate);

                try {
                    Date expiry = DateUtils.DATE_FORMAT.parse(expiryDate);
                    if (!expiry.before(new Date()) && expiry.after(new Date())) {
                        cardValidity.setText(getText(R.string.eid_valid));
                        cardValidity.setTextColor(Color.GREEN);
                        cardValidityTime.setTextColor(Color.GREEN);
                    } else {
                        cardValidity.setText(getText(R.string.eid_invalid));
                        cardValidity.setTextColor(Color.RED);
                        cardValidityTime.setTextColor(Color.RED);
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

    private class UseCounterTaskCallback implements UseCounterCallback {
        @Override
        public void onCounterRead(int counterByte) {
            certUsedView.setText(String.valueOf(counterByte));
        }

    }

    class SignCertificateCallback implements CertCallback {

        @Override
        public void onCertificateResponse(byte[] cert) {
            X509Cert x509Cert = new X509Cert(cert);

            tokenService.readUseCounter(Token.CertType.CertSign, new UseCounterTaskCallback());

            if (x509Cert.isValid()) {
                certValidity.setText(getText(R.string.eid_valid));
                certValidity.setTextColor(Color.GREEN);
                certValidityTime.setTextColor(Color.GREEN);
            } else {
                certValidity.setText(getText(R.string.eid_invalid));
                certValidity.setTextColor(Color.RED);
                certValidityTime.setTextColor(Color.RED);
            }
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

}
