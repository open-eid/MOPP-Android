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

import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.container.ContainerBuilder;
import ee.ria.DigiDoc.container.ContainerFacade;
import ee.ria.DigiDoc.preferences.SettingsActivity;
import ee.ria.DigiDoc.util.Constants;
import ee.ria.DigiDoc.util.FileUtils;

public class DashboardMenuActivity extends AppCompatActivity {

    private static final int CHOOSE_FILE_REQUEST_ID = 1111;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard_menu);
        showVersion();
    }

    private void showVersion() {
        TextView version = (TextView)findViewById(R.id.app_version);
        version.setText(getVersionName());
    }

    private String getVersionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "N/A";
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.dashboard_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void startSign(View view) {
        Intent intent = new Intent()
                .setType("*/*")
                .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                .setAction(Intent.ACTION_GET_CONTENT)
                .addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(
                Intent.createChooser(intent, getText(R.string.select_file)),
                CHOOSE_FILE_REQUEST_ID);

    }

    public void startMyEids(View view) {
        startActivity(ManageEidsActivity.class);
    }

    public void startPinUtilities(View view) {
        startActivity(PinUtilitiesActivity.class);
    }

    public void startContainerBrowse(View view) {
        startActivity(BrowseContainersActivity.class);
    }

    private void startActivity(Class<?> signingActivityClass) {
        Intent intent = new Intent(this, signingActivityClass);
        startActivity(intent);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CHOOSE_FILE_REQUEST_ID && resultCode == RESULT_OK && data != null) {
            FileUtils.clearContainerCache(this);
            FileUtils.clearDataFileCache(this);

            List<Uri> uris = new ArrayList<>();
            Uri uri;
            ClipData clipData;
            if ((clipData = data.getClipData()) != null) {
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    if ((uri = clipData.getItemAt(i).getUri()) != null) {
                        uris.add(uri);
                    }
                }
            } else if ((uri = data.getData()) != null) {
                uris.add(uri);
            }

            if (uris.isEmpty()) {
                return;
            }

            ContainerFacade containerFacade = ContainerBuilder.aContainer(this)
                    .withDataFiles(uris)
                    .build();

            Intent intent = new Intent(this, ContainerDetailsActivity.class);
            intent.putExtra(Constants.CONTAINER_NAME_KEY, containerFacade.getName());
            intent.putExtra(Constants.CONTAINER_PATH_KEY, containerFacade.getAbsolutePath());
            startActivity(intent);
        }
    }
}
