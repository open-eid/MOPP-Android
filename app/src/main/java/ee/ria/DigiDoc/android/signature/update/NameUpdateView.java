package ee.ria.DigiDoc.android.signature.update;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.text.InputType;
import android.util.AttributeSet;

import ee.ria.DigiDoc.R;

public final class NameUpdateView extends TextInputLayout {

    private final TextInputEditText editText;

    public NameUpdateView(Context context) {
        this(context, null);
    }

    public NameUpdateView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NameUpdateView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setHint(context.getString(R.string.signature_update_name_update_name));
        editText = new TextInputEditText(context);
        editText.setId(R.id.signatureUpdateNameUpdateName);
        editText.setInputType(InputType.TYPE_CLASS_TEXT);
        addView(editText, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }

    public String name() {
        return editText.getText().toString().trim();
    }

    public void name(String name) {
        editText.setText(name);
    }

    public void error(@Nullable @StringRes Integer errorRes) {
        if (errorRes == null) {
            setError(null);
        } else {
            setError(getResources().getString(errorRes));
        }
    }
}
