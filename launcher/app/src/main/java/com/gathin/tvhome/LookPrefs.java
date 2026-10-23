package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;

/** Stores the HomeLook in its own preferences file, out of reach of Config's theme-push reset. */
final class LookPrefs {
    private static final String NAME = "meu_chromecast_look";

    private LookPrefs() {
    }

    static HomeLook load(Context ctx) {
        return HomeLook.from(prefs(ctx).getAll());
    }

    /** commit, not apply: Home recreates right after, and must read the new value. */
    static void save(Context ctx, String key, Object value) {
        SharedPreferences.Editor e = prefs(ctx).edit();
        if (value instanceof Boolean) e.putBoolean(key, (Boolean) value);
        else e.putString(key, String.valueOf(value));
        e.commit();
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }
}
