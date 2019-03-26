package pw.dschmidt.lightsout;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;

import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;

import java.util.Arrays;

import pw.dschmidt.lightsout.util.Log;

import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
import static android.content.pm.PackageManager.DONT_KILL_APP;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;


public class MainActivity extends Activity {

    private final Log log = Log.createLogger(getClass());
    private TextView nightText;
    private TextView isDarkText;
    private Button startTimeBtn;
    private Button stopTimeBtn;
    private UpdateText textUpdater;
    private Switch activateSwitch;
    private RestoreBrightness restoreBrightness;

    private int startHour = 22;
    private int startMinutes = 0;
    private int stopHour = 8;
    private int stopMinutes = 0;
    private int darkValue = 0;


    private static boolean isM() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }


    private static String printTime(int hour, int minute) {
        return DateTimeFormat.shortTime().print(new LocalTime(hour, minute));
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        log.d("on create");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SeekBar nightSeeker = findViewById(R.id.nightLight);
        nightText = findViewById(R.id.nightText);
        startTimeBtn = findViewById(R.id.startTimeBtn);
        stopTimeBtn = findViewById(R.id.stopTimeBtn);
        activateSwitch = findViewById(R.id.activeSwitch);
        isDarkText = findViewById(R.id.isDarkText);

        final SharedPreferences preferences = getSharedPreferences(Prefs.FILE, 0);
        startHour = preferences.getInt(Prefs.START_HOUR, startHour);
        startMinutes = preferences.getInt(Prefs.START_MINUTE, startMinutes);
        stopHour = preferences.getInt(Prefs.STOP_HOUR, stopHour);
        stopMinutes = preferences.getInt(Prefs.STOP_MINUTE, stopMinutes);
        darkValue = preferences.getInt(Prefs.DARK_VALUE, LightSwitch.DEFAULT_DARK);
        activateSwitch.setChecked(preferences.getBoolean(Prefs.ACTIVE, false));

        if (isM()) {
            if (Settings.System.canWrite(this)) {
                LightSwitch.updateBrightness(getApplicationContext());
            } else {
                requestSettingsPermission();
            }
        } else {
            LightSwitch.updateBrightness(getApplicationContext());
        }

        log.d("onCreate() loaded darkness value: %s", darkValue);

        nightText.setText(getString(R.string.night_bright_text, darkValue));
        startTimeBtn.setText(printTime(startHour, startMinutes));
        stopTimeBtn.setText(printTime(stopHour, stopMinutes));
        nightSeeker.setProgress(darkValue);

        nightSeeker.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            private final Log log = Log.createLogger(getClass());


            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean wasUser) {

                log.d("progress changed: user? %s", wasUser);
                if (!wasUser || darkValue == progress) {
                    return;
                }

                darkValue = progress;
                nightText.setText(getString(R.string.night_bright_text, darkValue));

                final SharedPreferences.Editor editor = getSharedPreferences(Prefs.FILE, 0).edit();
                editor.putInt(Prefs.DARK_VALUE, darkValue);
                editor.apply();

                final ContentResolver cr = getContentResolver();

                if (!LightSwitch.shouldWeBeDark(getApplicationContext())) {
                    int brightMode = LightSwitch.getCurrentBrightMode(cr);
                    int brightness = LightSwitch.getCurrentBrightness(cr);
                    log.d("onProgressChanged() backing up mode: %s bright: %s", brightMode,
                            brightness);

                    if (restoreBrightness == null
                            || restoreBrightness.getStatus() == AsyncTask.Status.FINISHED) {
                        restoreBrightness = new RestoreBrightness(cr);
                        restoreBrightness.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                                brightMode,
                                brightness);
                    }
                    restoreBrightness.refreshTimer();
                }

                Settings.System.putInt(cr, SCREEN_BRIGHTNESS_MODE, SCREEN_BRIGHTNESS_MODE_MANUAL);
                Settings.System.putInt(cr, SCREEN_BRIGHTNESS, darkValue);
            }


            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }


            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestSettingsPermission() {
        log.d("requestSettingsPermission() requesting permissions");
        // start with id to read on intent callback
        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
        intent.setData(Uri.parse("package:" + this.getPackageName()));
        startActivityForResult(intent, 247);
    }


    @Override
    protected void onDestroy() {
        log.d("on Destroy");
        super.onDestroy();
    }


    @Override
    protected void onResume() {
        log.d("on Resume");
        super.onResume();
    }


    @Override
    protected void onRestart() {
        log.d("on Restart");
        super.onRestart();
    }


    @Override
    protected void onStop() {
        log.d("on Stop");

        if (textUpdater != null) {
            textUpdater.cancel(true);
            textUpdater = null;
        }

        if (restoreBrightness != null
                && restoreBrightness.getStatus() != AsyncTask.Status.FINISHED) {

            restoreBrightness.cancel(true);

            try {
                restoreBrightness.get();
            } catch (Exception e) {
                log.e("onStop() expected error after cancel get", e);
            }

            restoreBrightness = null;
        }

        super.onStop();
    }


    @Override
    protected void onStart() {
        log.d("on Start");

        textUpdater = new UpdateText(this);
        textUpdater.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        super.onStart();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        log.d("activity result: request:%s result:%s data:%s", requestCode, resultCode, data);
        if (isM()) {
            if (requestCode == 247) {
                log.d("can we now write settings? %s", Settings.System.canWrite(this));
                // TODO catch not being able to do stuff
            }
        }
    }


    private boolean is24hoursTime() {
        try {
            int hours = Settings.System.getInt(getContentResolver(), Settings.System.TIME_12_24);
            log.d("24h mode? %s", hours);
            return hours == 24;
        } catch (Settings.SettingNotFoundException e) {
            log.e("getting 24h time setting failed", e);
        }

        return false;
    }


    public void updateText() {
        isDarkText.setText(LightSwitch.areWeDark(
                getSharedPreferences(Prefs.FILE, 0)) ? "lights off" : "lights on");
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        log.d("on permission result entered");
        log.d(String.valueOf(requestCode));
        log.d(Arrays.toString(permissions));
        log.d(Arrays.toString(grantResults));
    }


    public void onStartTimeBtnClick(View v) {
        log.d("start time button clicked");
        TimePickerDialog tp1 = new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                startHour = hourOfDay;
                startMinutes = minute;
                startTimeBtn.setText(printTime(startHour, startMinutes));

                final SharedPreferences.Editor editor = getSharedPreferences(Prefs.FILE, 0).edit();
                editor.putInt(Prefs.START_HOUR, startHour);
                editor.putInt(Prefs.START_MINUTE, startMinutes);
                editor.apply();

                final Context context = getApplicationContext();
                LightSwitch.updateBrightness(context);

                if (activateSwitch.isChecked()) {
                    LightSwitch.planDarkness(context);
                }
            }
        }, startHour, startMinutes, is24hoursTime());

        tp1.show();
    }


    public void onStopTimeBtnClick(View v) {
        log.d("stop time button clicked");
        TimePickerDialog tp1 = new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                stopHour = hourOfDay;
                stopMinutes = minute;
                stopTimeBtn.setText(printTime(stopHour, stopMinutes));

                final SharedPreferences.Editor editor = getSharedPreferences(Prefs.FILE, 0).edit();
                editor.putInt(Prefs.STOP_HOUR, stopHour);
                editor.putInt(Prefs.STOP_MINUTE, stopMinutes);
                editor.apply();

                final Context context = getApplicationContext();
                LightSwitch.updateBrightness(context);

                if (activateSwitch.isChecked()) {
                    LightSwitch.planDarkness(context);
                }
            }
        }, stopHour, stopMinutes, is24hoursTime());

        tp1.show();
    }


    public void onActiveSwitchClick(View v) {
        Switch theSwitch = (Switch) v;
        log.d("onActiveSwitchClick() clicked now:%s", theSwitch.isChecked());

        final SharedPreferences.Editor editor = getSharedPreferences(Prefs.FILE, 0).edit();
        editor.putBoolean(Prefs.ACTIVE, theSwitch.isChecked());
        editor.apply();

        final Context context = getApplicationContext();

        ComponentName receiver = new ComponentName(context, BootReceiver.class);
        PackageManager pm = context.getPackageManager();

        if (theSwitch.isChecked()) {
            // boot receiver on
            pm.setComponentEnabledSetting(receiver, COMPONENT_ENABLED_STATE_ENABLED, DONT_KILL_APP);

            if (LightSwitch.shouldWeBeDark(context)) {
                LightSwitch.goDarkNow(context);
            }

            LightSwitch.planDarkness(context);
        } else {
            // boot receiver off
            pm.setComponentEnabledSetting(receiver, COMPONENT_ENABLED_STATE_DISABLED,
                    DONT_KILL_APP);

            LightSwitch.lightsOn(context);
            LightSwitch.cancelPlans(context);
        }
    }
}
