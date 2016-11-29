package ee.ria.token.tokenservice.callback;

public interface ChangePinCallback {

    void success();
    void error(Exception e);

}
