package ee.ria.DigiDoc.android.model.idcard;

import android.util.SparseArray;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class IdCardData {

    public abstract String givenNames();

    public abstract String surname();

    public abstract String personalCode();

    public static IdCardData create(SparseArray<String> data) {
        String givenName1 = data.get(2).trim();
        String givenName2 = data.get(3).trim();
        String surname = data.get(1).trim();
        String personalCode = data.get(7).trim();

        StringBuilder givenNames = new StringBuilder(givenName1);
        if (givenName2.length() > 0) {
            if (givenNames.length() > 0) {
                givenNames.append(" ");
            }
            givenNames.append(givenName2);
        }

        return create(givenNames.toString(), surname, personalCode);
    }

    private static IdCardData create(String givenNames, String surname, String personalCode) {
        return new AutoValue_IdCardData(givenNames, surname, personalCode);
    }
}
