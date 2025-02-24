package ee.ria.DigiDoc.android.utils.container;

import android.content.Context;
import android.text.InputType;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;
import ee.ria.DigiDoc.android.utils.display.DisplayUtil;
import ee.ria.DigiDoc.common.FileUtil;

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
        int minimumHeightDp = DisplayUtil.getDisplayMetricsDpToInt(context.getResources(), 48);
        setMinimumHeight(minimumHeightDp);
        editText = new TextInputEditText(context);
        editText.setId(R.id.nameUpdateName);
        editText.setInputType(InputType.TYPE_CLASS_TEXT);
        editText.setMinHeight(minimumHeightDp);
        AccessibilityUtils.setEditTextCursorToEnd(editText);

        AccessibilityUtils.setTextViewContentDescription(context, false,
                getResources().getString(R.string.signature_update_name_update_name),
                getResources().getString(R.string.signature_update_name_update_name), editText);
        addView(editText, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }

    public String name() {
        return editText.getText().toString().trim();
    }

    public void name(String name) {
        if (name.startsWith(".")) {
            editText.setText(nameWithoutExtension(FileUtil.DEFAULT_FILENAME + name));
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
