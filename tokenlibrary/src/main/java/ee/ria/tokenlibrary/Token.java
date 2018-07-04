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

import ee.ria.scardcomlibrary.SmartCardReaderException;

public interface Token {

    /**
     * Read personal information of the cardholder.
     *
     * @return Personal data of the cardholder.
     * @throws SmartCardReaderException When reading failed.
     */
    PersonalData personalData() throws SmartCardReaderException;

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

    byte[] sign(CodeType type, String pin, byte[] data, boolean ellipticCurveCertificate)
            throws SmartCardReaderException;

    boolean changePin(CodeType pinType, byte[] currentPin, byte[] newPin)
            throws SmartCardReaderException;

    boolean unblockAndChangePin(CodeType pinType, byte[] puk, byte[] newPin)
            throws SmartCardReaderException;

    byte[] decrypt(byte[] pin1, byte[] data) throws SmartCardReaderException;
}
