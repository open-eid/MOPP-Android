package ee.ria.DigiDoc.common;

import android.text.TextUtils;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TextUtil {

    public static String splitTextAndJoin(String text, String delimiter, String joinDelimiter) {
        String[] nameComponents = TextUtils.split(text, delimiter);
        return TextUtils.join(joinDelimiter, nameComponents);
    }

    public static boolean isOnlyDigits(String text) {
        return text.chars().allMatch(Character::isDigit);
    }

    public static String capitalizeString(String text) {
        return StringUtils.capitalize(text.toLowerCase());
    }

    public static boolean isEmpty(String text) {
        return StringUtils.isEmpty(text);
    }

    public static List<String> removeEmptyStrings(List<String> strings) {
        ArrayList<String> stringList = new ArrayList<>(strings);
        return stringList.stream()
                .filter(s -> !StringUtils.isBlank(s))
                .collect(Collectors.toList());
    }
}
