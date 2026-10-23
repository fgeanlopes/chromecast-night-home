package com.gathin.tvhome;

import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * meu-chromecast Night: the gear and the menu rows lose their solid white focus. A translucent film and light grey
 * text mark the focused item instead, so nothing glares in a dark room.
 */
final class LookMenus {
    private static final int FOCUS_FILM = 0x24FFFFFF, REST_FILM = 0x14FFFFFF, EDGE = 0x24FFFFFF;
    private static final int FOCUSED_TEXT = 0xFFE2E2E6, TEXT = 0xFFCFCFD4, DIM = 0x99CFCFD4;
    /** The gear inverts when focused (light grey with a dark icon), so it is obvious where focus is. */
    private static final int BUTTON_FOCUS = 0xFFD8D8DC, BUTTON_FOCUS_ICON = 0xFF111114;

    private LookMenus() {
    }

    /** A gear menu or panel row (item_input.xml). */
    static void styleRow(View row, float density) {
        row.setBackground(states(13 * density, FOCUS_FILM, 0, 0, density));
        ((TextView) row.findViewById(R.id.title)).setTextColor(textStates(FOCUSED_TEXT, TEXT));
        ((TextView) row.findViewById(R.id.status)).setTextColor(textStates(TEXT, DIM));
        ((ImageView) row.findViewById(R.id.icon)).setImageTintList(textStates(FOCUSED_TEXT, TEXT));
    }

    /** A top-bar pill (the gear). */
    static void styleButton(TextView button, float density) {
        button.setBackground(states(21 * density, BUTTON_FOCUS, REST_FILM, EDGE, density));
        button.setTextColor(textStates(BUTTON_FOCUS_ICON, TEXT));
        button.setCompoundDrawableTintList(textStates(BUTTON_FOCUS_ICON, TEXT));
    }

    private static StateListDrawable states(float radius, int focused, int rest, int edge, float density) {
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_focused}, shape(radius, focused, edge, density));
        s.addState(new int[]{}, shape(radius, rest, edge, density));
        return s;
    }

    private static GradientDrawable shape(float radius, int color, int edge, float density) {
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(radius);
        d.setColor(color);
        if (edge != 0) d.setStroke(Math.max(1, Math.round(density)), edge);
        return d;
    }

    private static ColorStateList textStates(int focused, int rest) {
        return new ColorStateList(new int[][]{{android.R.attr.state_focused}, {}}, new int[]{focused, rest});
    }
}
