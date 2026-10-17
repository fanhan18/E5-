package com.operit.printhelper.core;

import android.content.Context;
import android.content.SharedPreferences;

public class PantumSettings {
    private static final String PREFS = "pantum_helper";

    public static String host(Context c) { return sp(c).getString("host", "10.0.0.20"); }
    public static int port(Context c) { return sp(c).getInt("port", 631); }
    public static String path(Context c) { return sp(c).getString("path", "ipp/print"); }
    /** true = 短边翻 */
    public static boolean shortEdge(Context c) { return sp(c).getBoolean("short_edge", false); }
    /** true = 上进纸 */
    public static boolean topFeed(Context c) { return sp(c).getBoolean("top_feed", false); }
    /** true = 偶数阶段逆序发送（后出在上） */
    public static boolean reverseEven(Context c) { return sp(c).getBoolean("reverse_even", true); }
    public static int dpi(Context c) { return sp(c).getInt("dpi", 300); }
    public static String media(Context c) { return sp(c).getString("media", "iso_a4_210x297mm"); }

    public static void setHost(Context c, String v) { sp(c).edit().putString("host", v).apply(); }
    public static void setPort(Context c, int v) { sp(c).edit().putInt("port", v).apply(); }
    public static void setPath(Context c, String v) { sp(c).edit().putString("path", v).apply(); }
    public static void setShortEdge(Context c, boolean v) { sp(c).edit().putBoolean("short_edge", v).apply(); }
    public static void setTopFeed(Context c, boolean v) { sp(c).edit().putBoolean("top_feed", v).apply(); }
    public static void setReverseEven(Context c, boolean v) { sp(c).edit().putBoolean("reverse_even", v).apply(); }
    public static void setDpi(Context c, int v) { sp(c).edit().putInt("dpi", v).apply(); }
    public static void setMedia(Context c, String v) { sp(c).edit().putString("media", v).apply(); }

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
