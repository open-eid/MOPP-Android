package ee.ria.DigiDoc.android.signature.update;

import android.widget.RadioGroup;

import androidx.annotation.Nullable;

public interface SignatureAddView<T extends SignatureAddRequest, U extends SignatureAddResponse> {

    void reset(SignatureUpdateViewModel viewModel);

    T request();

    void response(@Nullable U response, @Nullable RadioGroup methodView);
}
