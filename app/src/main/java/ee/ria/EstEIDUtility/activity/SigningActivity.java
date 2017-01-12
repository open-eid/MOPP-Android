package ee.ria.EstEIDUtility.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;

import org.apache.commons.io.FilenameUtils;

import ee.ria.EstEIDUtility.R;
import ee.ria.EstEIDUtility.container.ContainerBuilder;
import ee.ria.EstEIDUtility.container.ContainerFacade;
import ee.ria.EstEIDUtility.util.Constants;
import ee.ria.EstEIDUtility.util.FileUtils;
import ee.ria.EstEIDUtility.util.NotificationUtil;

import static ee.ria.EstEIDUtility.container.ContainerBuilder.ContainerLocation.CACHE;

public class SigningActivity extends AppCompatActivity {

    private NotificationUtil notificationUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signing);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        notificationUtil = new NotificationUtil(this);
    }

    public void createNewContainer(View view) {
        FileUtils.clearContainerCache(this);
        EditText containerName = (EditText) findViewById(R.id.textToSign);

        String fileName = containerName.getText().toString();
        if (fileName.isEmpty()) {
            notificationUtil.showWarningMessage(getText(R.string.file_name_empty_message));
            return;
        }

        if (!FilenameUtils.getExtension(containerName.getText().toString()).equals(Constants.BDOC_EXTENSION)) {
            containerName.append(".");
            containerName.append(Constants.BDOC_EXTENSION);
            containerName.setText(containerName.getText().toString());
        }

        String containerFileName = containerName.getText().toString();

        ContainerFacade containerFacade = ContainerBuilder.aContainer(this)
                .withContainerLocation(CACHE)
                .withContainerName(containerFileName)
                .build();

        Intent intent = new Intent(this, ContainerDetailsActivity.class);
        intent.putExtra(Constants.CONTAINER_NAME_KEY, containerFacade.getName());
        intent.putExtra(Constants.CONTAINER_PATH_KEY, containerFacade.getAbsolutePath());
        startActivity(intent);
    }

}
