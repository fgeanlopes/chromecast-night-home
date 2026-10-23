package com.gathin.tvhome;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import java.util.HashMap;
import java.util.Map;

/**
 * Night and Glass tiles (meu-chromecast): the app's icon in grey and its name inside the card, on a dark tone of
 * the app's colour (Night) or on neutral glass that takes that tone only while focused (Glass).
 */
final class LookTiles {
    /** Glass tile at rest: a faint light film over the background, with a hairline edge. */
    private static final int GLASS_REST = 0x14FFFFFF, GLASS_EDGE = 0x24FFFFFF;
    /** The drawing of each app's icon in one colour (IconInk), per package and colour: built once per Home. */
    private static final Map<String, Drawable> INK = new HashMap<>();
    private static final int INK_SIZE = 144, NIGHT_INK = 0xFFC8C8CC, GLASS_INK = 0xFFE6E6EA;
    /** Share of the icon box the drawing's longer side fills, once cropped (IconInk.fit). */
    private static final float INK_FILL = 0.78f;

    private LookTiles() {
    }

    static void bind(AppAdapter.Holder h, AppAdapter.Entry e, int appColor, HomeLook look, float density) {
        int tone = HomeLook.nightTone(appColor);
        boolean glass = look.cards == HomeLook.Cards.GLASS;
        GradientDrawable fill = new GradientDrawable();
        ViewGroup.LayoutParams lp = h.card.getLayoutParams();
        fill.setCornerRadius(AppAdapter.cornerRadius(lp.width, density));
        if (glass) fill.setStroke(Math.max(1, Math.round(density)), GLASS_EDGE);
        h.lookRest = glass ? GLASS_REST : tone;
        h.lookFocus = tone;
        fill.setColor(h.card.isFocused() ? h.lookFocus : h.lookRest);
        h.lookFill = fill;
        h.card.setBackground(fill);

        h.banner.setImageDrawable(null);
        h.banner.setVisibility(View.GONE);
        int inkColor = glass ? GLASS_INK : NIGHT_INK;
        h.icon.setImageDrawable(ink(h.icon.getResources(), e.pkg + inkColor, e.icon != null ? e.icon : e.banner, inkColor));
        h.icon.setVisibility(View.VISIBLE);
        h.label.setText(e.label);
        h.label.setTextColor(glass ? 0xFFFFFFFF : HomeLook.NIGHT_NAME);
        h.label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
        h.label.setVisibility(View.VISIBLE);
        // The name is inside the card, so the one under it would repeat it.
        h.name.setVisibility(View.INVISIBLE);
    }

    /**
     * The app's own monochrome icon when it ships one (Android 13+), else its foreground layer, else the whole icon,
     * with any solid square behind the drawing taken out (IconInk) and the rest painted in one colour.
     */
    private static Drawable ink(Resources res, String key, Drawable icon, int color) {
        Drawable cached = INK.get(key);
        if (cached != null || icon == null) return cached;
        Drawable source = icon;
        if (icon instanceof AdaptiveIconDrawable) {
            AdaptiveIconDrawable adaptive = (AdaptiveIconDrawable) icon;
            Drawable mono = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ? adaptive.getMonochrome() : null;
            if (mono != null) source = mono;
            else if (adaptive.getForeground() != null) source = adaptive.getForeground();
        }
        Drawable copy = source.getConstantState() != null ? source.getConstantState().newDrawable(res).mutate() : source;
        Bitmap bmp = Bitmap.createBitmap(INK_SIZE, INK_SIZE, Bitmap.Config.ARGB_8888);
        copy.setBounds(0, 0, INK_SIZE, INK_SIZE);
        copy.draw(new Canvas(bmp));
        int[] px = new int[INK_SIZE * INK_SIZE];
        bmp.getPixels(px, 0, INK_SIZE, 0, 0, INK_SIZE, INK_SIZE);
        int[] alpha = IconInk.alpha(px, INK_SIZE, INK_SIZE);
        bmp.setPixels(IconInk.paint(alpha, color), 0, INK_SIZE, 0, 0, INK_SIZE, INK_SIZE);
        // Same visual size for every app: crop to the drawing, then fit it to one box (icons ship different margins).
        int[] b = IconInk.bounds(alpha, INK_SIZE, INK_SIZE);
        if (b != null) {
            float[] dst = IconInk.fit(b, INK_SIZE, INK_FILL);
            Bitmap fitted = Bitmap.createBitmap(INK_SIZE, INK_SIZE, Bitmap.Config.ARGB_8888);
            new Canvas(fitted).drawBitmap(bmp, new android.graphics.Rect(b[0], b[1], b[2] + 1, b[3] + 1),
                    new android.graphics.RectF(dst[0], dst[1], dst[2], dst[3]), new android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG));
            bmp = fitted;
        }
        Drawable ink = new BitmapDrawable(res, bmp);
        INK.put(key, ink);
        return ink;
    }
}
