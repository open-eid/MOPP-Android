package ee.ria.DigiDoc.android.utils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;

public final class TSLUtil {

    private static final String TSL_SEQUENCE_NUMBER_ELEMENT = "TSLSequenceNumber";

    private TSLUtil() {}

    public static Integer readSequenceNumber(InputStream tslInputStream) throws XmlPullParserException, IOException {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser parser = factory.newPullParser();
        parser.setInput(tslInputStream, null);
        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.getName().equals(TSL_SEQUENCE_NUMBER_ELEMENT)) {
                return Integer.parseInt(parser.nextText());
            }
            eventType = parser.next();
        }
        throw new TSLException("Error reading version from TSL");
    }

}
