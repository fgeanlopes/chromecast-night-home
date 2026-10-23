package com.gathin.tvhome;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * meu-chromecast: the current temperature from Open-Meteo (ADR-0006) and the rules that turn it into an icon and a
 * phrase. Plain Java, so it is unit-tested; WeatherClient fetches it.
 */
final class WeatherNow {

    enum Icon { SUN, MOON, CLOUD_SUN, CLOUD_MOON, CLOUD, FOG, RAIN, STORM }

    enum Band { RAIN, HOT, PLEASANT, COOL, COLD }

    /** A reading older than this is not shown. */
    static final long MAX_AGE_MS = 2 * 60 * 60 * 1000L;

    private static final Pattern CURRENT = Pattern.compile("\"current\"\\s*:\\s*\\{([^}]*)\\}");
    private static final Pattern TEMPERATURE = Pattern.compile("\"temperature_2m\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)");
    private static final Pattern CODE = Pattern.compile("\"weather_code\"\\s*:\\s*(\\d+)");
    private static final Pattern DAY = Pattern.compile("\"is_day\"\\s*:\\s*([01])");

    final int temperature;
    final int code;
    final boolean day;
    final long fetchedAt;

    WeatherNow(int temperature, int code, boolean day, long fetchedAt) {
        this.temperature = temperature;
        this.code = code;
        this.day = day;
        this.fetchedAt = fetchedAt;
    }

    /** The "current" block of a forecast response, or null when it is missing or incomplete. */
    static WeatherNow parse(String json, long fetchedAt) {
        if (json == null) return null;
        Matcher current = CURRENT.matcher(json);
        if (!current.find()) return null;
        String block = current.group(1);
        Matcher t = TEMPERATURE.matcher(block), c = CODE.matcher(block), d = DAY.matcher(block);
        if (!t.find() || !c.find() || !d.find()) return null;
        return new WeatherNow((int) Math.round(Double.parseDouble(t.group(1))), Integer.parseInt(c.group(1)),
                "1".equals(d.group(1)), fetchedAt);
    }

    boolean fresh(long now) {
        long age = now - fetchedAt;
        return age >= 0 && age <= MAX_AGE_MS;
    }

    Icon icon() {
        return icon(code, day);
    }

    Band band() {
        return band(temperature, raining(code));
    }

    /** WMO weather codes as Open-Meteo reports them. Snow has no icon of its own here (it does not snow at home). */
    static Icon icon(int code, boolean day) {
        if (code == 0) return day ? Icon.SUN : Icon.MOON;
        if (code == 1 || code == 2) return day ? Icon.CLOUD_SUN : Icon.CLOUD_MOON;
        if (code == 45 || code == 48) return Icon.FOG;
        if (code >= 95 && code <= 99) return Icon.STORM;
        if (raining(code)) return Icon.RAIN;
        return Icon.CLOUD;
    }

    static boolean raining(int code) {
        return (code >= 51 && code <= 67) || (code >= 80 && code <= 82) || (code >= 95 && code <= 99);
    }

    static Band band(int temperature, boolean raining) {
        if (raining) return Band.RAIN;
        if (temperature >= 28) return Band.HOT;
        if (temperature >= 20) return Band.PLEASANT;
        if (temperature >= 13) return Band.COOL;
        return Band.COLD;
    }

    /** Two decimals (~1 km): enough for the weather, and the exact position never leaves the TV. */
    static String requestUrl(double latitude, double longitude) {
        return String.format(Locale.ROOT,
                "https://api.open-meteo.com/v1/forecast?latitude=%.2f&longitude=%.2f&current=temperature_2m,weather_code,is_day",
                latitude, longitude);
    }
}
