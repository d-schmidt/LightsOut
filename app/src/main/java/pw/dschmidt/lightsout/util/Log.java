package pw.dschmidt.lightsout.util;


import android.support.annotation.NonNull;

import org.jetbrains.annotations.NonNls;

import pw.dschmidt.lightsout.BuildConfig;


public class Log {

    private final String tag;


    private Log(final String tag) {
        if (tag == null) {
            throw new IllegalArgumentException("tag is null");
        }

        if (tag.length() > 23) {
            this.tag = tag.substring(0, 23);
        } else {
            this.tag = tag;
        }
    }


    /**
     * @param tag to use
     * @return new instance of this Logger
     */
    private static Log createLogger(String tag) {
        return new Log(tag);
    }


    /**
     * @param clazz to create SimpleName tag from
     * @return new instance of this Logger
     */
    public static Log createLogger(Class<?> clazz) {
        return createLogger(clazz.getSimpleName());
    }


    /**
     * to wrap expensive debug message creation
     *
     * @return true if {@link android.util.Log#DEBUG} is loggable
     */
    public boolean isDebugEnabled() {
        return BuildConfig.DEBUG;
    }


    public void d(@NonNls @NonNull String msg) {
        if (isDebugEnabled()) {
            logD(msg);
        }
    }


    public void d(@NonNls @NonNull String msg, Throwable e) {
        if (isDebugEnabled()) {
            logD(createErrorMessage(msg, e));
        }
    }


    public void d(@NonNls @NonNull String format, @NonNls Object... params) {
        if (isDebugEnabled()) {
            logD(format(format, params));
        }
    }


    private void logD(String msg) {
        android.util.Log.d(tag, String.valueOf(msg));
    }


    public void i(@NonNls @NonNull String msg) {
        log(android.util.Log.INFO, msg);
    }


    public void i(@NonNls @NonNull String msg, Throwable e) {
        log(android.util.Log.INFO, msg, e);
    }


    public void i(@NonNls @NonNull String format, Object... params) {
        log(android.util.Log.INFO, format, params);
    }


    public void w(@NonNls @NonNull String msg) {
        log(android.util.Log.WARN, msg);
    }


    public void w(@NonNls @NonNull String msg, Throwable e) {
        log(android.util.Log.WARN, msg, e);
    }


    public void w(@NonNls @NonNull String format, Object... params) {
        log(android.util.Log.WARN, format, params);
    }


    public void e(@NonNls @NonNull String msg) {
        log(android.util.Log.ERROR, msg);
    }


    public void e(@NonNls @NonNull String msg, Throwable e) {
        log(android.util.Log.ERROR, msg, e);
    }


    public void e(@NonNls @NonNull String format, Object... params) {
        log(android.util.Log.ERROR, format, params);
    }


    private void log(int level, String msg) {
        android.util.Log.println(level, tag, String.valueOf(msg));
    }


    private void log(int level, String msg, Throwable e) {
        log(level, createErrorMessage(msg, e));
    }


    private String createErrorMessage(String msg, Throwable e) {
        return String.valueOf(msg) + '\n' + android.util.Log.getStackTraceString(e);
    }


    private void log(int level, String format, Object... params) {
        log(level, format(format, params));
    }


    private String format(String format, Object... params) {
        try {
            return String.format(format, params);
        } catch (IllegalArgumentException e) {
            android.util.Log.w(tag, "format() failed", e);
            return String.valueOf(format);
        } catch (NullPointerException e) {
            android.util.Log.w(tag, "format() failed", e);
            return String.valueOf(format);
        }
    }
}
