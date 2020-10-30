package ee.ria.DigiDoc.android.signature.update;

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
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
        if (name.startsWith(".")) {
            editText.setText(nameWithoutExtension("newFile" + name));
        } else {
            editText.setText(nameWithoutExtension(name));
        }
    }

    private String nameWithoutExtension(String name) {
            String[] containerNameParts = name.split("\\.");
            String containerNameExtension = containerNameParts[containerNameParts.length - 1];

            return name.replace("." + containerNameExtension, "");
    }

    public void error(@Nullable @StringRes Integer errorRes) {
        if (errorRes == null) {
            setError(null);
        } else {
            setError(getResources().getString(errorRes));
        }
    }
}
