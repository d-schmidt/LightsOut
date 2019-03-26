package pw.dschmidt.lightsout;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;

import org.joda.time.DateTime;
import org.joda.time.LocalTime;

import pw.dschmidt.lightsout.util.Log;

import static android.provider.Settings.System.SCREEN_BRIGHTNESS;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;
import static pw.dschmidt.lightsout.LightSwitchReceiver.OFF_REQ_CODE;
import static pw.dschmidt.lightsout.LightSwitchReceiver.ON_REQ_CODE;


public class LightSwitch {

    public static final int DEFAULT_DARK = 0;
    public static final int DEFAULT_BRIGHT = 127;
    public static final int DEFAULT_MODE = SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
    private static final Log log = Log.createLogger(LightSwitch.class);


    public static void updateBrightness(Context context) {
        if (shouldWeBeDark(context)) {
            goDarkNow(context);
        } else {
            lightsOn(context);
        }
    }


    public static void goDarkNow(Context context) {
        log.d("goDarkNow() going dark");

        final SharedPreferences preferences = context.getSharedPreferences(Prefs.FILE, 0);
        final ContentResolver cr = context.getContentResolver();

        if (!areWeDark(preferences)) {
            log.d("goDarkNow() saving bright brightness values");
            // save current values
            int brightMode = getCurrentBrightMode(cr);
            int brightness = getCurrentBrightness(cr);

            final SharedPreferences.Editor editor = preferences.edit();

            editor.putInt(Prefs.BRIGHT_MODE, brightMode);
            editor.putInt(Prefs.BRIGHT_VALUE, brightness);
            // save darkness enabled
            editor.putBoolean(Prefs.OUR_DARKNESS, true);

            editor.apply();

            // create notification
            final Notification.Builder mBuilder = new Notification.Builder(context)
                    .setSmallIcon(R.drawable.ic_brightness_medium_white_24dp)
                    .setContentTitle("LightsOut: light is off")
                    .setContentText("screen is currently dark")
                    .setShowWhen(false);

            Intent resultIntent = new Intent(context, MainActivity.class);
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
            stackBuilder.addParentStack(MainActivity.class);
            stackBuilder.addNextIntent(resultIntent);
            PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            mBuilder.setContentIntent(resultPendingIntent);
            NotificationManager mNotificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify("1337", 5, mBuilder.build());
        }

        int darkValue = preferences.getInt(Prefs.DARK_VALUE, DEFAULT_DARK);

        setBrightnessAndMode(cr, darkValue, SCREEN_BRIGHTNESS_MODE_MANUAL);
    }


    public static void lightsOn(Context context) {
        log.d("lightsOn() switching light on");
        final SharedPreferences preferences = context.getSharedPreferences(Prefs.FILE, 0);

        if (areWeDark(preferences)) {
            int brightMode = preferences.getInt(Prefs.BRIGHT_MODE,
                    SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
            int brightValue = preferences.getInt(Prefs.BRIGHT_VALUE, DEFAULT_BRIGHT);

            final SharedPreferences.Editor editor = preferences.edit();
            // save darkness disabled
            editor.putBoolean(Prefs.OUR_DARKNESS, false);

            editor.apply();

            final ContentResolver cr = context.getContentResolver();
            setBrightnessAndMode(cr, brightValue, brightMode);
        }

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel("1337", 5);
    }


    private static void setBrightnessAndMode(ContentResolver cr, int brightness, int mode) {
        log.d("setBrightnessAndMode() setting light to value=%s mode=%s", brightness, mode);
        Settings.System.putInt(cr, SCREEN_BRIGHTNESS_MODE, mode);
        Settings.System.putInt(cr, SCREEN_BRIGHTNESS, brightness);
    }


    public static void planDarkness(Context context) {
        log.d("planDarkness() adding alarm entry for upcoming darkness");
        final SharedPreferences preferences = context.getSharedPreferences(Prefs.FILE, 0);
        int startHour = preferences.getInt(Prefs.START_HOUR, 0);
        int startMinutes = preferences.getInt(Prefs.START_MINUTE, 0);
        int stopHour = preferences.getInt(Prefs.STOP_HOUR, 0);
        int stopMinutes = preferences.getInt(Prefs.STOP_MINUTE, 0);

        DateTime start = DateTime.now().withTime(new LocalTime(startHour, startMinutes));
        if (DateTime.now().isAfter(start)) {
            // first alarm tomorrow
            start = start.plusDays(1);
        }

        DateTime stop = DateTime.now().withTime(new LocalTime(stopHour, stopMinutes));
        if (DateTime.now().isAfter(stop)) {
            // we are still bright, first alarm tomorrow
            stop = stop.plusDays(1);
        }

        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingOff = getSwitchIntent(context, OFF_REQ_CODE);
        PendingIntent pendingOn = getSwitchIntent(context, ON_REQ_CODE);

        alarmMgr.setRepeating(AlarmManager.RTC, start.getMillis(), AlarmManager.INTERVAL_DAY,
                pendingOff);
        alarmMgr.setRepeating(AlarmManager.RTC, stop.getMillis(), AlarmManager.INTERVAL_DAY,
                pendingOn);
    }


    public static void cancelPlans(Context context) {
        log.d("cancelPlans() removing alarm registrations");
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        PendingIntent pendingOff = getSwitchIntent(context, OFF_REQ_CODE);
        PendingIntent pendingOn = getSwitchIntent(context, ON_REQ_CODE);

        alarmMgr.cancel(pendingOff);
        alarmMgr.cancel(pendingOn);
    }


    private static PendingIntent getSwitchIntent(Context context, int reqCode) {
        Intent offIntent = new Intent(context, LightSwitchReceiver.class);
        offIntent.putExtra(LightSwitchReceiver.LIGHTOFF, reqCode == OFF_REQ_CODE);
        return PendingIntent.getBroadcast(context, reqCode, offIntent, 0);
    }


    public static boolean shouldWeBeDark(Context context) {
        log.d("shouldWeBeDark() checking times and activation");

        final SharedPreferences preferences = context.getSharedPreferences(Prefs.FILE, 0);

        if (!preferences.getBoolean(Prefs.ACTIVE, false)) {
            return false;
        }

        int startHour = preferences.getInt(Prefs.START_HOUR, 0);
        int startMinutes = preferences.getInt(Prefs.START_MINUTE, 0);
        int stopHour = preferences.getInt(Prefs.STOP_HOUR, 0);
        int stopMinutes = preferences.getInt(Prefs.STOP_MINUTE, 0);

        LocalTime start = new LocalTime(startHour, startMinutes);
        LocalTime stop = new LocalTime(stopHour, stopMinutes);
        LocalTime now = LocalTime.now();

        if (start.isBefore(stop)) {
            return now.isAfter(start) && now.isBefore(stop);
        }

        return now.isBefore(stop) || now.isAfter(start);
    }


    public static boolean areWeDark(final SharedPreferences preferences) {
        return preferences.getBoolean(Prefs.OUR_DARKNESS, false);
    }


    public static int getCurrentBrightness(ContentResolver cr) {
        try {
            return Settings.System.getInt(cr, SCREEN_BRIGHTNESS);
        } catch (Settings.SettingNotFoundException e) {
            return DEFAULT_BRIGHT;
        }
    }


    public static int getCurrentBrightMode(ContentResolver cr) {
        try {
            return Settings.System.getInt(cr, SCREEN_BRIGHTNESS_MODE);
        } catch (Settings.SettingNotFoundException e) {
            return DEFAULT_MODE;
        }
    }
}
