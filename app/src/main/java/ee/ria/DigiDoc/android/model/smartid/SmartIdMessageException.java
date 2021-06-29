/*
 * app
 * Copyright 2017 - 2021 Riigi Infosüsteemi Amet
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

package ee.ria.DigiDoc.android.model.smartid;

import android.content.Context;

import ee.ria.DigiDoc.smartid.dto.response.SessionStatusResponse.ProcessStatus;

/**
 * Exception thrown by Smart-ID service that contains message suitable for showing to the user.
 */
public final class SmartIdMessageException extends Exception {

    public static SmartIdMessageException create(Context context, ProcessStatus status) {
        return new SmartIdMessageException(SmartIdStatusMessages.message(context, status));
    }

    private SmartIdMessageException(String message) {
        super(message);
    }
}
