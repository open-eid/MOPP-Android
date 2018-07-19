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

package ee.ria.DigiDoc.mobileid.soap;

import org.simpleframework.xml.strategy.Type;
import org.simpleframework.xml.strategy.Visitor;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.NodeMap;
import org.simpleframework.xml.stream.OutputNode;

public class RequestObjectInterceptor implements Visitor {

    @Override
    public void read(Type type, NodeMap<InputNode> node) throws Exception {
        //No need to read request objects
    }

    @Override
    public void write(Type type, NodeMap<OutputNode> node) throws Exception {
        OutputNode outputNode = node.getNode();
        if ("DdsRequestObject".equals(outputNode.getName())) {
            OutputNode operationNameAtribute = outputNode.getParent().getAttributes().get("DdsOperationName");
            outputNode.setName(operationNameAtribute.getValue());
        }
    }
}
