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

package ee.ria.DigiDoc.mobileid.dto.request;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root(name = "DataFileDigest")
public class DataFileDto {

    @Element(name = "Id")
    private String id;
    @Element(name = "DigestType")
    private String digestType;
    @Element(name = "DigestValue")
    private String digestValue;
    @Element(name = "MimeType")
    private String mimeType;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDigestType() {
        return digestType;
    }

    public void setDigestType(String digestType) {
        this.digestType = digestType;
    }

    public String getDigestValue() {
        return digestValue;
    }

    public void setDigestValue(String digestValue) {
        this.digestValue = digestValue;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DataFileDto{");
        sb.append("id='").append(id).append('\'');
        sb.append(", digestType='").append(digestType).append('\'');
        sb.append(", digestValue='").append(digestValue).append('\'');
        sb.append(", mimeType='").append(mimeType).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
