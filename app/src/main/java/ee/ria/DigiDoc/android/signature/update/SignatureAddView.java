package ee.ria.DigiDoc.android.signature.update;

public interface SignatureAddView<T extends SignatureAddRequest> {

    void reset(SignatureUpdateViewModel viewModel);

    T request();
}
