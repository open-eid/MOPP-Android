package ee.ria.DigiDoc.common;

import ee.ria.libdigidocpp.ContainerOpenCB;

public class DigidocContainerOpenCB extends ContainerOpenCB {
    private final boolean validate;

    public DigidocContainerOpenCB(boolean validate) {
        super();
        this.validate = validate;
    }

    @Override
    public boolean validateOnline() {
        return validate;
    }
}
