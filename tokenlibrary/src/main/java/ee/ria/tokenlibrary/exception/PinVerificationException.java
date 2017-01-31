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

package ee.ria.tokenlibrary.exception;

import ee.ria.tokenlibrary.Token;

public class PinVerificationException extends TokenException {

    private Token.PinType pinType;

    public PinVerificationException(Token.PinType pinType) {
        super();
        this.pinType = pinType;
    }

    public Token.PinType getPinType() {
        return pinType;
    }

    @Override
    public String getMessage() {
        return createExceptionMessage();
    }

    private String createExceptionMessage() {
        switch (pinType) {
            case PIN1:
                return "PIN1 login failed";
            case PIN2:
                return "PIN2 login failed";
            case PUK:
                return "PUK login failed";
        }
        return "Verification failed";
    }
}
