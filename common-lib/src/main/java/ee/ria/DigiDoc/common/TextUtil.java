package ee.ria.DigiDoc.common;

import android.text.TextUtils;

public class TextUtil {

    public static String splitTextAndJoin(String text, String delimiter, String joinDelimiter) {
        String[] nameComponents = TextUtils.split(text, delimiter);
        return TextUtils.join(joinDelimiter, nameComponents);
    }

    public static boolean isOnlyDigits(String text) {
        return text.chars().allMatch(Character::isDigit);
    }
}
