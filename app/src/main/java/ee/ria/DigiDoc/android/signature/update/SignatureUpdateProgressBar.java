package ee.ria.DigiDoc.android.signature.update;

import android.os.CountDownTimer;
import android.widget.ProgressBar;

public class SignatureUpdateProgressBar {

    private static final long PROGRESS_BAR_TIMEOUT_CANCEL = 125 * 1000;
    private static CountDownTimer timeoutTimer;

    public static void startProgressBar(ProgressBar progressBar) {
        timeoutTimer = new CountDownTimer(PROGRESS_BAR_TIMEOUT_CANCEL, 1000) {

            public void onTick(long millisUntilFinished) {
                progressBar.setMax((int) (PROGRESS_BAR_TIMEOUT_CANCEL / 1000));
                progressBar.incrementProgressBy(1);
            }

            public void onFinish() {
                stopProgressBar(progressBar, true);
            }

        }.start();
    }

    public static void stopProgressBar(ProgressBar progressBar, boolean isTimerStarted) {
        if (isTimerStarted && timeoutTimer != null) {
            progressBar.setProgress(0);
            timeoutTimer.cancel();
        }
    }
}
