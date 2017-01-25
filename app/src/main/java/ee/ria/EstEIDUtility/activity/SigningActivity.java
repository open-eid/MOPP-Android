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

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import org.apache.commons.io.FilenameUtils;

import ee.ria.EstEIDUtility.R;
import ee.ria.EstEIDUtility.container.ContainerBuilder;
import ee.ria.EstEIDUtility.container.ContainerFacade;
import ee.ria.EstEIDUtility.util.Constants;
import ee.ria.EstEIDUtility.util.FileUtils;
import ee.ria.EstEIDUtility.util.NotificationUtil;

public class SigningActivity extends AppCompatActivity {

    private NotificationUtil notificationUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signing);
        Button addContainer = (Button) findViewById(R.id.add_container);
        addContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createNewContainer();
            }
        });
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        notificationUtil = new NotificationUtil(this);
    }

    private void createNewContainer() {
        FileUtils.clearContainerCache(this);
        FileUtils.clearDataFileCache(this);

        EditText containerName = (EditText) findViewById(R.id.containerName);

        String fileName = containerName.getText().toString();
        if (fileName.isEmpty()) {
            notificationUtil.showWarningMessage(getText(R.string.file_name_empty_message));
            return;
        }

        if (!FilenameUtils.getExtension(containerName.getText().toString()).equals(Constants.BDOC_EXTENSION)) {
            containerName.append(".");
            containerName.append(Constants.BDOC_EXTENSION);
        }

        String containerFileName = containerName.getText().toString();

        ContainerFacade containerFacade = ContainerBuilder.aContainer(this)
                .withContainerName(containerFileName)
                .build();

        Intent intent = new Intent(this, ContainerDetailsActivity.class);

        intent.putExtra(Constants.CONTAINER_NAME_KEY, containerFacade.getName());
        intent.putExtra(Constants.CONTAINER_PATH_KEY, containerFacade.getAbsolutePath());
        startActivity(intent);
    }

}
