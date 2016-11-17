// PersonalFileResultListener.aidl
package ee.ria.aidl.token.tokenaidllibrary;

interface PersonalFileResultListener {
	void onPersonalFileResponse(String personalFile);
	void onPersonalFileError(String reason);
}
