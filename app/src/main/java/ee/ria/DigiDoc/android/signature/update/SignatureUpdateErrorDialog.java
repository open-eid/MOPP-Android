package ee.ria.DigiDoc.android.signature.update;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static ee.ria.DigiDoc.android.signature.update.SignatureUpdateErrorDialog.Type.DOCUMENTS_ADD;
import static ee.ria.DigiDoc.android.signature.update.SignatureUpdateErrorDialog.Type.DOCUMENT_REMOVE;
import static ee.ria.DigiDoc.android.signature.update.SignatureUpdateErrorDialog.Type.SIGNATURE_ADD;
import static ee.ria.DigiDoc.android.signature.update.SignatureUpdateErrorDialog.Type.SIGNATURE_REMOVE;
import static ee.ria.DigiDoc.android.utils.display.DisplayUtil.getDeviceLayoutWidth;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringDef;
import androidx.core.content.ContextCompat;

import java.io.FileNotFoundException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.signature.update.exception.DocumentExistsException;
import ee.ria.DigiDoc.android.signature.update.exception.DocumentRemoveException;
import ee.ria.DigiDoc.android.signature.update.exception.GeneralSignatureUpdateException;
import ee.ria.DigiDoc.android.utils.ClickableDialogUtil;
import ee.ria.DigiDoc.android.utils.ErrorMessageUtil;
import ee.ria.DigiDoc.android.utils.files.EmptyFileException;
import ee.ria.DigiDoc.android.utils.widget.ErrorDialog;
import ee.ria.DigiDoc.common.DetailMessageException;
import ee.ria.DigiDoc.common.DetailMessageSource;
import ee.ria.DigiDoc.common.exception.SignatureUpdateDetailError;
import ee.ria.DigiDoc.common.exception.SignatureUpdateError;
import ee.ria.DigiDoc.idcard.CodeVerificationException;
import ee.ria.DigiDoc.sign.CertificateRevokedException;
import ee.ria.DigiDoc.sign.NoInternetConnectionException;
import ee.ria.DigiDoc.sign.OcspInvalidTimeSlotException;
import ee.ria.DigiDoc.sign.TooManyRequestsException;
import ee.ria.DigiDoc.sign.utils.UrlMessage;
import io.reactivex.rxjava3.subjects.Subject;

public final class SignatureUpdateErrorDialog extends ErrorDialog implements DialogInterface.OnDismissListener {

    @StringDef({DOCUMENTS_ADD, DOCUMENT_REMOVE, SIGNATURE_ADD, SIGNATURE_REMOVE})
    @Retention(RetentionPolicy.SOURCE)
    @interface Type {
        String DOCUMENTS_ADD = "DOCUMENTS_ADD";
        String DOCUMENT_REMOVE = "DOCUMENT_REMOVE";
        String SIGNATURE_ADD = "SIGNATURE_ADD";
        String SIGNATURE_REMOVE = "SIGNATURE_REMOVE";
    }

    private final Subject<DocumentsAddIntent> documentsAddIntentSubject;
    private final Subject<DocumentRemoveIntent> documentRemoveIntentSubject;
    private final Subject<SignatureAddIntent> signatureAddIntentSubject;
    private final Subject<SignatureRemoveIntent> signatureRemoveIntentSubject;
    private final Subject<SignatureRoleViewIntent> signatureRoleViewIntentSubject;
    private final SignatureUpdateSignatureAddDialog signatureAddDialog;

    private String type;

    private View view;
    private View.OnLayoutChangeListener layoutChangeListener;

    SignatureUpdateErrorDialog(@NonNull Context context,
                               Subject<DocumentsAddIntent> documentsAddIntentSubject,
                               Subject<DocumentRemoveIntent> documentRemoveIntentSubject,
                               Subject<SignatureAddIntent> signatureAddIntentSubject,
                               Subject<SignatureRoleViewIntent> signatureRoleViewIntentSubject,
                               Subject<SignatureRemoveIntent> signatureRemoveIntentSubject,
                               SignatureUpdateSignatureAddDialog signatureAddDialog,
                               View view) {
        super(context, R.style.UniformDialog);
        this.documentsAddIntentSubject = documentsAddIntentSubject;
        this.documentRemoveIntentSubject = documentRemoveIntentSubject;
        this.signatureAddIntentSubject = signatureAddIntentSubject;
        this.signatureRoleViewIntentSubject = signatureRoleViewIntentSubject;
        this.signatureRemoveIntentSubject = signatureRemoveIntentSubject;
        this.signatureAddDialog = signatureAddDialog;
        this.view = view;
        setButton(BUTTON_POSITIVE, context.getString(android.R.string.ok), (dialog, which) -> {});
        setOnDismissListener(this);
    }

    void show(@Nullable Throwable documentsAddError, @Nullable Throwable documentRemoveError,
              @Nullable Throwable signatureAddError, @Nullable Throwable signatureRemoveError) {

        Window window = getWindow();

        setCustomLayoutChangeListener(window);
        view.addOnLayoutChangeListener(getCustomLayoutChangeListener());

        SignatureUpdateError updateError = null;
        SignatureUpdateDetailError detailError = null;

        if (documentsAddError != null) {
            type = DOCUMENTS_ADD;
            if (documentsAddError instanceof EmptyFileException) {
                updateError = new EmptyFileException();
            } else if (documentsAddError instanceof NoInternetConnectionException) {
                updateError = new NoInternetConnectionException();
            } else if (documentsAddError instanceof FileNotFoundException) {
                if (documentsAddError.getMessage() != null &&
                        documentsAddError.getMessage().contains("connection_failure")) {
                    updateError = new NoInternetConnectionException();
                } else {
                    updateError = new EmptyFileException();
                }
            } else {
                updateError = new DocumentExistsException();
            }
        } else if (documentRemoveError != null) {
            type = DOCUMENT_REMOVE;
            updateError = new DocumentRemoveException();
        } else if (signatureAddError != null) {
            type = SIGNATURE_ADD;
            if (signatureAddError instanceof CodeVerificationException) {
                updateError = new CodeVerificationException(
                        ((CodeVerificationException) signatureAddError).getType(),
                        getContext().getString(
                                R.string.signature_update_id_card_sign_pin2_locked)
                );
            } else if (signatureAddError instanceof NoInternetConnectionException) {
                updateError = new NoInternetConnectionException();
            } else if (signatureAddError instanceof DetailMessageSource) {
                String link = ErrorMessageUtil.extractLink(signatureAddError.getMessage());
                if (!link.isEmpty()) {
                    detailError = new DetailMessageException(Html.fromHtml("<span>" +
                                    ErrorMessageUtil.removeLink(signatureAddError.getMessage()) + "</span><a href=" + link + ">" +
                                    getTextFromTranslation(R.string.signature_update_signature_error_message_additional_information) + "</a>",
                            Html.FROM_HTML_MODE_LEGACY));
                } else {
                    setTitle(R.string.signature_update_signature_add_error_title);
                    if (((DetailMessageSource) signatureAddError).getDetailMessage() != null &&
                                    !((DetailMessageSource) signatureAddError).getDetailMessage().isEmpty()) {
                        String errorMessage = getContext().getString(R.string.signature_update_signature_error_message_details) +
                                ":\n" +
                                ((DetailMessageSource) signatureAddError).getDetailMessage();
                        detailError = new DetailMessageException(errorMessage);
                    } else {
                        detailError = new DetailMessageException(signatureAddError.getMessage());
                    }
                }
            } else if (signatureAddError instanceof TooManyRequestsException) {
                detailError = new TooManyRequestsException(
                        Html.fromHtml(UrlMessage.withURL(
                                getContext(),
                                R.string.signature_update_signature_error_message_too_many_requests,
                                R.string.signature_update_signature_error_message_additional_information
                        ), Html.FROM_HTML_MODE_LEGACY));
            } else if (signatureAddError instanceof OcspInvalidTimeSlotException) {
                detailError = new OcspInvalidTimeSlotException(Html.fromHtml(UrlMessage.withURL(
                        getContext(),
                        R.string.signature_update_signature_error_message_invalid_time_slot,
                        R.string.signature_update_signature_error_message_additional_information
                ), Html.FROM_HTML_MODE_LEGACY));
            } else if (signatureAddError instanceof CertificateRevokedException) {
                updateError = new CertificateRevokedException(
                        getContext().getString(R.string.signature_update_signature_error_message_certificate_revoked)
                );
            } else {
                setTitle(R.string.signature_update_signature_add_error);
                updateError = new GeneralSignatureUpdateException(signatureAddError.getMessage());
            }
        } else if (signatureRemoveError != null) {
            type = SIGNATURE_REMOVE;
            updateError = new GeneralSignatureUpdateException(
                    getContext().getString(R.string.signature_update_signature_remove_error));
        }

        if (updateError != null) {
            String message = updateError.getMessage(getContext());
            setMessage(message);
        } else if (detailError != null) {
            Spanned detailMessage = detailError.getDetailMessage(getContext());
            if (detailMessage == null) {
                setMessage(detailError.getMessage(getContext()));
            } else {
                setMessage(detailMessage);
            }
        } else {
            dismiss();
            return;
        }
        show();

        ClickableDialogUtil.makeLinksInDialogClickable(this);
    }

    private String getTextFromTranslation(int textId) {
        return getContext().getResources().getString(textId);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        setTitle(null);
        removeListeners();
        if (TextUtils.equals(type, DOCUMENTS_ADD)) {
            documentsAddIntentSubject.onNext(DocumentsAddIntent.clear());
        } else if (TextUtils.equals(type, DOCUMENT_REMOVE)) {
            documentRemoveIntentSubject.onNext(DocumentRemoveIntent.clear());
        } else if (TextUtils.equals(type, SIGNATURE_ADD)) {
            signatureAddIntentSubject.onNext(SignatureAddIntent.clear());
        } else if (TextUtils.equals(type, SIGNATURE_REMOVE)) {
            signatureRemoveIntentSubject.onNext(SignatureRemoveIntent.clear());
        }
    }

    // Prevent Dialog width change when rotating screen
    private void setCustomLayoutChangeListener(Window window) {
        layoutChangeListener = (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                window.setLayout(getDeviceLayoutWidth(getContext()), WRAP_CONTENT);
    }

    private View.OnLayoutChangeListener getCustomLayoutChangeListener() {
        return layoutChangeListener;
    }

    private void removeListeners() {
        if (layoutChangeListener == null) { return; }
        view.removeOnLayoutChangeListener(layoutChangeListener);
        layoutChangeListener = null;
    }
}
