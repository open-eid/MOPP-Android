package ee.ria.DigiDoc.android.model.idcard;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import org.threeten.bp.LocalDate;

import ee.ria.DigiDoc.android.model.EIDData;
import ee.ria.DigiDoc.android.model.EIDType;

@AutoValue
public abstract class IdCardData implements EIDData {

    public abstract String documentNumber();

    @Nullable public abstract LocalDate expiryDate();

    static IdCardData create(@Nullable @EIDType String type, String givenNames, String surname,
                             String personalCode, String citizenship, String documentNumber,
                             @Nullable LocalDate expiryDate) {
        return new AutoValue_IdCardData(type, givenNames, surname, personalCode, citizenship,
                documentNumber, expiryDate);
    }
}
