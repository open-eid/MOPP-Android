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

package ee.ria.DigiDoc.mobileid.dto.response;

import com.google.gson.Gson;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;

@Root(strict = false, name = "dig:MobileCreateSignatureResponse")
public class MobileCreateSignatureResponse {

    private static final String BASE_PATH = "Body/MobileCreateSignatureResponse/";

    @Element(name = "Sesscode")
    @Path(BASE_PATH)
    private int sesscode;
    @Element(name = "ChallengeID")
    @Path(BASE_PATH)
    private String challengeID;
    @Element(name = "Status")
    @Path(BASE_PATH)
    private String status;

    public static String toJson(MobileCreateSignatureResponse challenge) {
        return new Gson().toJson(challenge);
    }

    public static MobileCreateSignatureResponse fromJson(String json) {
        return new Gson().fromJson(json, MobileCreateSignatureResponse.class);
    }

    public int getSesscode() {
        return sesscode;
    }

    public void setSesscode(int sesscode) {
        this.sesscode = sesscode;
    }

    public String getChallengeID() {
        return challengeID;
    }

    public void setChallengeID(String challengeID) {
        this.challengeID = challengeID;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MobileCreateSignatureResponse {");
        sb.append("sesscode='").append(sesscode).append('\'');
        sb.append(", challengeID='").append(challengeID).append('\'');
        sb.append(", status='").append(status).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
