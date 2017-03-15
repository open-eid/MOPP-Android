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

package ee.ria.DigiDoc.util;

import org.apache.commons.io.FilenameUtils;

import ee.ria.DigiDoc.preferences.accessor.AppPreferences;

public class ContainerNameUtils {

    private ContainerNameUtils() {}

    public static boolean hasSupportedContainerExtension(String containerName) {
        return hasBdocExtension(containerName) || hasAsiceExtension(containerName);
    }

    public static boolean hasBdocExtension(String containerName) {
        return hasExtension(containerName, AppPreferences.BDOC_CONTAINER_TYPE);
    }

    public static boolean hasAsiceExtension(String containerName) {
        return hasExtension(containerName, AppPreferences.ASICE_CONTAINER_TYPE);
    }

    public static boolean hasExtension(String containerName, String extension) {
        return FilenameUtils.getExtension(containerName).equals(extension);
    }
}
