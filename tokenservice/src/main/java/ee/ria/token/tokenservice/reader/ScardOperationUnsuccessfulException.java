package ee.ria.token.tokenservice.reader;


class ScardOperationUnsuccessfulException extends SmartCardCommunicationException {
    ScardOperationUnsuccessfulException(String message) {
        super(message);
    }
}
