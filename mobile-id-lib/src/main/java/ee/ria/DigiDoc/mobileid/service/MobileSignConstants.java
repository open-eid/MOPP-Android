/*
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

package ee.ria.DigiDoc.mobileid.service;

public interface MobileSignConstants {

    String MID_BROADCAST_ACTION = "ee.ria.mopp.androidmobileid.MID_BROADCAST_ACTION";
    String MID_BROADCAST_TYPE_KEY = "ee.ria.mopp.androidmobileid.MID_BROADCAST_TYPE_KEY";

    String SIGNING_ROLE_DATA = "ee.ria.mopp.androidmobileid.SIGNING_ROLE_DATA";

    String CREATE_SIGNATURE_REQUEST = "ee.ria.mopp.androidmobileid.CREATE_SIGNATURE_REQUEST";
    String ACCESS_TOKEN_PASS = "ee.ria.mopp.androidmobileid.ACCESS_TOKEN_PASS";
    String ACCESS_TOKEN_PATH = "ee.ria.mopp.androidmobileid.ACCESS_TOKEN_PATH";
    String CERTIFICATE_CERT_BUNDLE = "ee.ria.mopp.androidmobileid.CERTIFICATE_CERT_BUNDLE";
    String CREATE_SIGNATURE_CHALLENGE = "ee.ria.mopp.androidmobileid.MID_CHALLENGE";
    String CREATE_SIGNATURE_STATUS = "ee.ria.mopp.androidmobileid.CREATE_SIGNATURE_STATUS";
    String SERVICE_FAULT = "ee.ria.mopp.androidmobileid.SERVICE_FAULT";
    String CONFIG_URL = "ee.ria.mopp.androidmobileid.CONFIG_URL";
    String PROXY_SETTING = "ee.ria.mopp.androidmobileid.PROXY_SETTING";
    String MANUAL_PROXY_HOST = "ee.ria.mopp.androidmobileid.HOST";
    String MANUAL_PROXY_PORT = "ee.ria.mopp.androidmobileid.PORT";
    String MANUAL_PROXY_USERNAME = "ee.ria.mopp.androidmobileid.USERNAME";
    String MANUAL_PROXY_PASSWORD = "ee.ria.mopp.androidmobileid.PASSWORD";
}
