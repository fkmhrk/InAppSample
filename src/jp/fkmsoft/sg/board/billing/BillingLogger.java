package jp.fkmsoft.sg.board.billing;

import android.util.Log;

public class BillingLogger {
    private static final boolean DEBUG_ENABLE = true;
    public static boolean isDebugEnabled() {
        return DEBUG_ENABLE;
    }
    public static void debug(String tag, String msg) {
        Log.d(tag, msg);
    }
    public static void info(String tag, String msg) {
        Log.i(tag, msg);
    }
    public static void warn(String tag, String msg) {
        Log.w(tag, msg);
    }
    public static void error(String tag, String msg) {
        Log.e(tag, msg);
    }
    
    
}
