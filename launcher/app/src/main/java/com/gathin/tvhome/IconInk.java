package com.gathin.tvhome;

/**
 * meu-chromecast Night/Glass: an app icon reduced to its drawing in one colour, without the tile-shaped square many
 * icons sit on. Works on raw ARGB pixels (plain Java, unit-tested); LookTiles renders and caches the result.
 */
final class IconInk {
    private IconInk() {
    }

    /**
     * The icon's solid background colour, read on a ring just inside its edges, or 0 when that ring is mostly
     * transparent or not one colour (then nothing is removed).
     */
    static int background(int[] argb, int w, int h) {
        int left = 0, top = 0, right = w - 1, bottom = h - 1;
        int[] points = ring(w, h);
        int n = points.length / 2;
        int[] samples = new int[n];
        for (int i = 0; i < n; i++) samples[i] = argb[points[2 * i + 1] * w + points[2 * i]];
        // The colour most of the ring agrees on: a logo may cross part of the ring (UniTV's big circle does).
        int best = 0, bestVotes = 0;
        for (int candidate : samples) {
            if ((candidate >>> 24) < 200) continue;
            int votes = 0;
            for (int other : samples) if ((other >>> 24) >= 200 && distance(other, candidate) <= 0.08) votes++;
            if (votes > bestVotes) {
                best = candidate;
                bestVotes = votes;
            }
        }
        if (bestVotes * 10 < n * 6) return 0;
        // A plain glyph with no backdrop fills its own bounds too: it is a background only if a drawing sits on it.
        int drawing = 0, area = 0;
        for (int y = top; y <= bottom; y++) {
            for (int x = left; x <= right; x++) {
                int p = argb[y * w + x];
                if ((p >>> 24) < 200) continue;
                area++;
                if (distance(p, best) > 0.18) drawing++;
            }
        }
        return drawing * 50 >= area ? 0xFF000000 | best : 0;
    }

    /** 0-255 per pixel: how much of it is drawing rather than background. */
    static int[] alpha(int[] argb, int w, int h) {
        int bg = background(argb, w, h);
        int[] out = new int[argb.length];
        for (int i = 0; i < argb.length; i++) {
            int p = argb[i];
            int a = p >>> 24;
            if (bg != 0) {
                double d = distance(p, bg);
                double ink = Math.max(0, Math.min(1, (d - 0.10) / 0.25));
                a = (int) Math.round(a * ink);
            }
            out[i] = a;
        }
        return out;
    }

    /** {left, top, right, bottom} of the drawing (alpha above a faint speck), inclusive; null when there is none. */
    static int[] bounds(int[] alpha, int w, int h) {
        int left = w, top = h, right = -1, bottom = -1;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (alpha[y * w + x] < 40) continue;
                left = Math.min(left, x);
                top = Math.min(top, y);
                right = Math.max(right, x);
                bottom = Math.max(bottom, y);
            }
        }
        return right < left ? null : new int[]{left, top, right, bottom};
    }

    /**
     * Where to draw a drawing with these bounds so its longer side is {@code fraction} of a {@code size} box, centred,
     * shape kept: every app's drawing then reads the same size, whatever margin its icon had.
     */
    static float[] fit(int[] bounds, int size, float fraction) {
        float w = bounds[2] - bounds[0] + 1, h = bounds[3] - bounds[1] + 1;
        float scale = size * fraction / Math.max(w, h);
        float dw = w * scale, dh = h * scale;
        float left = (size - dw) / 2f, top = (size - dh) / 2f;
        return new float[]{left, top, left + dw, top + dh};
    }

    static int[] paint(int[] alpha, int rgb) {
        int[] out = new int[alpha.length];
        for (int i = 0; i < alpha.length; i++) out[i] = alpha[i] << 24 | (rgb & 0xFFFFFF);
        return out;
    }

    /** RGB distance, 0 (same) to 1 (black against white). */
    private static double distance(int a, int b) {
        int dr = ((a >> 16) & 0xFF) - ((b >> 16) & 0xFF);
        int dg = ((a >> 8) & 0xFF) - ((b >> 8) & 0xFF);
        int db = (a & 0xFF) - (b & 0xFF);
        return Math.sqrt(dr * dr + dg * dg + db * db) / 441.67;
    }

    /** Close to the edge (6%): past rounded corners, before a logo that fills most of the icon (UniTV's does). */
    private static int[] ring(int w, int h) {
        int x0 = Math.round(w * 0.06f), y0 = Math.round(h * 0.06f), x1 = w - 1 - x0, y1 = h - 1 - y0;
        int steps = 8;
        int[] pts = new int[steps * 4 * 2];
        int k = 0;
        for (int i = 0; i < steps; i++) {
            int x = x0 + (x1 - x0) * i / steps, y = y0 + (y1 - y0) * i / steps;
            pts[k++] = x; pts[k++] = y0;
            pts[k++] = x; pts[k++] = y1;
            pts[k++] = x0; pts[k++] = y;
            pts[k++] = x1; pts[k++] = y;
        }
        return pts;
    }
}
