package ee.ria.DigiDoc.android.utils.navigator;

public final class ActivityResultException extends Exception {

    public final ActivityResult activityResult;

    public ActivityResultException(ActivityResult activityResult) {
        this.activityResult = activityResult;
    }
}
