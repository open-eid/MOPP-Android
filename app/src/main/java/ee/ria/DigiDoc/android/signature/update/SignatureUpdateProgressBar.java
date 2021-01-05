package ee.ria.DigiDoc.android.signature.update;

import android.os.CountDownTimer;
import android.widget.ProgressBar;

class SignatureUpdateProgressBar {

    private static final long PROGRESS_BAR_TIMEOUT_CANCEL = 120 * 1000;
    private static CountDownTimer timeoutTimer;

    void startProgressBar(ProgressBar progressBar) {
        stopProgressBar(progressBar, true);
        progressBar.setMax((int) (PROGRESS_BAR_TIMEOUT_CANCEL / 1000));
        timeoutTimer = new CountDownTimer(PROGRESS_BAR_TIMEOUT_CANCEL, 1000) {

            public void onTick(long millisUntilFinished) {
                progressBar.incrementProgressBy(1);
            }

            public void onFinish() {
                stopProgressBar(progressBar, true);
            }

        }.start();
    }

    void stopProgressBar(ProgressBar progressBar, boolean isTimerStarted) {
        if (isTimerStarted && timeoutTimer != null) {
            progressBar.setProgress(0);
            timeoutTimer.cancel();
        }
    }
}
