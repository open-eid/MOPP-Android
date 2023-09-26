package ee.ria.DigiDoc.android.utils;

import static ee.ria.DigiDoc.BuildConfig.APPLICATION_ID;
import static ee.ria.DigiDoc.android.Constants.DIR_INTERNAL_FILES;

import android.content.ClipData;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Parcelable;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.core.content.FileProvider;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.utils.files.FileStream;
import ee.ria.DigiDoc.common.FileUtil;
import ee.ria.DigiDoc.sign.DataFile;
import ee.ria.DigiDoc.sign.SignedContainer;
import timber.log.Timber;

public final class IntentUtils {

    private static final String DOCUMENTS_FOLDER = "Documents";

    /**
     * Create an intent to choose multiple files of any type.
     *
     * @return Intent to use with {@link android.app.Activity#startActivityForResult(Intent, int)}.
     */
    public static Intent createGetContentIntent(Boolean allowMultiple) {
        return Intent
                .createChooser(new Intent(Intent.ACTION_GET_CONTENT)
                        .setType("*/*")
                        .addCategory(Intent.CATEGORY_OPENABLE)
                        .putExtra(Intent.EXTRA_REFERRER, R.string.application_name)
                        .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple), null);
    }

    /**
     * Parse intent returned from {@link #createGetContentIntent() get content intent} to
     * {@link FileStream} objects.
     *
     * Always returns a list, even if only one file was chosen.
     *
     * @param context Context.
     * @param contentResolver Content resolver to get display name, type and input stream.
     * @param intent Intent returned from
     *               {@link android.app.Activity#onActivityResult(int, int, Intent)}.
     * @param externallyOpenedFilesDirectory File Externally opened files directory.
     * @return List of {@link FileStream file stream} objects.
     */
    public static ImmutableList<FileStream> parseGetContentIntent(Context context, ContentResolver contentResolver,
                                                                  Intent intent,
                                                                  File externallyOpenedFilesDirectory) {
        ImmutableList.Builder<FileStream> builder = ImmutableList.builder();

        ClipData clipData = intent.getClipData();
        Uri data = intent.getData();
        if (clipData != null) {
            for (int i = 0; i < clipData.getItemCount(); i++) {
                Uri uri = clipData.getItemAt(i).getUri();
                if (uri != null) {
                    builder.add(FileStream.create(contentResolver, uri, getFileSize(contentResolver,
                            FileUtil.normalizeUri(uri))));
                }
            }
        } else if (data != null) {
            FileStream fileStream = getFileStream(contentResolver, data);
            if (fileStream.fileSize() != 0) {
                builder.add(fileStream);
            } else {
                File file = parseGetContentIntent(context, contentResolver, data, externallyOpenedFilesDirectory);
                if (file != null) {
                    Path renamedFile = FileUtil.renameFile(file.toPath(),
                            getFileName(file));
                    builder.add(FileStream.create(renamedFile.toFile()));
                }
            }
        }
        return builder.build();
    }


    private static String getFileName(File file) {
        if (Files.getFileExtension(file.getName()).isEmpty()) {
            String containerExtension = ContainerMimeTypeUtil.getContainerExtension(file);
            if (!containerExtension.isEmpty()) {
                return file.getName() + "." + containerExtension;
            } else if (SignedContainer.isCdoc(file)) {
                return file.getName() + ".cdoc";
            } else if (SignedContainer.isDdoc(file)) {
                return file.getName() + ".ddoc";
            } else if (FileUtil.isPDF(file)) {
                return file.getName() + ".pdf";
            }
        }

        return file.getName();
    }

    private static boolean isCdoc(File file) {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);
            NodeList nodes = doc.getElementsByTagName("denc:EncryptionProperty");
            for (int i = 0; i < nodes.getLength(); i++) {
                NamedNodeMap attributes = nodes.item(i).getAttributes();
                for (int j = 0; j < attributes.getLength(); j++) {
                    if (attributes.item(j).getNodeValue().equals("DocumentFormat")) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Timber.log(Log.ERROR, e, "XML parsing failed");
            return false;
        }

        return false;
    }

    public static File parseGetContentIntent(Context context, ContentResolver contentResolver, Uri uri,
                                            File externallyOpenedFilesDirectory) {
       return getExternallyOpenedFile(context, contentResolver, uri,
                    externallyOpenedFilesDirectory.getPath());
    }

    private static FileStream getFileStream(ContentResolver contentResolver, Uri uri) {
        long fileSize = getFileSize(contentResolver, FileUtil.normalizeUri(uri));
        return FileStream.create(contentResolver, uri, fileSize);
    }

    /**
     * Create an intent to send or open a local file in other apps.
     *
     * File path has to be shared with {@link FileProvider}.
     *
     * @param context Context to use for {@link FileProvider#getUriForFile(Context, String, File)}
     *                and to get authority string.
     * @param file File to send or open.
     * @param action Intent action for the content.
     * @return {@link Intent#ACTION_VIEW View intent} or {@link Intent#ACTION_SEND View intent} with content Uri of the file.
     */
    public static Intent createActionIntent(Context context, File file, String action) {
        Uri uri = FileProvider.getUriForFile(context,
                context.getString(R.string.file_provider_authority), file);

        Intent shareIntent = new Intent();
        shareIntent.setAction(action);

        if (Intent.ACTION_VIEW.equals(action)) {
            shareIntent.setDataAndType(uri, SignedContainer.mimeType(file));
        }

        if (Intent.ACTION_SEND.equals(action)) {
            shareIntent.setType(SignedContainer.mimeType(file));
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.setClipData(
                    new ClipData(file.getName(),
                    new String[]{ shareIntent.getType() },
                    new ClipData.Item(uri)
            ));
        }
        shareIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        // Remove app from "Share" and "Open with" menu
        ArrayList<ComponentName> excludedExtraComponents = new ArrayList<>();
        PackageManager packageManager = context.getPackageManager();
        for (ResolveInfo resolveInfo : packageManager.queryIntentActivities(shareIntent, 0)) {
            String appPackageName = context.getPackageName() != null ?
                    context.getPackageName() : APPLICATION_ID;
            String packageName = resolveInfo.activityInfo.packageName;
            if (packageName.contains(appPackageName)) {
                excludedExtraComponents.add(new ComponentName(packageName, resolveInfo.activityInfo.name));
            }
        }

        Intent intentChooser = Intent.createChooser(shareIntent, null);
        intentChooser.putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, excludedExtraComponents.toArray(new Parcelable[0]));
        return intentChooser;
    }

    public static Intent createSaveIntent(DataFile dataFile) {
        return Intent
                .createChooser(new Intent(Intent.ACTION_CREATE_DOCUMENT)
                        .addCategory(Intent.CATEGORY_OPENABLE)
                        .putExtra(Intent.EXTRA_TITLE, FileUtil.sanitizeString(dataFile.name(), ""))
                        .setType(getDataFileMimetype(dataFile))
                        .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION), null);
    }

    public static Intent createSaveIntent(File file, Context context) {
        String storagePath = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            StorageManager storageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
            StorageVolume storageVolume = storageManager.getPrimaryStorageVolume();
            Intent storageIntent = storageVolume.createOpenDocumentTreeIntent();

            Parcelable initialUri = storageIntent.getParcelableExtra(DocumentsContract.EXTRA_INITIAL_URI);
            if (initialUri != null) {
                storagePath = initialUri.toString();
                storagePath = StringUtils.replace(storagePath, "/root/", "/document/");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    storagePath += URLEncoder.encode(":", StandardCharsets.UTF_8) + DOCUMENTS_FOLDER;
                } else {
                    storagePath += URLEncoder.encode(":") + DOCUMENTS_FOLDER;
                }
            } else {
                Timber.log(Log.ERROR, "Unable to get initial URL");
                return null;
            }
        }
        File dir = new File(context.getFilesDir(), DIR_INTERNAL_FILES);
        File destFile = new File(dir, FileUtil.sanitizeString(file.getName(), ""));
        try {
            Files.copy(file, dir);
        } catch (IOException e) {
            Timber.log(Log.ERROR, e, "Unable to copy file to internal files folder");
            return null;
        }

        Uri fileUri = FileProvider.getUriForFile(context,
                context.getString(R.string.file_provider_authority), destFile);
        Intent intent = Intent
                .createChooser(new Intent(Intent.ACTION_CREATE_DOCUMENT)
                        .addCategory(Intent.CATEGORY_OPENABLE)
                        .putExtra(Intent.EXTRA_TITLE, FileUtil.sanitizeString(file.getName(), ""))
                        .putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.parse(storagePath))
                        .setDataAndType(fileUri, SignedContainer.mimeType(file))
                        .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION), null);

        List<ResolveInfo> resInfoList = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : resInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;
            context.grantUriPermission(packageName, fileUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

        return intent;
    }

    public static Intent createBrowserIntent(Context context, int stringRes, Configuration configuration) {
        String localizedUrl = context.createConfigurationContext(configuration).getText(stringRes).toString();
        return new Intent(Intent.ACTION_VIEW, Uri.parse(localizedUrl));
    }

    public static Intent setIntentData(Intent intent, Path filePath, android.app.Activity activity) {
        intent.setData(Uri.parse(filePath.toUri().toString()));
        intent.setClipData(ClipData.newRawUri(filePath.getFileName().toString(), FileProvider.getUriForFile(
                activity,
                activity.getString(R.string.file_provider_authority),
                filePath.toFile())));
        return intent;
    }

    private static String getDataFileMimetype(DataFile dataFile) {
        int extensionIndex = dataFile.name().lastIndexOf(".");
        String extension = extensionIndex != -1 ? dataFile.name().substring(extensionIndex + 1) : "";
        return !extension.isEmpty() ? MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) : "application/octet-stream";
    }

    private static long getFileSize(ContentResolver contentResolver, Uri uri) {
        Cursor cursor = contentResolver.
                query(FileUtil.normalizeUri(uri),
                        null, null, null, null);
        long fileSize = 0;
        if (cursor != null) {
            int columnIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
            if (cursor.moveToFirst() && !cursor.isNull(columnIndex)) {
                fileSize = cursor.getLong(columnIndex);
            }
            cursor.close();
            return fileSize;
        }
        return fileSize;
    }

    private static File getExternallyOpenedFile(Context context, ContentResolver contentResolver, Uri uri, String directory) {
        try (InputStream initialStream = contentResolver.openInputStream(uri)) {
            // File without extension, as we can't tell what type of file it is
            File externalFile = new File(directory + "/file");

            FileUtils.copyInputStreamToFile(initialStream, externalFile);

            boolean isContainer = SignedContainer.isContainer(context, externalFile);
            if (isContainer) {
                return SignedContainer.open(externalFile).file();
            }

            return externalFile;
        } catch (Exception e) {
            Timber.log(Log.ERROR, e, "Unable to read externally opened file data");
            return null;
        }
    }

    private IntentUtils() {}
}
