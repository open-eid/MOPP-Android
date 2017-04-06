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
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.util.Constants;
import ee.ria.DigiDoc.util.NotificationUtil;
import ee.ria.scardcomlibrary.impl.ACS;
import ee.ria.token.tokenservice.TokenService;
import ee.ria.tokenlibrary.Token;

public class PinUtilitiesActivity extends AppCompatActivity {

    @BindView(R.id.changePin1) View changePin1;
    @BindView(R.id.changePin2) View changePin2;
    @BindView(R.id.changePUK) View changePUK;
    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.pinUtilities) View pinUtilities;

    private NotificationUtil notificationUtil;
    private TokenService tokenService;
    private boolean serviceBound = false;
    private BroadcastReceiver cardPresentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            showWarning(false);
        }
    };
    private BroadcastReceiver cardAbsentReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            showWarning(true);
        }
    };
    private ServiceConnection tokenServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            TokenService.LocalBinder binder = (TokenService.LocalBinder) service;
            tokenService = binder.getService();
            showWarning(!tokenService.isTokenAvailable());
            serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, TokenService.class);
        bindService(intent, tokenServiceConnection, Context.BIND_AUTO_CREATE);
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
        unregisterReceiver(cardPresentReceiver);
        unregisterReceiver(cardAbsentReciever);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (serviceBound) {
            unbindService(tokenServiceConnection);
            serviceBound = false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_utilities);
        ButterKnife.bind(this);
        notificationUtil = new NotificationUtil(this);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        showWarning(true);
    }

    private void showWarning(boolean warn){
        pinUtilities.setVisibility(warn ? View.GONE : View.VISIBLE);
        if (warn) {
            notificationUtil.showWarningMessage(getText(R.string.warning_card_not_found));
        } else {
            notificationUtil.clearMessages();
        }
    }

    @OnClick(R.id.changePin1)
    void onChangePin1() {
        launchPinChangeActivity(Token.PinType.PIN1);
    }

    @OnClick(R.id.changePin2)
    void onChangePin2() {
        launchPinChangeActivity(Token.PinType.PIN2);
    }

    @OnClick(R.id.changePUK)
    void onChangePUK() {
        startActivity(new Intent(this, PukChangeActivity.class));
        overridePendingTransition(R.anim.enter, R.anim.leave);
    }

    private void launchPinChangeActivity(Token.PinType pinType) {
        Intent intent = new Intent(this, PinChangeActivity.class);
        intent.putExtra(Constants.PIN_TYPE_KEY, pinType);
        startActivity(intent);
        overridePendingTransition(R.anim.enter, R.anim.leave);
    }
}
