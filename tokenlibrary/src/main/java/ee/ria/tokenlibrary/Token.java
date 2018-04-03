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

package ee.ria.tokenlibrary;

import android.util.SparseArray;

public interface Token {

	byte[] sign(PinType type, String pin, byte[] data, boolean ellipticCurveCertificate);
	SparseArray<String> readPersonalFile();
	boolean changePin(PinType pinType, byte[] currentPin, byte[] newPin);
	byte[] readCert(CertType type);
    byte readRetryCounter(PinType pinType);
    int readUseCounter(CertType certType);

    boolean unblockAndChangePin(PinType pinType, byte[] puk, byte[] newPin);

    enum CertType {
        CertAuth((byte) 0xAA),
        CertSign((byte) 0xDD);
        public byte value;

        CertType(byte value) {
            this.value = value;
        }
    }

    enum PinType {
        PIN1((byte) 0x01, (byte) 0x01),
        PIN2((byte) 0x02, (byte) 0x02),
        PUK((byte) 0x00, (byte) 0x03);
        public byte value;
        public byte retryValue;

        PinType(byte value, byte retryValue) {
            this.value = value;
            this.retryValue = retryValue;
        }
    }

}
