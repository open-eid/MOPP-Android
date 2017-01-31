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



import java.io.IOException;
import java.lang.annotation.Annotation;

import ee.ria.mopp.androidmobileid.dto.response.SoapFault;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Response;

public class ErrorUtils {

    public static SoapFault parseError(Response<?> response) {
        Converter<ResponseBody, SoapFault> converter =
                ServiceGenerator.retrofit()
                        .responseBodyConverter(SoapFault.class, new Annotation[0]);

        SoapFault error;

        try {
            error = converter.convert(response.errorBody());
        } catch (IOException e) {
            return new SoapFault();
        }

        return error;
    }
}
