// PinActionsListener.aidl
package ee.ria.aidl.token.tokenaidllibrary;

interface PinActionsListener {
    void onPinActionSuccessful();
    void onPinActionFailed(String reason);
}
