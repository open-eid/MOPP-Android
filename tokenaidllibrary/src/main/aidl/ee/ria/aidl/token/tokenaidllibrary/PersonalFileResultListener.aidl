// PersonalFileResultListener.aidl
package ee.ria.aidl.token.tokenaidllibrary;

interface PersonalFileResultListener {
	oneway void onPersonalFileResponse(String personalFile);
	oneway void onPersonalFileError(String reason);
}
