package ee.ria.DigiDoc.android.signature.update;

import static com.jakewharton.rxbinding4.widget.RxToolbar.navigationClicks;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Parcelable;
import android.widget.TextView;
import android.widget.Toolbar;

import androidx.coordinatorlayout.widget.CoordinatorLayout;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.ViewSavedState;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.Transaction;
import ee.ria.DigiDoc.common.TextUtil;
import ee.ria.DigiDoc.sign.Signature;

@SuppressLint("ViewConstructor")
public final class SignatureRoleDetailsView extends CoordinatorLayout {

    private final Navigator navigator;
    private final Toolbar toolbarView;

    private final ViewDisposables disposables = new ViewDisposables();

    public SignatureRoleDetailsView(Context context, Signature signature) {
        super(context);

        inflate(context, R.layout.signature_role_details_screen, this);
        AccessibilityUtils.setViewAccessibilityPaneTitle(this, R.string.signature_update_signature_role_and_address_title);

        navigator = Application.component(context).navigator();
        toolbarView = findViewById(R.id.toolbar);

        toolbarView.setTitle(TextUtil.capitalizeString(
                context.getString(R.string.signature_update_signature_role_and_address_title)));
        toolbarView.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbarView.setNavigationContentDescription(R.string.back);

        setData(signature);
    }

    private void setData(Signature signature) {
        TextView role = findViewById(R.id.signatureUpdateRoleDetailLabel);
        TextView city = findViewById(R.id.signatureUpdateRoleCityDetailLabel);
        TextView state = findViewById(R.id.signatureUpdateRoleStateDetailLabel);
        TextView country = findViewById(R.id.signatureUpdateRoleCountryDetailLabel);
        TextView zip = findViewById(R.id.signatureUpdateRoleZipDetailLabel);

        role.setText(formatString(String.join(", ", signature.roles())));
        city.setText(formatString(signature.city()));
        state.setText(formatString(signature.state()));
        country.setText(formatString(signature.country()));
        zip.setText(formatString(signature.zip()));
    }

    private String formatString(String text) {
        return String.format("%s%n%n", text);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        disposables.attach();
        disposables.add(navigationClicks(toolbarView).subscribe(o ->
                navigator.execute(Transaction.pop())));
    }

    @Override
    public void onDetachedFromWindow() {
        disposables.detach();
        super.onDetachedFromWindow();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        return ViewSavedState.onSaveInstanceState(super.onSaveInstanceState(), parcel -> { });
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(ViewSavedState.onRestoreInstanceState(state, parcel -> { }));
    }
}