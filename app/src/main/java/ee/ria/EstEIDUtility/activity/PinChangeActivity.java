package ee.ria.EstEIDUtility.activity;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import ee.ria.EstEIDUtility.R;
import ee.ria.EstEIDUtility.fragment.PIN1ChangeFragment;
import ee.ria.EstEIDUtility.fragment.PIN2ChangeFragment;

public class PinChangeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pin_change);
        createFragment();
    }

    private void createFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        PinUtilitiesActivity.FragmentToLaunch fragmentToLaunch =
                (PinUtilitiesActivity.FragmentToLaunch) getIntent().getSerializableExtra(PinUtilitiesActivity.PIN_FRAGMENT_TO_LAUNCH);

        switch (fragmentToLaunch) {
            case PIN1:
                setTitle(R.string.change_pin);
                PIN1ChangeFragment pin1ChangeFragment = new PIN1ChangeFragment();
                fragmentTransaction.add(R.id.pin_change, pin1ChangeFragment, PIN1ChangeFragment.TAG);
                break;
            case PIN2:
                setTitle(R.string.change_pin2);
                PIN2ChangeFragment pin2ChangeFragment = new PIN2ChangeFragment();
                fragmentTransaction.add(R.id.pin_change, pin2ChangeFragment, PIN2ChangeFragment.TAG);
                break;
        }

        fragmentTransaction.commit();
    }

}
