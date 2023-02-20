/*
 * Copyright 2017 - 2023 Riigi Infosüsteemi Amet
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

package ee.ria.DigiDoc.idcard;

import ee.ria.DigiDoc.smartcardreader.SmartCardReader;
import ee.ria.DigiDoc.smartcardreader.SmartCardReaderException;

class EstEIDv3d4 extends EstEIDToken {

    private final SmartCardReader reader;

    EstEIDv3d4(SmartCardReader reader) {
        super(reader);
        this.reader = reader;
    }

    @Override
    void selectMasterFile() throws SmartCardReaderException {
        reader.transmit(0x00, 0xA4, 0x00, 0x0C, null, null);
    }

    @Override
    void selectCatalogue() throws SmartCardReaderException {
        reader.transmit(0x00, 0xA4, 0x01, 0x0C, new byte[] {(byte) 0xEE, (byte) 0xEE}, null);
    }

    @Override
    void selectSecurityEnvironment(byte operation) throws SmartCardReaderException {
        reader.transmit(0x00, 0x22, 0xF3, operation, null, null);
    }
}
