package androidx.core.app;

/**
 * Workaround for JobIntentService crash bug<br/>
 *
 * PS! In order to keep the issue at bay never extend {@link androidx.core.app.JobIntentService} directly but rather given class extension instead!
 *
 * @see <a href="https://github.com/evernote/android-job/issues/255">https://github.com/evernote/android-job/issues/255</a>
 * @see <a href="https://medium.com/@eng.zak/workaround-to-solve-securityexception-caused-by-jobintentservice-1f4b0e688a26">https://medium.com/@eng.zak/workaround-to-solve-securityexception-caused-by-jobintentservice-1f4b0e688a26</a>
  */
public abstract class PatchedJobIntentService extends JobIntentService {

    // This method is the main reason for the bug and crash
    @Override
    GenericWorkItem dequeueWork() {
        try {
            return super.dequeueWork();
        } catch (SecurityException exception) {
            // the exception will be ignored here.
        }
        return null;
    }

}
