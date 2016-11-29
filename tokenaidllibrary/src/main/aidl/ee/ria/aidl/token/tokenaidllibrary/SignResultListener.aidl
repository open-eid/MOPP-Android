// SignResultListener.aidl
package ee.ria.aidl.token.tokenaidllibrary;

interface SignResultListener {
    oneway void onSignSuccess(String signatureInHex);
    oneway void onSignFailed(String reason);
}
