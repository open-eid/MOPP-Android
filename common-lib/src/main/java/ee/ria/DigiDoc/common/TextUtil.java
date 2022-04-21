package ee.ria.DigiDoc.common;

public class TextUtil {

    public static String splitTextAndJoin(String text, String delimiter) {
        String[] splitText = text.split("");
        return String.join(delimiter, splitText);
    }

    public static boolean isOnlyDigits(String text) {
        return text.chars().allMatch(Character::isDigit);
    }
}