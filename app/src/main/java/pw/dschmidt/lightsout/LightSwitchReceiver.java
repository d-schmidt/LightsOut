package pw.dschmidt.lightsout;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import pw.dschmidt.lightsout.util.Log;


public class LightSwitchReceiver extends BroadcastReceiver {

    public static final int OFF_REQ_CODE = 0x0ff;
    public static final int ON_REQ_CODE = 0x1;

    public static final String LIGHTOFF = "lightOff";
    private final Log log = Log.createLogger(getClass());


    @Override
    public void onReceive(Context context, Intent intent) {
        log.d("onReceive intent: %s", intent);
        final boolean lightOff = intent.getBooleanExtra(LIGHTOFF, false);

        if (lightOff) {
            LightSwitch.goDarkNow(context);
        } else {
            LightSwitch.lightsOn(context);
        }
    }
}
