package pw.dschmidt.lightsout;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import pw.dschmidt.lightsout.util.Log;


public class BootReceiver extends BroadcastReceiver {

    private final Log log = Log.createLogger(getClass());


    @Override
    public void onReceive(Context context, Intent intent) {
        log.d("onReceive");
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {

            LightSwitch.planDarkness(context);

            // go dark if in darktime
            if (LightSwitch.shouldWeBeDark(context)) {
                LightSwitch.goDarkNow(context);
            }
        }
    }
}
