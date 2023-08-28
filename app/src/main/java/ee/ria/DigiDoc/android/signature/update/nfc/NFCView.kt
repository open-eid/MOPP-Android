package ee.ria.DigiDoc.android.signature.update.nfc

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.View.OnFocusChangeListener
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textview.MaterialTextView
import com.jakewharton.rxbinding4.widget.afterTextChangeEvents
import ee.ria.DigiDoc.R
import ee.ria.DigiDoc.android.Constants
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils
import ee.ria.DigiDoc.android.signature.update.SignatureAddView
import ee.ria.DigiDoc.android.signature.update.SignatureUpdateViewModel
import ee.ria.DigiDoc.android.utils.ErrorMessageUtil
import ee.ria.DigiDoc.android.utils.mvi.State
import ee.ria.DigiDoc.common.PinConstants
import ee.ria.DigiDoc.idcard.Token
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subjects.Subject

class NFCView @JvmOverloads constructor(
    context: Context?, attrs: AttributeSet? = null, defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes),
    SignatureAddView<NFCRequest, NFCResponse?> {
    //private var token: Token? = null
    private val positiveButtonStateSubject: Subject<Any> = PublishSubject.create()
    private val message by lazy { findViewById<TextView>(R.id.signatureUpdateNFCMessage) }
    private val canView by lazy { findViewById<EditText>(R.id.signatureUpdateNFCCAN) }
    private val pinView by lazy { findViewById<EditText>(R.id.signatureUpdateNFCPIN2) }
    private val canLayout by lazy { findViewById<TextInputLayout>(R.id.signatureUpdateNFCCANLayout) }
    private val pinLayout by lazy { findViewById<TextInputLayout>(R.id.signatureUpdateNFCPIN2Layout) }
    private val canLabel by lazy { findViewById<MaterialTextView>(R.id.signatureUpdateNFCCANLabel) }
    private val pinLabel by lazy { findViewById<MaterialTextView>(R.id.signatureUpdateNFCPIN2Label) }

    init {
        orientation = VERTICAL
        inflate(context, R.layout.signature_update_nfc, this)
        AccessibilityUtils.setSingleCharactersContentDescription(canView)
        AccessibilityUtils.setSingleCharactersContentDescription(pinView)
        AccessibilityUtils.setEditTextCursorToEnd(canView)
        AccessibilityUtils.setEditTextCursorToEnd(pinView)
        checkInputsValidity()
    }

    fun positiveButtonState(): Observable<Any> {
        return Observable.merge(positiveButtonStateSubject, pinView.afterTextChangeEvents())
    }

    fun positiveButtonEnabled(): Boolean {
        val pinCodeText = pinView.text
        return /* token != null && */ pinCodeText != null && isPinLengthEnough(pinCodeText.toString())
    }

    override fun reset(viewModel: SignatureUpdateViewModel?) {
        canView.setText("")
        pinView.setText("")
        AccessibilityUtils.setEditTextCursorToEnd(canView)
        AccessibilityUtils.setEditTextCursorToEnd(pinView)
        ErrorMessageUtil.setTextViewError(context, null, canLabel, canLayout, canView)
        ErrorMessageUtil.setTextViewError(context, null, pinLabel, pinLayout, pinView)
        message.clearFocus()
        canView.clearFocus()
        pinView.clearFocus()
    }

    override fun request(): NFCRequest {
        return NFCRequest(canView.text.toString(), pinView.text.toString())
    }

    override fun response(response: NFCResponse?, methodView: RadioGroup?) {
        positiveButtonStateSubject.onNext(Constants.VOID)
    }

    private fun checkInputsValidity() {
        checkPinCodeValidity()
        pinView.onFocusChangeListener =
            OnFocusChangeListener { view: View?, hasfocus: Boolean -> checkPinCodeValidity() }
    }

    private fun checkPinCodeValidity() {
        pinLabel.error = null
        val pinCodeView = pinView.text
        if (pinCodeView != null && !pinCodeView.toString().isEmpty() &&
            !isPinLengthEnough(pinCodeView.toString())
        ) {
            pinLabel.error = resources.getString(
                R.string.id_card_sign_pin_invalid_length,
                resources.getString(R.string.signature_id_card_pin2),
                Integer.toString(PinConstants.PIN2_MIN_LENGTH)
            )
        }
    }

    private fun isPinLengthEnough(pin: String): Boolean {
        return pin.length >= PinConstants.PIN2_MIN_LENGTH
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
    }
}