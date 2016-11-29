// PinActionsListener.aidl
package ee.ria.aidl.token.tokenaidllibrary;

interface PinActionsListener {
    oneway void onPinActionSuccessful();
    oneway void onPinActionFailed(String reason);
}
