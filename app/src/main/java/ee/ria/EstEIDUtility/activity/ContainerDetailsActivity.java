package ee.ria.EstEIDUtility.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import ee.ria.EstEIDUtility.R;
import ee.ria.EstEIDUtility.fragment.ContainerDetailsFragment;
import ee.ria.EstEIDUtility.util.Constants;
import ee.ria.EstEIDUtility.util.FileUtils;

public class ContainerDetailsActivity extends AppCompatActivity {

    private String containerName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_container_details);
        createFragment();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FileUtils.clearCacheDir(getCacheDir());
    }

    private void createFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        ContainerDetailsFragment containerDetailsFragment = findContainerDetailsFragment();
        if (containerDetailsFragment != null) {
            return;
        }

        Intent intent = getIntent();
        containerName = intent.getExtras().getString(Constants.CONTAINER_NAME_KEY);

        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        Bundle extras = new Bundle();
        extras.putString(Constants.CONTAINER_NAME_KEY, containerName);

        containerDetailsFragment = new ContainerDetailsFragment();
        setTitle(R.string.bdoc_detail_title);
        containerDetailsFragment.setArguments(extras);
        fragmentTransaction.add(R.id.bdoc_detail, containerDetailsFragment, ContainerDetailsFragment.TAG);
        fragmentTransaction.commit();
    }

    private ContainerDetailsFragment findContainerDetailsFragment() {
        return (ContainerDetailsFragment) getSupportFragmentManager().findFragmentByTag(ContainerDetailsFragment.TAG);
    }

}
