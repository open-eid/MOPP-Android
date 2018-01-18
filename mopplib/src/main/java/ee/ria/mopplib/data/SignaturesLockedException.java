package ee.ria.mopplib.data;

/**
 * Signatures can't be added or removed in the case of Legacy containers.
 *
 * @see SignedContainer#removeSignature(Signature)
 * @see SignedContainer#addAdEsSignature(byte[])
 */
public final class SignaturesLockedException extends Exception {
}
