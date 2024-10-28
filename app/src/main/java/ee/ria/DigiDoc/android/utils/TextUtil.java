package ee.ria.DigiDoc.android.utils;

import static android.util.TypedValue.COMPLEX_UNIT_PX;
import static android.view.View.GONE;
import static android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO;
import static android.view.View.VISIBLE;

import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.Layout;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.style.AlignmentSpan;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatTextView;

import com.google.android.material.textfield.TextInputLayout;

import ee.ria.DigiDoc.R;

public class TextUtil {

    public static AppCompatEditText getEditText(View view) {
        if (view instanceof final ScrollView scrollView) {
            final int scrollViewChildCount = scrollView.getChildCount();
            for (int i = 0; i < scrollViewChildCount; ++i) {
                final View scrollViewChild = scrollView.getChildAt(i);
                if (scrollViewChild instanceof final LinearLayout linearLayout) {
                    final int linearLayoutChildCount = linearLayout.getChildCount();
                    for (int j = 0; j < linearLayoutChildCount; ++j) {
                        final View linearLayoutChild = linearLayout.getChildAt(j);
                        if (linearLayoutChild instanceof AppCompatEditText) {
                            return (AppCompatEditText) linearLayoutChild;
                        }
                    }
                }
            }
        }

        return null;
    }

    public static AppCompatTextView getTextView(View view) {
        if (view instanceof final ScrollView scrollView) {
            final int scrollViewChildCount = scrollView.getChildCount();
            for (int i = 0; i < scrollViewChildCount; ++i) {
                final View scrollViewChild = scrollView.getChildAt(i);
                if (scrollViewChild instanceof final LinearLayout linearLayout) {
                    final int linearLayoutChildCount = linearLayout.getChildCount();
                    for (int j = 0; j < linearLayoutChildCount; ++j) {
                        final View linearLayoutChild = linearLayout.getChildAt(j);
                        if (linearLayoutChild instanceof AppCompatTextView) {
                            return (AppCompatTextView) linearLayoutChild;
                        }
                    }
                }
            }
        }

        return null;
    }

    /** @noinspection unused*/
    public static AppCompatTextView getTextInputLayoutAppCompatTextView(TextInputLayout textInputLayout) {
        for (int i = 0; i < textInputLayout.getChildCount(); i++) {
            final View textInputLayoutChild = textInputLayout.getChildAt(i);
            if (textInputLayoutChild instanceof final LinearLayout linearLayout) {
                for (int j = 0; j < linearLayout.getChildCount(); j++) {
                    final View linearLayoutChild = linearLayout.getChildAt(j);
                    if (linearLayoutChild instanceof final FrameLayout frameLayout) {
                        for (int k = 0; k < frameLayout.getChildCount(); k++) {
                            final View frameLayoutChild = frameLayout.getChildAt(k);
                            if (frameLayoutChild instanceof AppCompatTextView) {
                                return (AppCompatTextView) frameLayoutChild;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public static void setTextViewSizeInContainer(TextView textView) {
        textView.setAutoSizeTextTypeUniformWithConfiguration(
                1, 16, 1, TypedValue.COMPLEX_UNIT_DIP);
        textView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                textView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                SpannableString itemText = new SpannableString((textView).getText());
                itemText.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, itemText.length(), 0);
                textView.setText(itemText);
            }
        });
    }

    public static void handleDetailText(@Nullable String text, View detailView) {
        if (text != null && !text.isEmpty() && detailView instanceof TextView && detailView.getParent() != null) {
            ((TextView) detailView).setText(text);
            ((View) detailView.getParent()).setVisibility(VISIBLE);
        } else {
            if (detailView != null && detailView.getParent() != null) {
                ((View) detailView.getParent()).setVisibility(GONE);
            }
        }
    }

    public static int convertPxToDp(float size, Context context) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                size, context.getResources().getDisplayMetrics());
    }

    public static void setSearchViewTextSizeConstraints(SearchView searchView, TextView searchEditText) {
        float maxTextSize = 16;
        float minTextSize = 11;
        String queryHint = "";

        if (searchView.getQueryHint() != null) {
            queryHint = searchView.getQueryHint().toString();
        }

        int maxWidth = searchView.getWidth();

        TextPaint textPaint = new TextPaint();
        textPaint.setTextSize(maxTextSize);
        textPaint.setAntiAlias(true);

        float textWidth = textPaint.measureText(queryHint);

        while (textWidth > (float) maxWidth) {
            if (!(textPaint.getTextSize() < minTextSize)) {
                maxTextSize--;
                textPaint.setTextSize(maxTextSize);
                textWidth = textPaint.measureText(queryHint);
            }
        }

        searchEditText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, maxTextSize);
    }

    public static TextView getInvisibleElementTextView(Context context) {
        TextView textView = new TextView(context);
        textView.setText(R.string.last_invisible_element_name);
        textView.setTextColor(Color.GRAY);
        textView.setId(R.id.lastInvisibleElement);
        textView.setTag(R.string.last_invisible_element_tag);
        textView.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        textView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        textView.setEnabled(false);
        textView.setAlpha(0.001f);

        return textView;
    }

    public static TextWatcher addTextWatcher(EditText editText) {

        float defaultTextSize = editText.getTextSize();

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                TextPaint textPaint = editText.getPaint();
                float currentTextSize = editText.getTextSize();

                String currentText = editText.getText().toString();
                float viewWidth = editText.getWidth() - 80;

                float measureText = textPaint.measureText(currentText);

                while (measureText != 0 && measureText > viewWidth) {
                    measureText = textPaint.measureText(currentText);
                    currentTextSize -= 0.5;
                    editText.setTextSize(COMPLEX_UNIT_PX, currentTextSize);
                }

                while (measureText < viewWidth && currentTextSize <= defaultTextSize) {
                    measureText = textPaint.measureText(currentText);
                    currentTextSize += 0.5;
                    if (currentTextSize <= defaultTextSize) {
                        editText.setTextSize(COMPLEX_UNIT_PX, currentTextSize);
                    }
                }

                if (editText.getTextSize() <= 0) {
                    editText.setTextSize(COMPLEX_UNIT_PX, defaultTextSize);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        editText.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                editText.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                editText.addTextChangedListener(textWatcher);
            }
        });

        return textWatcher;
    }

    public static TextWatcher smartIdLatvianPersonalCodeHandler() {
        return new TextWatcher() {
            private boolean isUpdating = false;
            private String lastPersonalCode = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                lastPersonalCode = s.toString();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isUpdating) return;
                isUpdating = true;

                if (s != null) {
                    String currentText = s.toString();
                    String cleanedText = currentText.replace("-", "");

                    if (lastPersonalCode.endsWith("-") && !currentText.endsWith("-")) {
                        isUpdating = false;
                        return;
                    }

                    String newText = (cleanedText.length() >= 6)
                            ? cleanedText.substring(0, 6) + "-" + cleanedText.substring(6)
                            : cleanedText;

                    if (!newText.equals(currentText)) {
                        s.replace(0, s.length(), newText);
                    }
                }

                isUpdating = false;
            }
        };
    }

    public static void removeTextWatcher(EditText editText, TextWatcher textWatcher) {
        if (textWatcher != null) {
            editText.removeTextChangedListener(textWatcher);
        }
    }
}
