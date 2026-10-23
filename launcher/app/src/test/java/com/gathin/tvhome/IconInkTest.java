package com.gathin.tvhome;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class IconInkTest {
    private static final int N = 20;

    /** A square icon: {@code bg} everywhere, {@code ink} in the centre block, transparent outside {@code inset}. */
    private static int[] icon(int bg, int ink, int inset) {
        int[] px = new int[N * N];
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                boolean outside = x < inset || y < inset || x >= N - inset || y >= N - inset;
                boolean centre = x >= 7 && x < 13 && y >= 7 && y < 13;
                px[y * N + x] = outside ? 0 : centre ? ink : bg;
            }
        }
        return px;
    }

    private static int at(int[] a, int x, int y) {
        return a[y * N + x];
    }

    // deve manter só o desenho quando o fundo do ícone já é transparente
    @Test public void aTransparentBackgroundKeepsTheGlyphAsIs() {
        int[] alpha = IconInk.alpha(icon(0x00000000, 0xFFE53935, 0), N, N);
        assertEquals(255, at(alpha, 10, 10));
        assertEquals(0, at(alpha, 3, 3));
    }

    // deve remover o quadrado branco e manter o desenho escuro
    @Test public void removesAWhiteSquareAndKeepsADarkGlyph() {
        int[] alpha = IconInk.alpha(icon(0xFFFFFFFF, 0xFF202020, 1), N, N);
        assertEquals(255, at(alpha, 10, 10));
        assertEquals(0, at(alpha, 4, 4));
        assertEquals(0, at(alpha, 0, 0));
    }

    // deve remover o quadrado escuro e manter o desenho claro
    @Test public void removesADarkSquareAndKeepsALightGlyph() {
        int[] alpha = IconInk.alpha(icon(0xFF2A2A2A, 0xFFFFFFFF, 1), N, N);
        assertEquals(255, at(alpha, 10, 10));
        assertEquals(0, at(alpha, 4, 4));
    }

    // deve reconhecer o fundo sólido pela borda interna do ícone
    @Test public void findsASolidBackgroundOnTheInnerRing() {
        assertEquals(0xFFFFFFFF, IconInk.background(icon(0xFFFFFFFF, 0xFF000000, 1), N, N));
        assertEquals(0, IconInk.background(icon(0x00000000, 0xFF000000, 0), N, N));
    }

    // deve reconhecer o fundo mesmo quando o desenho encosta em parte da borda interna
    @Test public void findsTheBackgroundWhenTheGlyphTouchesPartOfTheRing() {
        int[] px = icon(0xFFFFFFFF, 0xFF3050C0, 1);
        for (int y = 2; y < 18; y++) px[y * N + 3] = 0xFFE8622C;   // a logo stroke across the left of the ring
        assertEquals(0xFFFFFFFF, IconInk.background(px, N, N));
        int[] alpha = IconInk.alpha(px, N, N);
        assertEquals(0, at(alpha, 15, 15));
        assertEquals(255, at(alpha, 3, 10));
    }

    // deve achar o fundo branco de um ícone cujo logo circular ocupa quase todo o quadrado (UniTV)
    @Test public void findsTheBackgroundBehindALogoThatFillsMostOfTheIcon() {
        int[] px = icon(0xFFFFFFFF, 0xFFFFFFFF, 0);
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                double r = Math.hypot(x - 9.5, y - 9.5);
                if (r >= 6 && r <= 8) px[y * N + x] = 0xFFE8622C;
            }
        }
        assertEquals(0xFFFFFFFF, IconInk.background(px, N, N));
        int[] alpha = IconInk.alpha(px, N, N);
        assertEquals(0, at(alpha, 0, 0));
        assertEquals(0, at(alpha, 10, 10));
        assertEquals(255, at(alpha, 17, 10));
    }

    // deve tratar fundo com várias cores como transparente e não apagar o desenho
    @Test public void aBusyBackgroundIsNotRemoved() {
        int[] px = icon(0xFFFFFFFF, 0xFF000000, 0);
        int[] stripes = {0xFF0000FF, 0xFFFF0000, 0xFF00C000, 0xFFFFFF00};
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) if (px[y * N + x] != 0xFF000000) px[y * N + x] = stripes[x / 5];
        }
        assertEquals(0, IconInk.background(px, N, N));
        int[] alpha = IconInk.alpha(px, N, N);
        assertTrue(at(alpha, 10, 10) > 0);
    }

    // deve achar a área que o desenho ocupa, ignorando a margem transparente
    @Test public void findsTheBoundsOfTheDrawing() {
        int[] alpha = new int[N * N];
        for (int y = 5; y < 12; y++) for (int x = 8; x < 11; x++) alpha[y * N + x] = 255;
        alpha[0] = 10;   // a faint speck does not count
        assertEquals(java.util.Arrays.toString(new int[]{8, 5, 10, 11}),
                java.util.Arrays.toString(IconInk.bounds(alpha, N, N)));
        assertEquals(null, IconInk.bounds(new int[N * N], N, N));
    }

    // deve encaixar desenhos de tamanhos diferentes na mesma caixa, centralizados e sem distorcer
    @Test public void fitsEveryDrawingToTheSameBoxKeepingItsShape() {
        float[] wide = IconInk.fit(new int[]{0, 0, 99, 49}, 144, 0.7f);
        float[] tall = IconInk.fit(new int[]{10, 10, 29, 69}, 144, 0.7f);
        // a maior dimensão vira 70% da caixa nos dois
        assertEquals(144 * 0.7f, wide[2] - wide[0], 0.5f);
        assertEquals(144 * 0.7f, tall[3] - tall[1], 0.5f);
        // proporção mantida
        assertEquals(2f, (wide[2] - wide[0]) / (wide[3] - wide[1]), 0.05f);
        // centralizado
        assertEquals(72f, (tall[0] + tall[2]) / 2, 0.5f);
        assertEquals(72f, (wide[1] + wide[3]) / 2, 0.5f);
    }

    // deve gerar o desenho numa cor só, com a transparência da máscara
    @Test public void paintsTheMaskInOneColour() {
        int[] out = IconInk.paint(new int[]{255, 0, 128}, 0xFFC8C8CC);
        assertEquals(0xFFC8C8CC, out[0]);
        assertEquals(0x00C8C8CC, out[1]);
        assertEquals(0x80C8C8CC, out[2]);
    }
}
