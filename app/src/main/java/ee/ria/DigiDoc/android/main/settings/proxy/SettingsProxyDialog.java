package ee.ria.DigiDoc.android.main.settings.proxy;

import static com.jakewharton.rxbinding4.view.RxView.clicks;
import static com.jakewharton.rxbinding4.widget.RxRadioGroup.checkedChanges;
import static com.jakewharton.rxbinding4.widget.RxTextView.textChanges;
import static ee.ria.DigiDoc.common.ProxySetting.MANUAL_PROXY;
import static ee.ria.DigiDoc.common.ProxySetting.NO_PROXY;
import static ee.ria.DigiDoc.common.ProxySetting.SYSTEM_PROXY;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.jakewharton.rxbinding4.widget.RxCompoundButton;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.ApplicationApp;
import ee.ria.DigiDoc.android.main.settings.SettingsDataStore;
import ee.ria.DigiDoc.android.utils.SecureUtil;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.common.ManualProxy;
import ee.ria.DigiDoc.common.ProxyConfig;
import ee.ria.DigiDoc.common.ProxySetting;
import ee.ria.DigiDoc.common.ProxyUtil;
import ee.ria.DigiDoc.sign.SignLib;
import timber.log.Timber;

public class SettingsProxyDialog extends Dialog {

    private final Navigator navigator;
    private final SettingsDataStore settingsDataStore;

    private final ViewDisposables disposables;

    private final RadioGroup proxyGroup;
    private final RadioButton noProxy;
    private final RadioButton systemProxy;
    private final RadioButton manualProxy;

    private final TextInputEditText host;
    private final TextInputEditText port;
    private final TextInputEditText username;
    private final TextInputEditText password;

    private final TextInputLayout portLayout;

    private final ManualProxy manualProxySettings;

    private final ImageButton backButton;

    public SettingsProxyDialog(Context context) {
        super(context);

        Window window = getWindow();
        if (window != null) {
            SecureUtil.markAsSecure(context, window);
        }

        setContentView(R.layout.main_settings_proxy_dialog_layout);

        navigator = ApplicationApp.component(context).navigator();
        settingsDataStore = ApplicationApp.component(context).settingsDataStore();
        disposables = new ViewDisposables();

        proxyGroup = findViewById(R.id.mainSettingsProxyGroup);
        noProxy = findViewById(R.id.mainSettingsProxyNoProxy);
        systemProxy = findViewById(R.id.mainSettingsProxyUseSystem);
        manualProxy = findViewById(R.id.mainSettingsProxyManual);

        host = findViewById(R.id.mainSettingsProxyHost);
        port = findViewById(R.id.mainSettingsProxyPort);
        username = findViewById(R.id.mainSettingsProxyUsername);
        password = findViewById(R.id.mainSettingsProxyPassword);

        portLayout = findViewById(R.id.mainSettingsProxyPortLayout);

        manualProxySettings = getManualProxySettings();

        backButton = findViewById(R.id.mainSettingsProxyBackButton);
        backButton.requestFocus();

        checkActiveProxySetting(settingsDataStore);
        checkManualProxySettings(settingsDataStore, manualProxySettings);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        disposables.attach();
        disposables.add(clicks(backButton).subscribe(o -> dismiss()));
        disposables.add(textChanges(port)
                .map(CharSequence::toString)
                .subscribe(this::validatePortNumber));
        disposables.add(checkedChanges(proxyGroup).subscribe(setting ->
                setProxySetting(settingsDataStore, setting)));
    }

    @Override
    public void onDetachedFromWindow() {
        if (settingsDataStore != null) {
            ProxySetting currentProxySetting = settingsDataStore.getProxySetting();
            if (currentProxySetting.equals(MANUAL_PROXY)) {
                manualProxySettings.setHost(host.getEditableText().toString().trim());
                String portNumber = port.getEditableText().toString().trim();
                try {
                    manualProxySettings.setPort(
                            portNumber.isEmpty() || !isValidPortNumber(portNumber) ? 80 :
                                    Integer.parseInt(port.getEditableText().toString().trim()));
                } catch (NumberFormatException nfe) {
                    Timber.log(Log.ERROR, nfe, "Unable to get the port number");
                    manualProxySettings.setPort(80);
                }
                manualProxySettings.setUsername(username.getEditableText().toString().trim());
                manualProxySettings.setPassword(password.getEditableText().toString().trim());
                setManualProxySettings(settingsDataStore, manualProxySettings);
            } else if (currentProxySetting.equals(SYSTEM_PROXY)) {
                ProxyConfig systemSettings = ProxyUtil.getProxy(currentProxySetting, null);
                ManualProxy proxySettings = systemSettings.manualProxy();
                if (proxySettings != null) {
                    overrideLibdigidocppProxy(proxySettings);
                    return;
                }
                clearProxySettings(settingsDataStore);
            } else {
                clearProxySettings(settingsDataStore);
            }
        }

        disposables.detach();
        super.onDetachedFromWindow();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
    }

    public static void resetSettings(Context context, SettingsDataStore settingsDataStore) {
        if (settingsDataStore != null) {
            settingsDataStore.setProxySetting(NO_PROXY);
            settingsDataStore.setProxyHost("");
            settingsDataStore.setProxyPort(80);
            settingsDataStore.setProxyUsername("");
            settingsDataStore.setProxyPassword(context, "");
        }
        SignLib.overrideProxy("", 80, "", "");
    }

    public static void overrideLibdigidocppProxy(ManualProxy manualProxy) {
        SignLib.overrideProxy(manualProxy.getHost(), manualProxy.getPort(),
                manualProxy.getUsername(), manualProxy.getPassword());
    }

    private void checkActiveProxySetting(SettingsDataStore settingsDataStore) {
        if (settingsDataStore != null) {
            ProxySetting currentProxySetting = settingsDataStore.getProxySetting();
            switch (currentProxySetting) {
                case NO_PROXY -> {
                    noProxy.setChecked(true);
                    isManualProxyEnabled(false);
                }
                case SYSTEM_PROXY -> {
                    systemProxy.setChecked(true);
                    isManualProxyEnabled(false);
                }
                case MANUAL_PROXY -> {
                    manualProxy.setChecked(true);
                    isManualProxyEnabled(true);
                }
            }
        }
    }

    private void setProxySetting(SettingsDataStore settingsDataStore, int buttonId) {
        if (settingsDataStore != null) {
            if (noProxy.getId() == buttonId) {
                settingsDataStore.setProxySetting(NO_PROXY);
            } else if (systemProxy.getId() == buttonId) {
                settingsDataStore.setProxySetting(SYSTEM_PROXY);
            } else if (manualProxy.getId() == buttonId) {
                settingsDataStore.setProxySetting(MANUAL_PROXY);
            }

            checkActiveProxySetting(settingsDataStore);
        }
    }

    private ManualProxy getManualProxySettings() {
        return new ManualProxy(
                settingsDataStore.getProxyHost(),
                settingsDataStore.getProxyPort(),
                settingsDataStore.getProxyUsername(),
                settingsDataStore.getProxyPassword(navigator.activity())
        );
    }

    private void checkManualProxySettings(SettingsDataStore settingsDataStore, ManualProxy manualProxy) {
        if (settingsDataStore != null) {
            host.setText(manualProxy.getHost());
            port.setText(String.valueOf(manualProxy.getPort()));
            username.setText(manualProxy.getUsername());
            password.setText(manualProxy.getPassword());
        }
    }

    private void setManualProxySettings(SettingsDataStore settingsDataStore, ManualProxy manualProxy) {
        if (settingsDataStore != null) {
            settingsDataStore.setProxyHost(manualProxy.getHost());
            settingsDataStore.setProxyPort(manualProxy.getPort());
            settingsDataStore.setProxyUsername(manualProxy.getUsername());
            settingsDataStore.setProxyPassword(navigator.activity(), manualProxy.getPassword());
            overrideLibdigidocppProxy(manualProxy);
        }
    }

    private void clearProxySettings(SettingsDataStore settingsDataStore) {
        manualProxySettings.setHost("");
        manualProxySettings.setPort(80);
        manualProxySettings.setUsername("");
        manualProxySettings.setPassword("");
        setManualProxySettings(settingsDataStore, manualProxySettings);
    }

    private void isManualProxyEnabled(boolean isEnabled) {
        host.setEnabled(isEnabled);
        port.setEnabled(isEnabled);
        username.setEnabled(isEnabled);
        password.setEnabled(isEnabled);
    }

    private void validatePortNumber(String portNumber) {
        if (!portNumber.isEmpty()) {
            if (!isValidPortNumber(portNumber)) {
                portLayout.setError("Min 1, max 65535");
            } else {
                portLayout.setError(null);
            }
        }
    }

    private static boolean isValidPortNumber(String portNumber) {
        try {
            int number = Integer.parseInt(portNumber);
            return number >= 1 && number <= 65535;
        } catch (NumberFormatException e) {
            Timber.log(Log.ERROR, e, String.format("Invalid number: %s", portNumber));
            return false;
        }
    }
}