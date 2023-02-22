package ee.ria.DigiDoc.android.signature.update;

import android.os.CountDownTimer;
import android.widget.ProgressBar;

import ee.ria.DigiDoc.R;

class SignatureUpdateProgressBar {

    private static final long MOBILE_ID_PROGRESS_BAR_TIMEOUT_CANCEL = 125 * 1000;
    private static final long SMART_ID_PROGRESS_BAR_TIMEOUT_CANCEL = 85 * 1000;
    private static CountDownTimer timeoutTimer;

    static void startProgressBar(ProgressBar progressBar) {
        if (progressBar != null && progressBar.getProgress() == 0) {
            progressBar.setMax((int) (isMobileIdProgressBar(progressBar) ? (MOBILE_ID_PROGRESS_BAR_TIMEOUT_CANCEL / 1000) : (SMART_ID_PROGRESS_BAR_TIMEOUT_CANCEL / 1000)));
            timeoutTimer = new CountDownTimer(isMobileIdProgressBar(progressBar) ? MOBILE_ID_PROGRESS_BAR_TIMEOUT_CANCEL : SMART_ID_PROGRESS_BAR_TIMEOUT_CANCEL, 1000) {

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
        if (progressBar != null) {
            progressBar.setProgress(0);
        }
        if (timeoutTimer != null) {
            timeoutTimer.cancel();
            timeoutTimer = null;
        }
    }

    static boolean isMobileIdProgressBar(ProgressBar progressBar) {
        return progressBar.getId() == R.id.activityIndicatorMobileId;
    }
}
