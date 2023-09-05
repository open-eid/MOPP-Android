package ee.ria.DigiDoc.android.main.settings.role;

import static com.jakewharton.rxbinding4.widget.RxCompoundButton.checkedChanges;
import static com.jakewharton.rxbinding4.widget.RxToolbar.navigationClicks;
import static ee.ria.DigiDoc.android.main.settings.util.SettingsUtil.getToolbarViewTitle;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;
import android.widget.Toolbar;

import androidx.appcompat.widget.SwitchCompat;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Activity;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.main.settings.SettingsDataStore;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.android.utils.navigator.Transaction;

public final class SettingsRoleAndAddressView extends CoordinatorLayout {

    private final Toolbar toolbarView;
    private final SwitchCompat askRoleAndAddressSwitch;

    private final Navigator navigator;
    private final SettingsDataStore settingsDataStore;
    private final ViewDisposables disposables;

    public SettingsRoleAndAddressView(Context context) {
        this(context, null);
    }

    public SettingsRoleAndAddressView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SettingsRoleAndAddressView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.main_settings_role_and_address, this);
        toolbarView = findViewById(R.id.toolbar);
        TextView toolbarTitleView = getToolbarViewTitle(toolbarView);
        navigator = Application.component(context).navigator();
        settingsDataStore = Application.component(context).settingsDataStore();
        disposables = new ViewDisposables();

        toolbarView.setTitle(R.string.main_settings_role_and_address_button);
        toolbarView.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbarView.setNavigationContentDescription(R.string.back);

        if (toolbarTitleView != null) {
            toolbarTitleView.setContentDescription("\u202F");
        }

        Activity activityContext = (Activity) this.getContext();

        askRoleAndAddressSwitch = findViewById(R.id.mainSettingsAskRoleAndAddress);
        if (askRoleAndAddressSwitch != null && activityContext != null) {
            askRoleAndAddressSwitch.setChecked(activityContext.getSettingsDataStore().getIsRoleAskingEnabled());

            askRoleAndAddressSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                    activityContext.getSettingsDataStore().setIsRoleAskingEnabled(isChecked));
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        disposables.attach();
        disposables.add(navigationClicks(toolbarView).subscribe(o ->
                navigator.execute(Transaction.pop())));
        disposables.add(checkedChanges(askRoleAndAddressSwitch)
                .subscribe(settingsDataStore::setIsRoleAskingEnabled));
    }

    @Override
    public void onDetachedFromWindow() {
        disposables.detach();
        super.onDetachedFromWindow();
    }
}
