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

package ee.ria.mopp.androidmobileid.soap;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

public class RequestBody {

    @Element(name = "DdsRequestObject")
    private RequestObject object;
    @Attribute(name = "DdsOperationName", required = false)
    private String operationName;

    public RequestBody(RequestObject object) {
        this.object = object;
        this.operationName = object.getOperationName();
    }

    public RequestObject getObject() {
        return object;
    }

    public void setObject(RequestObject object) {
        this.object = object;
        this.operationName = object.getOperationName();
    }

    public String getOperationName() {
        return operationName;
    }

    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }
}
