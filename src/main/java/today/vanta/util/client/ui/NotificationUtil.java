package today.vanta.util.client.ui;


import java.util.ArrayList;
import java.util.List;

public class NotificationUtil {
    // genuinely worst notification system but only thing that came to mind
    public static List<String> notifTitle = new ArrayList<>();
    public static List<String> notifMessage = new ArrayList<>();
    public static List<Long> notifLifetime = new ArrayList<>();
    public static List<Long> notifTime = new ArrayList<>();

    public static boolean registerNotificationBoolean(String title,String message,Long lifetimeMS) {
        notifTitle.add(title);
        notifMessage.add(message);
        notifLifetime.add(lifetimeMS);
        notifTime.add(System.currentTimeMillis());
        return true;
    }

    public static void registerNotification(String title,String message,Long lifetimeMS) {
        notifTitle.add(title);
        notifMessage.add(message);
        notifLifetime.add(lifetimeMS);
        notifTime.add(System.currentTimeMillis());
    }


}
