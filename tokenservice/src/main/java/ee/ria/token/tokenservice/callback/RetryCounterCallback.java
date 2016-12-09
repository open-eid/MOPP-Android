package ee.ria.token.tokenservice.callback;

public interface RetryCounterCallback {

    void onCounterRead(byte counterByte);

    void cardNotProvided();
}
