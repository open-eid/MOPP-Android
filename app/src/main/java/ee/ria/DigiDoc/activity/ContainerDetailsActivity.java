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

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.fragment.ContainerDetailsFragment;
import ee.ria.DigiDoc.util.FileUtils;

public class ContainerDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_container_details);
        createFragment();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FileUtils.clearContainerCache(this);
        FileUtils.clearDataFileCache(this);
    }

    private void createFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        ContainerDetailsFragment containerDetailsFragment = findContainerDetailsFragment();
        if (containerDetailsFragment != null) {
            return;
        }
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        containerDetailsFragment = new ContainerDetailsFragment();
        setTitle(R.string.bdoc_detail_title);
        containerDetailsFragment.setArguments(getIntent().getExtras());
        fragmentTransaction.add(R.id.bdoc_detail, containerDetailsFragment, ContainerDetailsFragment.TAG);
        fragmentTransaction.commit();
    }

    private ContainerDetailsFragment findContainerDetailsFragment() {
        return (ContainerDetailsFragment) getSupportFragmentManager().findFragmentByTag(ContainerDetailsFragment.TAG);
    }

}
