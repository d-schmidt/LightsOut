package pw.dschmidt.lightsout;

import android.os.AsyncTask;

import pw.dschmidt.lightsout.util.Log;


public class UpdateText extends AsyncTask<Void, Float, Void> {

    private final Log log = Log.createLogger(getClass());

    private MainActivity mainActivity;


    public UpdateText(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }


    @Override
    protected Void doInBackground(Void... voids) {
        log.d("do in background");

        while (!isCancelled()) {

            publishProgress();

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.d("interrupted");
                return null;
            }
        }

        return null;
    }


    @Override
    protected void onCancelled(Void aVoid) {
        log.d("Cancelled");
        mainActivity = null;
    }


    @Override
    protected void onProgressUpdate(Float... values) {
        mainActivity.updateText();
    }
}
