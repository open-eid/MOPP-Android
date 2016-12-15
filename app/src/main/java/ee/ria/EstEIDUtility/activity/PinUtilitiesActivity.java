package ee.ria.EstEIDUtility.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.View;

import ee.ria.EstEIDUtility.R;
import ee.ria.EstEIDUtility.util.Constants;
import ee.ria.token.tokenservice.token.Token;

public class PinUtilitiesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_utilities);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        View changePin1 = findViewById(R.id.changePin1);
        changePin1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchPinChangeActivity(Token.PinType.PIN1);
            }
        });

        View changePin2 = findViewById(R.id.changePin2);
        changePin2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchPinChangeActivity(Token.PinType.PIN2);
            }
        });
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
