/*
 * app
 * Copyright 2017 - 2022 Riigi Infosüsteemi Amet
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

package ee.ria.DigiDoc.android.signature.update.smartid;

import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;

import ee.ria.DigiDoc.android.signature.update.SignatureAddResponse;
import ee.ria.DigiDoc.smartid.dto.response.SessionStatusResponse;
import ee.ria.DigiDoc.sign.SignedContainer;

@AutoValue
public abstract class SmartIdResponse implements SignatureAddResponse {

    @Nullable public abstract SessionStatusResponse.ProcessStatus status();
    @Nullable public abstract String challenge();
    public abstract boolean selectDevice();

    @Override
    public boolean showDialog() {
        return false;
    }

    @Override
    public SignatureAddResponse mergeWith(@Nullable SignatureAddResponse previous) {
        if (previous == null || !(previous instanceof SmartIdResponse)) {
            return this;
        }
        SmartIdResponse previousResponse = (SmartIdResponse) previous;
        SessionStatusResponse.ProcessStatus status =
                status() == null ? previousResponse.status() : status();
        String challenge = challenge() == null ? previousResponse.challenge() : challenge();
        boolean selectDevice = selectDevice() ? selectDevice() : previousResponse.selectDevice();
        return create(container(), active(), status, challenge, selectDevice);
    }

    public static SmartIdResponse status(
            SessionStatusResponse.ProcessStatus status) {
        return create(null, true, status, null, false);
    }

    static SmartIdResponse challenge(String challenge) {
        return create(null, true, null, challenge, false);
    }

    static SmartIdResponse selectDevice(boolean selectDevice) {
        return create(null, true, null, null, selectDevice);
    }

    public static SmartIdResponse success(SignedContainer container) {
        return create(container, false, null, null, false);
    }

    private static AutoValue_SmartIdResponse create(
            @Nullable SignedContainer container, boolean active,
            @Nullable SessionStatusResponse.ProcessStatus status,
            @Nullable String challenge,
            boolean selectDevice) {
        return new AutoValue_SmartIdResponse(container, active, status, challenge, selectDevice);
    }
}
