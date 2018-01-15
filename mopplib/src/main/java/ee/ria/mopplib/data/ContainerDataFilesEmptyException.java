package ee.ria.mopplib.data;

import com.google.common.collect.ImmutableList;

import java.io.File;

/**
 * Container must contain at least one data file.
 *
 * @see SignedContainer#create(File, ImmutableList)
 * @see SignedContainer#removeDataFile(DataFile)
 */
public class ContainerDataFilesEmptyException extends Exception {
}
