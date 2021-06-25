/*
 * Copyright 2021 Riigi Infosüsteemi Amet
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

    String CREATE_SIGNATURE_REQUEST = "ee.ria.mopp.androidmobileid.CREATE_SIGNATURE_REQUEST";
    String ACCESS_TOKEN_PASS = "ee.ria.mopp.androidmobileid.ACCESS_TOKEN_PASS";
    String ACCESS_TOKEN_PATH = "ee.ria.mopp.androidmobileid.ACCESS_TOKEN_PATH";
    String CERTIFICATE_CERT_BUNDLE = "ee.ria.mopp.androidmobileid.CERTIFICATE_CERT_BUNDLE";
    String CREATE_SIGNATURE_CHALLENGE = "ee.ria.mopp.androidmobileid.MID_CHALLENGE";
    String CREATE_SIGNATURE_STATUS = "ee.ria.mopp.androidmobileid.CREATE_SIGNATURE_STATUS";
    String SERVICE_FAULT = "ee.ria.mopp.androidmobileid.SERVICE_FAULT";
}
