package ee.ria.DigiDoc.android.main.settings.proxy;

import static com.jakewharton.rxbinding4.view.RxView.clicks;
import static com.jakewharton.rxbinding4.widget.RxRadioGroup.checkedChanges;
import static com.jakewharton.rxbinding4.widget.RxTextView.textChanges;
import static ee.ria.DigiDoc.common.NetworkUtil.constructClientBuilder;
import static ee.ria.DigiDoc.common.ProxySetting.MANUAL_PROXY;
import static ee.ria.DigiDoc.common.ProxySetting.NO_PROXY;
import static ee.ria.DigiDoc.common.ProxySetting.SYSTEM_PROXY;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.ApplicationApp;
import ee.ria.DigiDoc.android.accessibility.AccessibilityUtils;
import ee.ria.DigiDoc.android.main.settings.SettingsDataStore;
import ee.ria.DigiDoc.android.utils.SecureUtil;
import ee.ria.DigiDoc.android.utils.ToastUtil;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.navigator.Navigator;
import ee.ria.DigiDoc.common.ManualProxy;
import ee.ria.DigiDoc.common.ProxyConfig;
import ee.ria.DigiDoc.common.ProxySetting;
import ee.ria.DigiDoc.common.ProxyUtil;
import ee.ria.DigiDoc.common.UserAgentUtil;
import ee.ria.DigiDoc.common.exception.NoInternetConnectionException;
import ee.ria.DigiDoc.sign.SignLib;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
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
    private final Button checkConnection;

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
        checkConnection = findViewById(R.id.mainSettingsProxyCheckInternetConnectionButton);

        portLayout = findViewById(R.id.mainSettingsProxyPortLayout);

        manualProxySettings = getManualProxySettings();

        backButton = findViewById(R.id.mainSettingsProxyBackButton);
        backButton.requestFocus();

        checkActiveProxySetting(settingsDataStore);
        checkManualProxySettings(settingsDataStore, manualProxySettings);

        if (AccessibilityUtils.isTalkBackEnabled()) {
            handleSivaUrlContentDescription();
            AccessibilityUtils.setEditTextCursorToEnd(host);
            AccessibilityUtils.setEditTextCursorToEnd(port);
            AccessibilityUtils.setEditTextCursorToEnd(username);
            AccessibilityUtils.setEditTextCursorToEnd(password);
            AccessibilityUtils.setTextViewContentDescription(context, false, null, context.getString(R.string.main_settings_proxy_host), host);
            AccessibilityUtils.setTextViewContentDescription(context, false, "80", context.getString(R.string.main_settings_proxy_port), port);
            AccessibilityUtils.setTextViewContentDescription(context, false, null, context.getString(R.string.main_settings_proxy_username), username);
            AccessibilityUtils.setTextViewContentDescription(context, false, null, context.getString(R.string.main_settings_proxy_password), password);
        }
    }

    private void handleSivaUrlContentDescription() {
        Optional<Editable> hostEditable = Optional.ofNullable(host.getText());
        Optional<Editable> portEditable = Optional.ofNullable(port.getText());
        Optional<Editable> usernameEditable = Optional.ofNullable(username.getText());
        Optional<Editable> passwordEditable = Optional.ofNullable(password.getText());

        if (Stream.of(hostEditable, portEditable, usernameEditable, passwordEditable)
                .allMatch(Optional::isPresent)) {
            hostEditable.ifPresent(hostText ->
                    AccessibilityUtils.setContentDescription(host, String.format("%s %s",
                    getContext().getString(R.string.main_settings_proxy_host), hostText)));

            portEditable.ifPresent(portText ->
                    AccessibilityUtils.setContentDescription(port, String.format("%s %s",
                    getContext().getString(R.string.main_settings_proxy_port), portText)));

            usernameEditable.ifPresent(usernameText ->
                    AccessibilityUtils.setContentDescription(username, String.format("%s %s",
                    getContext().getString(R.string.main_settings_proxy_username), usernameText)));

            passwordEditable.ifPresent(passwordText ->
                    AccessibilityUtils.setContentDescription(password, String.format("%s %s",
                    getContext().getString(R.string.main_settings_proxy_password), passwordText)));
        }
    }

    public static void addTextWatcherToViews(TextWatcher textWatcher, EditText... editTexts) {
        for (EditText editText : editTexts) {
            editText.addTextChangedListener(textWatcher);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (AccessibilityUtils.isTalkBackEnabled()) {
                    handleSivaUrlContentDescription();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        addTextWatcherToViews(textWatcher, host, port, username, password);
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
        disposables.add(clicks(checkConnection).subscribe(o -> checkConnection()));
    }

    @Override
    public void onDetachedFromWindow() {
        saveProxySettings(true);

        disposables.detach();
        super.onDetachedFromWindow();
    }

    private void saveProxySettings(boolean clearSettings) {
        if (settingsDataStore != null) {
            ProxySetting currentProxySetting = settingsDataStore.getProxySetting();
            if (currentProxySetting.equals(MANUAL_PROXY)) {
                manualProxySettings.setHost(host.getEditableText().toString().trim());
                String portNumber = port.getEditableText().toString().trim();
                try {
                    manualProxySettings.setPort(
                            portNumber.isEmpty() || isNotValidPortNumber(portNumber) ? 80 :
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
                if (clearSettings) {
                    clearProxySettings(settingsDataStore);
                }
            } else {
                if (clearSettings) {
                    clearProxySettings(settingsDataStore);
                }
            }
        }
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
            if (isNotValidPortNumber(portNumber)) {
                portLayout.setError("Min 1, max 65535");
            } else {
                portLayout.setError(null);
            }
        }
    }

    private static boolean isNotValidPortNumber(String portNumber) {
        try {
            int number = Integer.parseInt(portNumber);
            return number < 1 || number > 65535;
        } catch (NumberFormatException e) {
            Timber.log(Log.ERROR, e, String.format("Invalid number: %s", portNumber));
            return true;
        }
    }

    private void checkConnection() {
        Timber.log(Log.DEBUG, "Checking connection");

        saveProxySettings(false);

        CompletableFuture<String> result = new CompletableFuture<>();

        Request request = new Request.Builder()
                .url("https://id.eesti.ee/config.json")
                .addHeader("Content-Type", "application/json")
                .addHeader("User-Agent", UserAgentUtil.getUserAgent(navigator.activity(), false))
                .build();

        OkHttpClient httpClient;
        try {
            httpClient = constructClientBuilder(navigator.activity()).build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to construct HTTP client", e);
        }

        CompletableFuture.runAsync(() -> {
            Call call = httpClient.newCall(request);
            try {
                Response response = call.execute();
                if (response.code() == 403) {
                    Timber.log(Log.DEBUG, "Forbidden error with proxy configuration");
                    navigator.activity().runOnUiThread(() -> ToastUtil.showError(navigator.activity(), R.string.main_settings_proxy_check_username_and_password));
                    result.completeExceptionally(new NoInternetConnectionException());
                    return;
                }

                if (response.code() != 200) {
                    Timber.log(Log.DEBUG, "No Internet connection detected");
                    navigator.activity().runOnUiThread(() -> ToastUtil.showError(navigator.activity(), R.string.main_settings_proxy_check_connection_unsuccessful));
                    result.completeExceptionally(new NoInternetConnectionException());
                } else {
                    Timber.log(Log.DEBUG, "Internet connection detected successfully");
                    navigator.activity().runOnUiThread(() -> ToastUtil.showError(navigator.activity(), R.string.main_settings_proxy_check_connection_success));
                    result.complete("Internet connection detected successfully");
                }
            } catch (IOException e) {
                if (e.getMessage() != null && (e.getMessage().contains("CONNECT: 403") ||
                        e.getMessage().contains("Failed to authenticate with proxy"))) {
                    Timber.log(Log.DEBUG, "Received HTTP status 403 or failed to authenticate. Unable to connect with proxy configuration");
                    navigator.activity().runOnUiThread(() -> ToastUtil.showError(navigator.activity(), R.string.main_settings_proxy_check_connection_unsuccessful));
                    result.completeExceptionally(new NoInternetConnectionException());
                    return;
                }
                Timber.log(Log.DEBUG, e, "Unable to check Internet connection");
                navigator.activity().runOnUiThread(() -> ToastUtil.showError(navigator.activity(), R.string.main_settings_proxy_check_connection_unsuccessful));
                result.completeExceptionally(new NoInternetConnectionException());
            }
        });
    }
}