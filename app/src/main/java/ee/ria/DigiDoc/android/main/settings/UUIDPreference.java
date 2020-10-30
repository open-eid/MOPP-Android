/*
 * app
 * Copyright 2020 Riigi Infosüsteemi Amet
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */

package ee.ria.DigiDoc.android.main.settings;

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatCheckBox;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.CheckBox;

import com.takisoft.fix.support.v7.preference.EditTextPreference;

import java.util.Arrays;

import ee.ria.DigiDoc.R;

public class UUIDPreference extends EditTextPreference {

    private final CheckBox checkBox;

    public UUIDPreference(Context context) {
        this(context, null);
    }

    public UUIDPreference(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.editTextPreferenceStyle);
    }

    public UUIDPreference(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public UUIDPreference(Context context, @Nullable AttributeSet attrs, int defStyleAttr,
                            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        getEditText().setHint("00000000-0000-0000-0000-000000000000");
        checkBox = new AppCompatCheckBox(context);
        checkBox.setId(android.R.id.checkbox);
        checkBox.setText(R.string.main_settings_tsa_url_use_default);
        checkBox.setMinHeight((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                48f, context.getResources().getDisplayMetrics()));
        checkBox.setMinWidth((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                120f, context.getResources().getDisplayMetrics()));
    }

    public CheckBox getCheckBox() {
        return checkBox;
    }

    @Override
    public CharSequence getSummary() {
        String text = getText();
        if (TextUtils.isEmpty(text)) {
            return getEditText().getHint();
        }
        char[] password = new char[text.length()];
        Arrays.fill(password, '·');
        return new String(password);
    }
}
