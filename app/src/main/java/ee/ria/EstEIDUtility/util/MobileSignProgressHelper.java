/*
 * Copyright 2017 Riigi Infosüsteemide Amet
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

package ee.ria.EstEIDUtility.util;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import ee.ria.EstEIDUtility.R;
import ee.ria.mopp.androidmobileid.dto.response.GetMobileCreateSignatureStatusResponse;

public class MobileSignProgressHelper {

    private LinearLayout progressLayout;
    private TextView signingProgressText;
    private TextView controlCodeText;
    private TextView mobileSignInfoText;
    private ProgressBar mobileSignProgressBar;
    private MobileSignStatusMessageSource messageSource;

    public MobileSignProgressHelper(View layout) {
        messageSource = new MobileSignStatusMessageSource(layout.getResources());
        progressLayout = (LinearLayout) layout.findViewById(R.id.mobile_sign_progress);
        signingProgressText = (TextView) layout.findViewById(R.id.signing_progress_text);
        controlCodeText = (TextView) layout.findViewById(R.id.mobile_sign_control_code);
        mobileSignInfoText = (TextView) layout.findViewById(R.id.mobile_sign_info);
        mobileSignProgressBar = (ProgressBar) layout.findViewById(R.id.mobile_sign_progress_bar);
    }

    public void showMobileSignProgress(CharSequence controlCode) {
        controlCodeText.setText(controlCode);
        signingProgressText.setText(messageSource.getInitialStatusMessage());
        showAll();
    }

    private void showAll() {
        progressLayout.setVisibility(View.VISIBLE);
    }

    public void updateStatus(GetMobileCreateSignatureStatusResponse.ProcessStatus processStatus) {
        signingProgressText.setText(messageSource.getMessage(processStatus));
    }

    public void close() {
        progressLayout.setVisibility(View.GONE);
    }

    public CharSequence getMessage(GetMobileCreateSignatureStatusResponse.ProcessStatus status) {
        return messageSource.getMessage(status);
    }
}
