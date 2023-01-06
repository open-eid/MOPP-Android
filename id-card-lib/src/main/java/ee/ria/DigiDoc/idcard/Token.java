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

import org.bouncycastle.util.encoders.Hex;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import ee.ria.DigiDoc.smartcardreader.SmartCardReader;
import ee.ria.DigiDoc.smartcardreader.SmartCardReaderException;

/**
 * EstEID token interface.
 */
public interface Token {

    /**
     * Read personal information of the cardholder.
     *
     * @return Personal data of the cardholder.
     * @throws SmartCardReaderException When reading failed.
     */
    PersonalData personalData() throws SmartCardReaderException;

    /**
     * Change PIN1/PIN2/PUK code.
     *
     * @param type Code type.
     * @param currentCode Current code.
     * @param newCode New code.
     * @throws SmartCardReaderException When changing failed.
     * @throws CodeVerificationException When current code is wrong.
     */
    void changeCode(CodeType type, byte[] currentCode, byte[] newCode)
            throws SmartCardReaderException;

    /**
     * Unblock PIN1/PIN2 via PUK code and change it to a new value.
     *
     * When PIN1/PIN2 is not blocked yet it will be blocked before unblocking.
     *
     * @param pukCode PUK code.
     * @param type Code type.
     * @param newCode New code.
     * @throws SmartCardReaderException When changing failed.
     * @throws CodeVerificationException When PUK code is wrong.
     */
    void unblockAndChangeCode(byte[] pukCode, CodeType type, byte[] newCode)
            throws SmartCardReaderException;

    /**
     * Read retry counter for PIN1/PIN2/PUK code.
     *
     * @param type Code type.
     * @return Code retry counter.
     */
    int codeRetryCounter(CodeType type) throws SmartCardReaderException;

    /**
     * Read certificate data of the cardholder.
     *
     * @param type Type of the certificate.
     * @return Certificate data.
     * @throws SmartCardReaderException When reading failed.
     */
    byte[] certificate(CertificateType type) throws SmartCardReaderException;

    /**
     * Calculate electronic signature with pre-calculated hash.
     *
     * @param pin2 PIN2 code.
     * @param hash Pre-calculated hash.
     * @param ecc Whether it's a elliptic curve certificate.
     * @return Signed data.
     * @throws SmartCardReaderException When calculating signature failed.
     * @throws CodeVerificationException When PIN2 code is wrong.
     */
    byte[] calculateSignature(byte[] pin2, byte[] hash, boolean ecc)
            throws SmartCardReaderException;

    /**
     * Decrypt data.
     *
     * @param pin1 PIN1 code.
     * @param data Data to decrypt.
     * @param ecc Whether it's a elliptic curve certificate.
     *
     * @return Decrypt result.
     * @throws SmartCardReaderException When decrypting failed.
     * @throws CodeVerificationException When PIN1 code is wrong.
     */
    byte[] decrypt(byte[] pin1, byte[] data, boolean ecc) throws SmartCardReaderException;

    /**
     * Create an instance of Token based on the current card in the reader.
     *
     * @param reader Smart card reader instance, must be connected.
     * @return Token instance.
     * @throws SmartCardReaderException When card is not supported or reader is not connected.
     */
    static Token create(SmartCardReader reader) throws SmartCardReaderException {
        byte[] atr = reader.atr();
        if (atr == null) {
            throw new SmartCardReaderException("ATR cannot be null");
        }
        if (Arrays.equals(Hex.decode("3bdb960080b1fe451f830012233f536549440f9000f1"), atr)) {
            return new ID1(reader);
        } else if (Arrays.equals(Hex.decode("3bfa1800008031fe45fe654944202f20504b4903"), atr) ||
                Arrays.equals(Hex.decode("3bfe1800008031fe45803180664090a4162a00830f9000ef"), atr)
        ) {
            return new EstEIDv3d5(reader);
        } else if (
                Arrays.equals(Hex.decode("3bfe1800008031fe454573744549442076657220312e30a8"), atr)
                // TODO check for 3.0 card
        ) {
            return new EstEIDv3d4(reader);
        }

        throw new SmartCardReaderException("Unsupported card ATR: " + new String(Hex.encode(atr), StandardCharsets.UTF_8));
    }
}
