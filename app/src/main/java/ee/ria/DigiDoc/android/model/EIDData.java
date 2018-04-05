package ee.ria.DigiDoc.android.model;

public interface EIDData {

    @EIDType String type();

    String givenNames();

    String surname();

    String personalCode();

    String citizenship();
}
