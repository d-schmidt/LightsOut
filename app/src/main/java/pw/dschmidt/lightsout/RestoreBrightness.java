package pw.dschmidt.lightsout;

import android.content.ContentResolver;
import android.os.AsyncTask;
import android.provider.Settings;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import pw.dschmidt.lightsout.util.Log;

import static android.provider.Settings.System.SCREEN_BRIGHTNESS;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE;


public class RestoreBrightness extends AsyncTask<Integer, Void, Integer[]> {

    private final Log log = Log.createLogger(getClass());
    private final AtomicLong sleepUntil;
    private final ContentResolver cr;


    public RestoreBrightness(ContentResolver cr) {

        this.cr = cr;
        this.sleepUntil = new AtomicLong(0);
        refreshTimer();
    }


    public void refreshTimer() {
        // sleep five more seconds
        sleepUntil.set(System.nanoTime() + TimeUnit.SECONDS.toNanos(5));
    }


    @Override
    protected Integer[] doInBackground(Integer... values) {
        log.d("do in background");

        try {
            long timeToSleep = sleepUntil.get() - System.nanoTime();

            while (timeToSleep > 0) {
                TimeUnit.NANOSECONDS.sleep(timeToSleep);
                timeToSleep = sleepUntil.get() - System.nanoTime();
            }
        } catch (InterruptedException e) {
            log.d("interrupted");
        }

        return values;
    }


    @Override
    protected void onCancelled(Integer[] values) {
        resetBrightness(values);
    }


    @Override
    protected void onPostExecute(Integer[] values) {
        resetBrightness(values);
    }


    private void resetBrightness(Integer[] values) {
        Settings.System.putInt(cr, SCREEN_BRIGHTNESS_MODE, values[0]);
        Settings.System.putInt(cr, SCREEN_BRIGHTNESS, values[1]);
    }
}
