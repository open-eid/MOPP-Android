// SignResultListener.aidl
package ee.ria.aidl.token.tokenaidllibrary;

interface SignResultListener {
    void onSignSuccess(String signatureInHex);
    void onSignFailed(String reason);
}
