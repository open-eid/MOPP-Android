package ee.ria.DigiDoc.android.signature.update;

public interface SignatureAddView<T extends SignatureAddRequest> {

    void init(T request);

    T request();
}
