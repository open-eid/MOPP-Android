package ee.ria.DigiDoc.sign.utils;

import android.content.Context;
import android.content.res.Resources;
import android.util.Patterns;

import androidx.annotation.StringRes;

import java.util.regex.Matcher;

import ee.ria.DigiDoc.sign.R;

public class UrlMessage {

    public static String withURL(Context context, @StringRes int messageTranslation, @StringRes int urlMessageTranslation) {
        String message = getTextFromTranslation(context, messageTranslation);
        Matcher urlMatcher = Patterns.WEB_URL.matcher(message);
        if (urlMatcher.find()) {
            return message.replace(urlMatcher.group(),"") + "</span> <a href=" + urlMatcher.group() + ">" +
                    getTextFromTranslation(context, urlMessageTranslation) + "</a>";
        }
        return message;
    }

    public static String withURLAndQuestion(Context context, @StringRes int messageTranslation,
                                            @StringRes  int urlMessageTranslation,
                                            @StringRes int continueQuestion) {
        String message = getTextFromTranslation(context, messageTranslation);
        Matcher urlMatcher = Patterns.WEB_URL.matcher(message);
        if (urlMatcher.find()) {
            return "<span>" +
                    message.replace(urlMatcher.group(),"") + "</span> <a href=" + urlMatcher.group() + ">" +
                    getTextFromTranslation(context, urlMessageTranslation) + "</a>. <br />" +
                    getTextFromTranslation(context, continueQuestion);
        }
        return message;
    }

    private static String getTextFromTranslation(Context context, int textId) {
        Resources resources = context.getResources();
        if (textId == R.string.signature_update_signature_error_message_too_many_requests) {
            return resources.getString(textId, resources.getString(R.string.id_card_conditional_speech));
        }
        return resources.getString(textId);
    }

    private static String extractLink(String text) {
        Matcher m = Patterns.WEB_URL.matcher(text);
        while (m.find()) {
            return m.group();
        }

        return "";
    }

    private static String removeLink(String text) {
        Matcher m = Patterns.WEB_URL.matcher(text);
        while (m.find()) {
            return text.replace(m.group(),"");
        }

        return text;
    }

}
