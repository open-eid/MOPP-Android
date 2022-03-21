package ee.ria.DigiDoc.android.signature.update;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.common.RoleData;

public final class RoleAddView extends LinearLayout {

    private final EditText roleTextView;
    private final EditText cityTextView;
    private final EditText stateTextView;
    private final EditText countryTextView;
    private final EditText zipTextView;

    public RoleAddView(Context context) {
        this(context, null);
    }

    public RoleAddView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RoleAddView(Context context, @Nullable AttributeSet attrs,
                                           int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public RoleAddView(Context context, @Nullable AttributeSet attrs,
                                           int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setOrientation(VERTICAL);
        inflate(context, R.layout.signature_update_role_and_address, this);

        roleTextView = findViewById(R.id.signatureUpdateRoleText);
        cityTextView = findViewById(R.id.signatureUpdateRoleCityText);
        stateTextView = findViewById(R.id.signatureUpdateRoleStateText);
        countryTextView = findViewById(R.id.signatureUpdateRoleCountryText);
        zipTextView = findViewById(R.id.signatureUpdateRoleZipText);
    }

    public void reset(RoleViewModel viewModel) {
        roleTextView.setText(String.join(", ", viewModel.roles()));
        cityTextView.setText(viewModel.city());
        stateTextView.setText(viewModel.state());
        countryTextView.setText(viewModel.country());
        zipTextView.setText(viewModel.zip());
    }

    public RoleData request() {
        List<String> roles = Arrays.stream(roleTextView.getText().toString().split(","))
                .map(String::trim).collect(Collectors.toList());
        return RoleData.create(roles,
                cityTextView.getText().toString(), stateTextView.getText().toString(),
                countryTextView.getText().toString(), zipTextView.getText().toString());
    }
}