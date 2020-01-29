package ee.ria.DigiDoc.android.main.settings;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.annotation.Nullable;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.PreferenceManager;
import android.util.AttributeSet;

import ee.ria.DigiDoc.R;

public class OpenAllFiletypesPreference extends SwitchPreference {

    public OpenAllFiletypesPreference(Context context) {
        this(context, null);
    }

    public OpenAllFiletypesPreference(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.switchPreferenceStyle);
    }

    public OpenAllFiletypesPreference(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public OpenAllFiletypesPreference(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public boolean callChangeListener(Object newValue) {
        super.callChangeListener(newValue);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        sharedPreferences.edit().putBoolean(getContext().getString(R.string.main_settings_open_all_filetypes_key), (boolean) newValue).apply();

        PackageManager packageManager = getContext().getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(getContext().getPackageName());
        ComponentName componentName = intent.getComponent();
        Intent restartIntent = Intent.makeRestartActivityTask(componentName);
        restartIntent.setAction(Intent.ACTION_CONFIGURATION_CHANGED);
        getContext().startActivity(restartIntent);
        return (boolean) newValue;
    }
}
