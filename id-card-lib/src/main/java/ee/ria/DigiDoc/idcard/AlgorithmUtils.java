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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

final class AlgorithmUtils {

    private static final int BINARY_SHA1_LENGTH = 20;
    private static final int BINARY_SHA224_LENGTH = 28;
    private static final int BINARY_SHA256_LENGTH = 32;
    private static final int BINARY_SHA384_LENGTH = 48;
    private static final int BINARY_SHA512_LENGTH = 64;

    private enum ALGORITHM {
        SHA_1(new byte[]{
                0x30, 0x21, 0x30, 0x09, 0x06, 0x05, 0x2B, 0x0E, 0x03, 0x02, 0x1A, 0x05, 0x00, 0x04,
                0x14
        }),
        SHA_224(new byte[]{
                0x30, 0x2D, 0x30, 0x0D, 0x06, 0x09, 0x60, (byte) 0x86, 0x48, 0x01, 0x65, 0x03, 0x04,
                0x02, 0x04, 0x05, 0x00, 0x04, 0x1C
        }),
        SHA_256(new byte[]{
                0x30, 0x31, 0x30, 0x0D, 0x06, 0x09, 0x60, (byte) 0x86, 0x48, 0x01, 0x65, 0x03, 0x04,
                0x02, 0x01, 0x05, 0x00, 0x04, 0x20
        }),
        SHA_384(new byte[]{
                0x30, 0x41, 0x30, 0x0D, 0x06, 0x09, 0x60, (byte) 0x86, 0x48, 0x01, 0x65, 0x03, 0x04,
                0x02, 0x02, 0x05, 0x00, 0x04, 0x30
        }),
        SHA_512(new byte[]{
                0x30, 0x51, 0x30, 0x0D, 0x06, 0x09, 0x60, (byte) 0x86, 0x48, 0x01, 0x65, 0x03, 0x04,
                0x02, 0x03, 0x05, 0x00, 0x04, 0x40
        });

        private byte[] padding;

        ALGORITHM(byte[] padding) {
            this.padding = padding;
        }
    }

    static byte[] addPadding(byte[] hash, boolean ellipticCurveCertificate) throws IdCardException {
        if (ellipticCurveCertificate) {
            return hash;
        }
        try (ByteArrayOutputStream toSign = new ByteArrayOutputStream()) {
            toSign.write(getAlgorithm(hash.length).padding);
            toSign.write(hash);
            return toSign.toByteArray();
        } catch (IOException e) {
            throw new IdCardException("Failed to Add padding to hash", e);
        }
    }

    private static ALGORITHM getAlgorithm(int hashLength) throws IdCardException {
        switch (hashLength) {
            case BINARY_SHA1_LENGTH:
                return ALGORITHM.SHA_1;
            case BINARY_SHA224_LENGTH:
                return ALGORITHM.SHA_224;
            case BINARY_SHA256_LENGTH:
                return ALGORITHM.SHA_256;
            case BINARY_SHA384_LENGTH:
                return ALGORITHM.SHA_384;
            case BINARY_SHA512_LENGTH:
                return ALGORITHM.SHA_512;
            default:
                throw new IdCardException("Unsupported Algorithm");
        }
    }
}
