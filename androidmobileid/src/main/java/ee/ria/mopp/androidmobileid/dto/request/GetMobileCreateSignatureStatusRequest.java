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

package ee.ria.mopp.androidmobileid.dto.request;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root(name = "dig:GetMobileCreateSignatureStatus")
public class GetMobileCreateSignatureStatusRequest implements RequestObject {

    @Element(name = "Sesscode")
    private int sessCode;
    @Element(name = "WaitSignature")
    private boolean waitSignature;

    public GetMobileCreateSignatureStatusRequest(int sessCode) {
        this.sessCode = sessCode;
        this.waitSignature = false;
    }

    public int getSessCode() {
        return sessCode;
    }

    public void setSessCode(int sessCode) {
        this.sessCode = sessCode;
    }

    public boolean isWaitSignature() {
        return waitSignature;
    }

    public void setWaitSignature(boolean waitSignature) {
        this.waitSignature = waitSignature;
    }

    @Override
    public String getOperationName() {
        return "dig:GetMobileCreateSignatureStatus";
    }
}
