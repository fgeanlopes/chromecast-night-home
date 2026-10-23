package com.gathin.tvhome;

import java.util.HashMap;
import java.util.Map;

/**
 * The look chosen on the TV from the gear's Theme and Options panels (meu-chromecast fork): card style,
 * background and the header switches. Plain Java, so the rules are unit-tested; LookPrefs stores it.
 * Kept in its own preferences file, so a theme push (Config.syncThemeChoices) never clears it.
 */
final class HomeLook {

    enum Cards {
        CLASSIC("classic"), NIGHT("night"), GLASS("glass");

        final String id;

        Cards(String id) {
            this.id = id;
        }
    }

    enum Background {
        SKY("sky", 0xFF000000, -1, true, 1f, -1, 0x141416, 0xFFFFFFFF, 0xC7FFFFFF),
        GRAPHITE("graphite", 0xFF0A0A0B, 0, false, 0f, 100, 0x141416, 0xFFF2F2F2, 0xFFA1A1A6),
        MIDNIGHT("midnight", 0xFF060A14, 0, true, 0.48f, 60, 0x1C2234, 0xFFEEF1F8, 0xFFA3ACC2);

        final String id;
        final int color;
        /** sky_intensity for this background; -1 keeps the theme's. */
        final int sky;
        final boolean glow;
        /** Ambient glow strength relative to the launcher's (1 = as before). */
        final float glowStrength;
        /** Dock band opacity in percent; -1 keeps the theme's. */
        final int dockOpacity;
        final int dockRgb;
        final int primaryText, secondaryText;

        Background(String id, int color, int sky, boolean glow, float glowStrength, int dockOpacity, int dockRgb,
                   int primaryText, int secondaryText) {
            this.id = id;
            this.color = color;
            this.sky = sky;
            this.glow = glow;
            this.glowStrength = glowStrength;
            this.dockOpacity = dockOpacity;
            this.dockRgb = dockRgb;
            this.primaryText = primaryText;
            this.secondaryText = secondaryText;
        }
    }

    static final String KEY_CARDS = "cards", KEY_BACKGROUND = "background", KEY_GREETING = "greeting",
            KEY_BIG_CLOCK = "big_clock", KEY_SMALL_CLOCK = "small_clock", KEY_DATE = "date",
            KEY_BIGGER_CARDS = "bigger_cards", KEY_HEADER_TEMPERATURE = "header_temperature",
            KEY_WEATHER_LINE = "weather_line";

    /** Name on a Night tile and the dimmer text of the Night look: grey instead of white in a dark room. */
    static final int NIGHT_NAME = 0xFFD8D8DC, NIGHT_TEXT = 0xFFCFCFD4, NIGHT_SECONDARY = 0xB8CFCFD4;
    /** Focus ring of Night and Glass: grey, opaque, flush with the card (white glares in a dark room). */
    static final int GREY_RING = 0xFFA8A8B0;

    final Cards cards;
    final Background background;
    final boolean greeting, bigClock, smallClock, date, biggerCards, headerTemperature, weatherLine;

    private HomeLook(Cards cards, Background background, boolean greeting, boolean bigClock, boolean smallClock,
                     boolean date, boolean biggerCards, boolean headerTemperature, boolean weatherLine) {
        this.cards = cards;
        this.background = background;
        this.greeting = greeting;
        this.bigClock = bigClock;
        this.smallClock = smallClock;
        this.date = date;
        this.biggerCards = biggerCards;
        this.headerTemperature = headerTemperature;
        this.weatherLine = weatherLine;
    }

    /** The saved choices (SharedPreferences.getAll()); a missing, unknown or mistyped value keeps its default. */
    static HomeLook from(Map<String, ?> saved) {
        Cards cards = Cards.CLASSIC;
        Object c = saved.get(KEY_CARDS);
        for (Cards v : Cards.values()) if (v.id.equals(c)) cards = v;
        Background background = Background.SKY;
        Object b = saved.get(KEY_BACKGROUND);
        for (Background v : Background.values()) if (v.id.equals(b)) background = v;
        return new HomeLook(cards, background, bool(saved, KEY_GREETING, true), bool(saved, KEY_BIG_CLOCK, false),
                bool(saved, KEY_SMALL_CLOCK, true), bool(saved, KEY_DATE, true), bool(saved, KEY_BIGGER_CARDS, false),
                bool(saved, KEY_HEADER_TEMPERATURE, false), bool(saved, KEY_WEATHER_LINE, true));
    }

    private static boolean bool(Map<String, ?> saved, String key, boolean fallback) {
        Object v = saved.get(key);
        return v instanceof Boolean ? (Boolean) v : fallback;
    }

    Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<>();
        m.put(KEY_CARDS, cards.id);
        m.put(KEY_BACKGROUND, background.id);
        m.put(KEY_GREETING, greeting);
        m.put(KEY_BIG_CLOCK, bigClock);
        m.put(KEY_SMALL_CLOCK, smallClock);
        m.put(KEY_DATE, date);
        m.put(KEY_BIGGER_CARDS, biggerCards);
        m.put(KEY_HEADER_TEMPERATURE, headerTemperature);
        m.put(KEY_WEATHER_LINE, weatherLine);
        return m;
    }

    /** This look with one choice changed; every other choice stays as it is. */
    HomeLook with(String key, Object value) {
        Map<String, Object> m = toMap();
        m.put(key, value);
        return from(m);
    }

    boolean simpleTiles() {
        return cards != Cards.CLASSIC;
    }

    boolean showsHeroTitle(boolean dockTitle) {
        return dockTitle && cards == Cards.CLASSIC;
    }

    boolean ambientGlow(boolean themeGlow) {
        return themeGlow && background.glow && cards != Cards.NIGHT;
    }

    /** 0 keeps the launcher's glass highlight. */
    int focusRingColor() {
        return simpleTiles() ? GREY_RING : 0;
    }

    int primaryText() {
        return cards == Cards.NIGHT ? NIGHT_TEXT : background.primaryText;
    }

    int secondaryText() {
        return cards == Cards.NIGHT ? NIGHT_SECONDARY : background.secondaryText;
    }

    /** "h:mm a · EEEE d MMMM" for the header's small line, with only the parts that are switched on. */
    static String smallLinePattern(String timePattern, String datePattern, boolean time, boolean date) {
        if (time && date) return timePattern + " '·' " + datePattern;
        if (time) return timePattern;
        return date ? datePattern : "";
    }

    /** Bigger cards: the tile grows by {@code boost}, but never past what its column leaves for the focus zoom. */
    static int boostedTileWidthPx(int tileWidthPx, int innerWidthPx, int columns, float density, float boost) {
        if (boost <= 1f || columns <= 0 || innerWidthPx <= 0) return tileWidthPx;
        // 8dp instead of the usual 12dp between zoomed neighbours: the room bigger cards grow into.
        int fit = (int) ((innerWidthPx / columns - 8 * density) / AppAdapter.FOCUS_SCALE);
        return Math.max(tileWidthPx, Math.min(Math.round(tileWidthPx * boost), fit));
    }

    /** The app's colour as a deep, muted tone for a Night tile, dark enough that NIGHT_NAME reads on it (4.5:1). */
    static int nightTone(int argb) {
        float[] hsl = hsl(argb);
        float s = Math.min(hsl[1], 0.5f);
        float l = 0.22f;
        int tone = fromHsl(hsl[0], s, l);
        while (contrast(tone, NIGHT_NAME) < 4.5 && l > 0.04f) {
            l -= 0.02f;
            tone = fromHsl(hsl[0], s, l);
        }
        return tone;
    }

    static double luminance(int argb) {
        return 0.2126 * channel((argb >> 16) & 0xFF) + 0.7152 * channel((argb >> 8) & 0xFF) + 0.0722 * channel(argb & 0xFF);
    }

    private static double channel(int c) {
        double v = c / 255.0;
        return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
    }

    static double contrast(int a, int b) {
        double la = luminance(a), lb = luminance(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    private static float[] hsl(int argb) {
        float r = ((argb >> 16) & 0xFF) / 255f, g = ((argb >> 8) & 0xFF) / 255f, b = (argb & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        float l = (max + min) / 2f, h = 0f, s = 0f;
        float d = max - min;
        if (d > 0f) {
            s = d / (1f - Math.abs(2f * l - 1f));
            if (max == r) h = ((g - b) / d) % 6f;
            else if (max == g) h = (b - r) / d + 2f;
            else h = (r - g) / d + 4f;
            h *= 60f;
            if (h < 0) h += 360f;
        }
        return new float[]{h, s, l};
    }

    private static int fromHsl(float h, float s, float l) {
        float c = (1f - Math.abs(2f * l - 1f)) * s;
        float x = c * (1f - Math.abs((h / 60f) % 2f - 1f));
        float m = l - c / 2f;
        float r, g, b;
        if (h < 60) { r = c; g = x; b = 0; }
        else if (h < 120) { r = x; g = c; b = 0; }
        else if (h < 180) { r = 0; g = c; b = x; }
        else if (h < 240) { r = 0; g = x; b = c; }
        else if (h < 300) { r = x; g = 0; b = c; }
        else { r = c; g = 0; b = x; }
        return 0xFF000000 | Math.round((r + m) * 255) << 16 | Math.round((g + m) * 255) << 8 | Math.round((b + m) * 255);
    }
}
