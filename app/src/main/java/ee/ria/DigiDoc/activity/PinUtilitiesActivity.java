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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.View;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.util.Constants;
import ee.ria.tokenlibrary.Token;

public class PinUtilitiesActivity extends AppCompatActivity {

    @BindView(R.id.changePin1) View changePin1;
    @BindView(R.id.changePin2) View changePin2;
    @BindView(R.id.toolbar) Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_utilities);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @OnClick(R.id.changePin1)
    void onChangePin1() {
        launchPinChangeActivity(Token.PinType.PIN1);
    }

    @OnClick(R.id.changePin2)
    void onChangePin2() {
        launchPinChangeActivity(Token.PinType.PIN2);
    }

    private void launchPinChangeActivity(Token.PinType pinType) {
        Intent intent = new Intent(this, PinChangeActivity.class);
        intent.putExtra(Constants.PIN_TYPE_KEY, pinType);
        startActivity(intent);
        overridePendingTransition(R.anim.enter, R.anim.leave);
    }

    @Override
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        return super.onCreateView(parent, name, context, attrs);
    }

}
