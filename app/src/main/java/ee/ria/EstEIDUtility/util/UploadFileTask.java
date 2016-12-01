package ee.ria.EstEIDUtility.util;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.WriteMode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class UploadFileTask extends AsyncTask<Void, Void, FileMetadata> {

    private static final String TAG = "UploadFileTask";

    private final Context context;
    private final DbxClientV2 dbxClient;
    private final String fileName;
    private final Callback callback;
    private Exception error;

    public UploadFileTask(Context context, DbxClientV2 dbxClient, String fileName, Callback callback) {
        this.context = context;
        this.dbxClient = dbxClient;
        this.fileName = fileName;
        this.callback = callback;
    }

    public interface Callback {
        void onUploadComplete(FileMetadata result);
        void onError(Exception e);
    }

    protected void onPostExecute(FileMetadata result) {
        if (error != null) {
            callback.onError(error);
        } else if (result == null) {
            callback.onError(null);
        } else {
            callback.onUploadComplete(result);
        }
    }

    @Override
    protected FileMetadata doInBackground(Void... params) {
        File bdocsPath = FileUtils.getBdocsPath(context.getFilesDir());
        File filePath = new File(bdocsPath, fileName);
        try (FileInputStream inputStream = new FileInputStream(filePath)) {
            FileMetadata fileMetadata = dbxClient.files().uploadBuilder("/" + fileName)
                    .withMode(WriteMode.OVERWRITE)
                    .uploadAndFinish(inputStream);
            Log.i(TAG, "The uploaded file's rev is: " + fileMetadata.getRev() + " Size:" + fileMetadata.getSize());
            return fileMetadata;
        } catch (IOException | DbxException e) {
            Log.e(TAG, "doInBackground: ", e);
            error = e;
        }
        return null;
    }
}
