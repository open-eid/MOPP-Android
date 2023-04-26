package ee.ria.DigiDoc.common;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class NotificationUtil {

    public static void createNotificationChannel(Context context, String notificationChannel, String channelName) {
        NotificationChannel channel = new NotificationChannel(notificationChannel,
                channelName,
                NotificationManager.IMPORTANCE_HIGH);
        NotificationManager systemService = context.getSystemService(NotificationManager.class);
        if (systemService != null) {
            systemService.createNotificationChannel(channel);
        }
    }

    public static Notification createNotification(Context context,
                                                  String notificationChannel,
                                                  int smallIcon,
                                                  @Nullable String title,
                                                  @Nullable String text,
                                                  int priority,
                                                  boolean isSilent) {
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context, notificationChannel)
                .setSmallIcon(smallIcon)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(priority)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setAutoCancel(true)
                .setSilent(isSilent);
        if (PowerUtil.isPowerSavingMode(context)) {
            notification.setSound(null)
                    .setVibrate(null)
                    .setLights(0, 0, 0);
        }
        return notification.build();
    }
}
