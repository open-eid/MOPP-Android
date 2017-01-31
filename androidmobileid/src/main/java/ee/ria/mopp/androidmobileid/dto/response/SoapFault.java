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

package ee.ria.mopp.androidmobileid.dto.response;

import com.google.gson.Gson;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;

@Root(strict = false, name = "SOAP-ENV:Fault")
public class SoapFault {

    private static final String BASE_PATH = "Body/Fault/";

    @Element
    @Path(BASE_PATH)
    private String faultcode;
    @Element
    @Path(BASE_PATH)
    private String faultstring;
    @Element
    @Path(BASE_PATH + "detail/")
    private String message;


    public static String toJson(SoapFault fault) {
        return new Gson().toJson(fault);
    }

    public static SoapFault fromJson(String json) {
        return new Gson().fromJson(json, SoapFault.class);
    }

    public String getFaultcode() {
        return faultcode;
    }

    public void setFaultcode(String faultcode) {
        this.faultcode = faultcode;
    }

    public String getFaultstring() {
        return faultstring;
    }

    public void setFaultstring(String faultstring) {
        this.faultstring = faultstring;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
