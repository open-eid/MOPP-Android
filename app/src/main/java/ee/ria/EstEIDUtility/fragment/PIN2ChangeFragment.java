package ee.ria.EstEIDUtility.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;

import ee.ria.EstEIDUtility.R;

public class PIN2ChangeFragment extends Fragment {

    public static final String TAG = "PIN2_CHANGE_FRAGMENT";

    public PIN2ChangeFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View fragLayout = inflater.inflate(R.layout.pin2_change, container, false);

        RadioButton radioPIN2 = (RadioButton) fragLayout.findViewById(R.id.radioPIN2);
        radioPIN2.setChecked(true);

        return fragLayout;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

}
