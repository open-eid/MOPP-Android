package ee.ria.DigiDoc.android.signature.list;

import android.content.Context;
import android.util.AttributeSet;

public class MiddleMultilineTextView extends androidx.appcompat.widget.AppCompatTextView {

    private final String SYMBOL = "…";
    private final int SYMBOL_LENGTH = SYMBOL.length();

    public MiddleMultilineTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (getMaxLines() > 1) {
            int originalLength = getText().length();
            int visibleLength = getVisibleLength();

            if (originalLength > visibleLength) {
                setText(smartTrim(getText().toString(), visibleLength - SYMBOL_LENGTH));
            }
        }
    }

    private String smartTrim(String string, int maxLength) {
        if (string == null)
            return null;
        if (maxLength < 1)
            return string;
        if (string.length() <= maxLength)
            return string;
        if (maxLength == 1)
            return string.charAt(0) + SYMBOL;

        int midpoint = (int) Math.ceil((double) string.length() / 2);
        int toremove = string.length() - maxLength;
        int lstrip = (int) Math.ceil((double) toremove / 2);
        int rstrip = toremove - lstrip;

        return string.substring(0, midpoint - lstrip) + SYMBOL + string.substring(midpoint + rstrip);
    }

    private int getVisibleLength() {
        int start = getLayout().getLineStart(0);
        int end = getLayout().getLineVisibleEnd(getMaxLines() - 1);
        return getText().toString().substring(start, end).length();
    }
}