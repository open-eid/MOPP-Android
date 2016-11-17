package ee.ria.EstEIDUtility.activity;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import ee.ria.EstEIDUtility.fragment.BdocDetailFragment;
import ee.ria.EstEIDUtility.R;

public class BdocDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bdoc_detail);

        createFragment();
    }

    private void createFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        BdocDetailFragment bdocDetailFragment = new BdocDetailFragment();
        setTitle(R.string.bdoc_detail_title);
        fragmentTransaction.add(R.id.bdoc_detail, bdocDetailFragment, "BDOC_DETAIL_FRAGMENT");

        fragmentTransaction.commit();
    }

}
