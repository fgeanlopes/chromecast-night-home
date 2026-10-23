package com.gathin.tvhome;

import android.app.Activity;
import android.graphics.Typeface;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

import java.util.Locale;

/**
 * meu-chromecast header, built from the Options switches: the greeting (large, or small over a big clock), the
 * big clock, and one small line with the small clock and the date. Rearranges activity_main.xml's own views and
 * adds the small line in code, so the layout file stays as upstream has it.
 */
final class LookHeader {
    private LookHeader() {
    }

    /** The small line and the temperature at its end, for showWeather. */
    static final class Views {
        final TextClock line;
        final TextView separator, temperature;
        final android.widget.ImageView icon;
        final View row;

        Views(View row, TextClock line, TextView separator, android.widget.ImageView icon, TextView temperature) {
            this.row = row;
            this.line = line;
            this.separator = separator;
            this.icon = icon;
            this.temperature = temperature;
        }
    }

    static Views apply(Activity a, HomeLook look) {
        TextView greeting = a.findViewById(R.id.greeting);
        TextClock clock = a.findViewById(R.id.clock);
        TextClock ampm = a.findViewById(R.id.ampm);
        LinearLayout block = a.findViewById(R.id.leftBlock);
        ViewGroup clockRow = (ViewGroup) clock.getParent();
        Typeface light = font(a, R.font.outfit_light), extraLight = font(a, R.font.outfit_extralight);
        int primary = look.primaryText(), secondary = look.secondaryText();

        if (!look.greeting) {
            greeting.setVisibility(View.GONE);
        } else if (!look.bigClock) {
            greeting.setAllCaps(false);
            greeting.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32f);
            greeting.setLetterSpacing(-0.01f);
            if (light != null) greeting.setTypeface(light);
            greeting.setTextColor(primary);
        } else {
            greeting.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
            greeting.setTextColor(secondary);
        }

        // The date moves to the small line: the row keeps only the big clock and its AM/PM.
        for (int i = 2; i < clockRow.getChildCount(); i++) clockRow.getChildAt(i).setVisibility(View.GONE);
        clockRow.setVisibility(look.bigClock ? View.VISIBLE : View.GONE);
        clock.setTextSize(TypedValue.COMPLEX_UNIT_SP, 60f);
        if (extraLight != null) clock.setTypeface(extraLight);
        clock.setTextColor(primary);
        ampm.setTextColor(secondary);

        Locale locale = a.getResources().getConfiguration().getLocales().get(0);
        String datePattern = DateFormat.getBestDateTimePattern(locale, look.bigClock ? "EEEdMMM" : "EEEEdMMMM");
        String twelve = HomeLook.smallLinePattern(DateFormat.getBestDateTimePattern(locale, "hmma"), datePattern,
                look.smallClock, look.date);
        String twentyFour = HomeLook.smallLinePattern(DateFormat.getBestDateTimePattern(locale, "Hm"), datePattern,
                look.smallClock, look.date);
        TextClock line = new TextClock(a);
        line.setFormat12Hour(twelve);
        line.setFormat24Hour(twentyFour);
        line.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
        if (light != null) line.setTypeface(light);
        line.setTextColor(secondary);
        line.setSingleLine(true);
        line.setVisibility(twelve.isEmpty() ? View.GONE : View.VISIBLE);

        // The temperature sits at the end of the small line, in the same size and colour (Phase 2, ADR-0006).
        float density = a.getResources().getDisplayMetrics().density;
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.addView(line);
        TextView separator = smallText(a, light, secondary, " · ");
        row.addView(separator);
        android.widget.ImageView icon = new android.widget.ImageView(a);
        icon.setImageTintList(android.content.res.ColorStateList.valueOf(secondary));
        int iconSize = Math.round(20 * density);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(iconSize, iconSize);
        ilp.setMarginEnd(Math.round(4 * density));
        row.addView(icon, ilp);
        TextView temperature = smallText(a, light, secondary, "");
        row.addView(temperature);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Math.round(6 * density);
        lp.setMarginStart(Math.round(2 * density));
        block.addView(row, lp);
        Views views = new Views(row, line, separator, icon, temperature);
        showWeather(views, look, null);
        return views;
    }

    /** Shows or hides the temperature at the end of the small line; null hides it (no fresh reading). */
    static void showWeather(Views v, HomeLook look, WeatherNow now) {
        boolean weather = look.headerTemperature && now != null;
        boolean line = v.line.getVisibility() == View.VISIBLE;
        v.separator.setVisibility(weather && line ? View.VISIBLE : View.GONE);
        v.icon.setVisibility(weather ? View.VISIBLE : View.GONE);
        v.temperature.setVisibility(weather ? View.VISIBLE : View.GONE);
        if (weather) {
            v.icon.setImageResource(iconRes(now.icon()));
            v.temperature.setText(now.temperature + "°");
        }
        v.row.setVisibility(weather || line ? View.VISIBLE : View.GONE);
    }

    static int iconRes(WeatherNow.Icon icon) {
        switch (icon) {
            case SUN: return R.drawable.ic_meu_weather_sun;
            case MOON: return R.drawable.ic_meu_weather_moon;
            case CLOUD_SUN: return R.drawable.ic_meu_weather_cloud_sun;
            case CLOUD_MOON: return R.drawable.ic_meu_weather_cloud_moon;
            case FOG: return R.drawable.ic_meu_weather_fog;
            case RAIN: return R.drawable.ic_meu_weather_rain;
            case STORM: return R.drawable.ic_meu_weather_storm;
            default: return R.drawable.ic_meu_weather_cloud;
        }
    }

    static TextView smallText(Activity a, Typeface font, int color, String text) {
        TextView t = new TextView(a);
        t.setText(text);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
        if (font != null) t.setTypeface(font);
        t.setTextColor(color);
        t.setSingleLine(true);
        return t;
    }

    static Typeface font(Activity a, int res) {
        try {
            return a.getResources().getFont(res);   // API 26+, the launcher's minSdk
        } catch (RuntimeException e) {
            return null;
        }
    }
}
