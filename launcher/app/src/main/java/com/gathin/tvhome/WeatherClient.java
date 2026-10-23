package com.gathin.tvhome;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * meu-chromecast (ADR-0006): the current temperature for the system's last known location, from Open-Meteo, every
 * 30 minutes off the main thread. The last reading is kept, so Home shows it at once after a restart while it is
 * still fresh; no location, no network or a stale reading hide the temperature instead of showing an error.
 */
final class WeatherClient {
    interface Listener {
        /** A fresh reading, or null when there is none to show. Called on the main thread. */
        void onWeather(WeatherNow now);
    }

    private static final long REFRESH_MS = 30 * 60 * 1000L;
    private static final String PREFS = "meu_chromecast_weather";
    private static final ExecutorService NETWORK = Executors.newSingleThreadExecutor();

    private final Context ctx;
    private final Handler main;
    private final Listener listener;
    private final Runnable tick = this::tick;
    private WeatherNow last;

    WeatherClient(Context ctx, Handler main, Listener listener) {
        this.ctx = ctx.getApplicationContext();
        this.main = main;
        this.listener = listener;
        SharedPreferences p = prefs();
        if (p.contains("fetched_at")) {
            last = new WeatherNow(p.getInt("temperature", 0), p.getInt("code", 0), p.getBoolean("day", true),
                    p.getLong("fetched_at", 0));
        }
    }

    void start() {
        main.removeCallbacks(tick);
        tick();
    }

    void stop() {
        main.removeCallbacks(tick);
    }

    private void tick() {
        long now = System.currentTimeMillis();
        listener.onWeather(last != null && last.fresh(now) ? last : null);
        if (last == null || now - last.fetchedAt >= REFRESH_MS || now < last.fetchedAt) fetch();
        main.postDelayed(tick, REFRESH_MS);
    }

    private void fetch() {
        NETWORK.execute(() -> {
            Location where = lastLocation();
            if (where == null) return;
            WeatherNow fresh = WeatherNow.parse(get(WeatherNow.requestUrl(where.getLatitude(), where.getLongitude())),
                    System.currentTimeMillis());
            if (fresh == null) return;
            prefs().edit().putInt("temperature", fresh.temperature).putInt("code", fresh.code)
                    .putBoolean("day", fresh.day).putLong("fetched_at", fresh.fetchedAt).apply();
            main.post(() -> {
                last = fresh;
                listener.onWeather(fresh);
            });
        });
    }

    /** The newest fix any provider already has; the launcher never asks for a new one. */
    private Location lastLocation() {
        if (ctx.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ctx.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        LocationManager lm = ctx.getSystemService(LocationManager.class);
        if (lm == null) return null;
        List<String> providers = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) providers.add(LocationManager.FUSED_PROVIDER);
        providers.add(LocationManager.NETWORK_PROVIDER);
        providers.add(LocationManager.PASSIVE_PROVIDER);
        providers.add(LocationManager.GPS_PROVIDER);
        Location best = null;
        for (String p : providers) {
            try {
                Location l = lm.getLastKnownLocation(p);
                if (l != null && (best == null || l.getTime() > best.getTime())) best = l;
            } catch (SecurityException | IllegalArgumentException ignored) {
                // a provider this device does not have, or a permission taken back meanwhile
            }
        }
        return best;
    }

    private static String get(String url) {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(10_000);
            c.setReadTimeout(10_000);
            if (c.getResponseCode() != 200) return null;
            try (InputStream in = c.getInputStream()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                for (int n; (n = in.read(buf)) > 0; ) out.write(buf, 0, n);
                return out.toString(StandardCharsets.UTF_8.name());
            }
        } catch (Exception e) {
            return null;
        } finally {
            if (c != null) c.disconnect();
        }
    }

    private SharedPreferences prefs() {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
