package ee.ria.DigiDoc.android.signature.update;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import java.io.File;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.utils.navigator.conductor.ConductorScreen;

import static ee.ria.DigiDoc.android.utils.BundleUtils.getFile;
import static ee.ria.DigiDoc.android.utils.BundleUtils.putFile;

public final class SignatureUpdateScreen extends ConductorScreen {

    private static final String IS_EXISTING_CONTAINER = "isExistingContainer";
    private static final String IS_NESTED_CONTAINER = "isNestedContainer";
    private static final String CONTAINER_FILE = "containerFile";
    private static final String SIGNATURE_ADD_VISIBLE = "signatureAddVisible";
    private static final String SIGNATURE_ADD_SUCCESS_MESSAGE_VISIBLE =
            "signatureAddSuccessMessageVisible";

    public static SignatureUpdateScreen create(boolean isExistingContainer,
                                               boolean isNestedContainer, File containerFile,
                                               boolean signatureAddVisible,
                                               boolean signatureAddSuccessMessageVisible) {
        Bundle args = new Bundle();
        args.putBoolean(IS_EXISTING_CONTAINER, isExistingContainer);
        args.putBoolean(IS_NESTED_CONTAINER, isNestedContainer);
        putFile(args, CONTAINER_FILE, containerFile);
        args.putBoolean(SIGNATURE_ADD_VISIBLE, signatureAddVisible);
        args.putBoolean(SIGNATURE_ADD_SUCCESS_MESSAGE_VISIBLE, signatureAddSuccessMessageVisible);
        return new SignatureUpdateScreen(args);
    }

    private final boolean isExistingContainer;
    private final boolean isNestedContainer;
    private final File containerFile;
    private final boolean signatureAddVisible;
    private final boolean signatureAddSuccessMessageVisible;

    @SuppressWarnings("WeakerAccess")
    public SignatureUpdateScreen(Bundle args) {
        super(R.id.signatureUpdateScreen, args);
        isExistingContainer = args.getBoolean(IS_EXISTING_CONTAINER);
        isNestedContainer = args.getBoolean(IS_NESTED_CONTAINER);
        containerFile = getFile(args, CONTAINER_FILE);
        signatureAddVisible = args.getBoolean(SIGNATURE_ADD_VISIBLE);
        signatureAddSuccessMessageVisible = args.getBoolean(SIGNATURE_ADD_SUCCESS_MESSAGE_VISIBLE);
    }

    @Override
    protected View view(Context context) {
        return new SignatureUpdateView(context, getInstanceId(), isExistingContainer,
                isNestedContainer, containerFile, signatureAddVisible,
                signatureAddSuccessMessageVisible);
    }
}
