/*
 * smart-id-lib
 * Copyright 2017 - 2024 Riigi Infosüsteemi Amet
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

package ee.ria.DigiDoc.smartid.service;

public interface SmartSignConstants {

    String SID_BROADCAST_ACTION = "ee.ria.mopp.android.smartid.SID_BROADCAST_ACTION";
    String SID_BROADCAST_TYPE_KEY = "ee.ria.mopp.android.smartid.SID_BROADCAST_TYPE_KEY";

    String SIGNING_ROLE_DATA = "ee.ria.mopp.android.smartid.SIGNING_ROLE_DATA";

    String CREATE_SIGNATURE_REQUEST = "ee.ria.mopp.android.smartid.CREATE_SIGNATURE_REQUEST";
    String CERTIFICATE_CERT_BUNDLE = "ee.ria.mopp.android.smartid.CERTIFICATE_CERT_BUNDLE";
    String CREATE_SIGNATURE_DEVICE = "ee.ria.mopp.android.smartid.SID_DEVICE";
    String CREATE_SIGNATURE_CHALLENGE = "ee.ria.mopp.android.smartid.SID_CHALLENGE";
    String CREATE_SIGNATURE_STATUS = "ee.ria.mopp.android.smartid.CREATE_SIGNATURE_STATUS";
    String SERVICE_FAULT = "ee.ria.mopp.android.smartid.SERVICE_FAULT";
    String PROXY_SETTING = "ee.ria.mopp.smartid.PROXY_SETTING";
    String MANUAL_PROXY_HOST = "ee.ria.mopp.smartid.HOST";
    String MANUAL_PROXY_PORT = "ee.ria.mopp.smartid.PORT";
    String MANUAL_PROXY_USERNAME = "ee.ria.mopp.smartid.USERNAME";
    String MANUAL_PROXY_PASSWORD = "ee.ria.mopp.smartid.PASSWORD";

    String NOTIFICATION_CHANNEL = "SMART_ID_CHANNEL";
    int NOTIFICATION_PERMISSION_CODE = 1;
}
