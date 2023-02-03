package ee.ria.DigiDoc.android.signature.update;

import android.os.CountDownTimer;
import android.widget.ProgressBar;

class SignatureUpdateProgressBar {

    private static final long PROGRESS_BAR_TIMEOUT_CANCEL = 120 * 1000;
    private static CountDownTimer timeoutTimer;

    static void startProgressBar(ProgressBar progressBar) {
        if (progressBar.getProgress() == 0) {
            progressBar.setMax((int) (PROGRESS_BAR_TIMEOUT_CANCEL / 1000));
            timeoutTimer = new CountDownTimer(PROGRESS_BAR_TIMEOUT_CANCEL, 1000) {

                public void onTick(long millisUntilFinished) {
                    progressBar.incrementProgressBy(1);
                }

                public void onFinish() {
                    stopProgressBar(progressBar);
                }

            }.start();
        }
    }

    static void stopProgressBar(ProgressBar progressBar) {
        if (timeoutTimer != null) {
            progressBar.setProgress(0);
            timeoutTimer.cancel();
            timeoutTimer = null;
        }
    }
}
