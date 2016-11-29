package ee.ria.token.tokenservice.callback;

public interface UnblockPinCallback {

    void success();
    void error(Exception e);

}
