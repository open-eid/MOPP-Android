package ee.ria.DigiDoc.android.signature.update;

import android.content.Context;
import android.content.DialogInterface;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringDef;
import android.text.Html;
import android.text.TextUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.model.mobileid.MobileIdMessageException;
import ee.ria.DigiDoc.android.model.smartid.SmartIdMessageException;
import ee.ria.DigiDoc.android.utils.ClickableDialogUtil;
import ee.ria.DigiDoc.android.utils.ErrorMessageUtil;
import ee.ria.DigiDoc.android.utils.widget.ErrorDialog;
import ee.ria.DigiDoc.idcard.CodeVerificationException;
import ee.ria.DigiDoc.sign.CertificateRevokedException;
import ee.ria.DigiDoc.sign.NoInternetConnectionException;
import ee.ria.DigiDoc.sign.OcspInvalidTimeSlotException;
import ee.ria.DigiDoc.sign.TooManyRequestsException;
import ee.ria.DigiDoc.sign.utils.ErrorMessage;
import io.reactivex.subjects.Subject;

import static ee.ria.DigiDoc.android.signature.update.SignatureUpdateErrorDialog.Type.DOCUMENTS_ADD;
import static ee.ria.DigiDoc.android.signature.update.SignatureUpdateErrorDialog.Type.DOCUMENT_REMOVE;
import static ee.ria.DigiDoc.android.signature.update.SignatureUpdateErrorDialog.Type.SIGNATURE_ADD;
import static ee.ria.DigiDoc.android.signature.update.SignatureUpdateErrorDialog.Type.SIGNATURE_REMOVE;

public final class SignatureUpdateErrorDialog extends ErrorDialog implements DialogInterface.OnDismissListener {

    @StringDef({DOCUMENTS_ADD, DOCUMENT_REMOVE, SIGNATURE_ADD, SIGNATURE_REMOVE})
    @Retention(RetentionPolicy.SOURCE)
    @interface Type {
        String DOCUMENTS_ADD = "DOCUMENTS_ADD";
        String DOCUMENT_REMOVE = "DOCUMENT_REMOVE";
        String SIGNATURE_ADD = "SIGNATURE_ADD";
        String SIGNATURE_REMOVE = "SIGNATURE_REMOVE";
    }

    private final Subject<Intent.DocumentsAddIntent> documentsAddIntentSubject;
    private final Subject<Intent.DocumentRemoveIntent> documentRemoveIntentSubject;
    private final Subject<Intent.SignatureAddIntent> signatureAddIntentSubject;
    private final Subject<Intent.SignatureRemoveIntent> signatureRemoveIntentSubject;
    private final SignatureUpdateSignatureAddDialog signatureAddDialog;

    private String type;

    SignatureUpdateErrorDialog(@NonNull Context context,
                               Subject<Intent.DocumentsAddIntent> documentsAddIntentSubject,
                               Subject<Intent.DocumentRemoveIntent> documentRemoveIntentSubject,
                               Subject<Intent.SignatureAddIntent> signatureAddIntentSubject,
                               Subject<Intent.SignatureRemoveIntent> signatureRemoveIntentSubject,
                               SignatureUpdateSignatureAddDialog signatureAddDialog) {
        super(context);
        this.documentsAddIntentSubject = documentsAddIntentSubject;
        this.documentRemoveIntentSubject = documentRemoveIntentSubject;
        this.signatureAddIntentSubject = signatureAddIntentSubject;
        this.signatureRemoveIntentSubject = signatureRemoveIntentSubject;
        this.signatureAddDialog = signatureAddDialog;
        setButton(BUTTON_POSITIVE, context.getString(android.R.string.ok), (dialog, which) -> {});
        setOnDismissListener(this);
    }

    void show(@Nullable Throwable documentsAddError, @Nullable Throwable documentRemoveError,
              @Nullable Throwable signatureAddError, @Nullable Throwable signatureRemoveError) {
        if (documentsAddError != null) {
            type = DOCUMENTS_ADD;
            setMessage(getContext().getString(
                    R.string.signature_update_documents_add_error_exists));
        } else if (documentRemoveError != null) {
            type = DOCUMENT_REMOVE;
            setMessage(getContext().getString(R.string.signature_update_document_remove_error));
        } else if (signatureAddError != null) {
            type = SIGNATURE_ADD;
            if (signatureAddError instanceof CodeVerificationException) {
                setMessage(getContext().getString(
                        R.string.signature_update_id_card_sign_pin2_locked));
            } else if (signatureAddError instanceof NoInternetConnectionException) {
                setMessage(getContext().getString(R.string.no_internet_connection));
            } else if (signatureAddError instanceof MobileIdMessageException ||
                    signatureAddError instanceof SmartIdMessageException) {
                String link = ErrorMessageUtil.extractLink(signatureAddError.getMessage());
                if (!link.isEmpty()) {
                    setMessage(
                            Html.fromHtml("<span>" +
                                    ErrorMessageUtil.removeLink(signatureAddError.getMessage()) + "</span><a href=" + link + ">" +
                                    getTextFromTranslation(R.string.signature_update_signature_error_message_additional_information) + "</a>"
                            )
                    );
                } else {
                    setMessage(signatureAddError.getMessage());
                }
            } else if (signatureAddError instanceof TooManyRequestsException) {
                setMessage(Html.fromHtml(ErrorMessage.withURL(
                        getContext(),
                        R.string.signature_update_signature_error_message_too_many_requests,
                        R.string.signature_update_signature_error_message_additional_information
                )));
            } else if (signatureAddError instanceof OcspInvalidTimeSlotException) {
                setMessage(Html.fromHtml(ErrorMessage.withURL(
                        getContext(),
                        R.string.signature_update_signature_error_message_invalid_time_slot,
                        R.string.signature_update_signature_error_message_additional_information
                )));
            } else if (signatureAddError instanceof CertificateRevokedException) {
                setMessage(getContext().getString(R.string.signature_update_signature_error_message_certificate_revoked));
            } else {
                setTitle(R.string.signature_update_signature_add_error);
                setMessage(signatureAddError.getMessage());
            }
        } else if (signatureRemoveError != null) {
            type = SIGNATURE_REMOVE;
            setMessage(getContext().getString(R.string.signature_update_signature_remove_error));
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
        if (TextUtils.equals(type, DOCUMENTS_ADD)) {
            documentsAddIntentSubject.onNext(Intent.DocumentsAddIntent.clear());
        } else if (TextUtils.equals(type, DOCUMENT_REMOVE)) {
            documentRemoveIntentSubject.onNext(Intent.DocumentRemoveIntent.clear());
        } else if (TextUtils.equals(type, SIGNATURE_ADD)) {
            signatureAddIntentSubject.onNext(Intent.SignatureAddIntent.clear());
        } else if (TextUtils.equals(type, SIGNATURE_REMOVE)) {
            signatureRemoveIntentSubject.onNext(Intent.SignatureRemoveIntent.clear());
        }
    }
}
