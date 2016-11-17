// TokenAidlInterface.aidl
package ee.ria.aidl.token.tokenaidllibrary;
import ee.ria.aidl.token.tokenaidllibrary.PinActionsListener;
import ee.ria.aidl.token.tokenaidllibrary.SignResultListener;
import ee.ria.aidl.token.tokenaidllibrary.PersonalFileResultListener;
import ee.ria.aidl.token.tokenaidllibrary.CertificateResultListener;

interface TokenAidlInterface {
    oneway void changePin1(PinActionsListener listener, String oldPin, String newPin);
    oneway void changePin2(PinActionsListener listener, String oldPin, String newPin);
    oneway void changePuk(PinActionsListener listener, String oldPuk, String newPuk);
    oneway void unblockPin1(PinActionsListener listener, String puk);
    oneway void unblockPin2(PinActionsListener listener, String puk);
    oneway void sign(SignResultListener listener, String pin2, String hashToSignInHex);
    oneway void readPersonalFile(PersonalFileResultListener listener);
    oneway void readSignCertificateInHex(CertificateResultListener listener);
    oneway void readAuthCertificateInHex(CertificateResultListener listener);
}
