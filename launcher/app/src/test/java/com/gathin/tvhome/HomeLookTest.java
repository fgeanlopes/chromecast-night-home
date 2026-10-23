package com.gathin.tvhome;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HomeLookTest {
    private static HomeLook look(Object... keyValues) {
        Map<String, Object> saved = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) saved.put((String) keyValues[i], keyValues[i + 1]);
        return HomeLook.from(saved);
    }

    // deve usar os padrões da especificação numa instalação nova
    @Test public void aFreshInstallUsesTheSpecDefaults() {
        HomeLook l = look();
        assertEquals(HomeLook.Cards.CLASSIC, l.cards);
        assertEquals(HomeLook.Background.SKY, l.background);
        assertTrue(l.greeting);
        assertFalse(l.bigClock);
        assertTrue(l.smallClock);
        assertTrue(l.date);
        assertFalse(l.biggerCards);
        assertFalse(l.headerTemperature);
        assertTrue(l.weatherLine);
    }

    // deve ler os interruptores de temperatura sem alterar os outros
    @Test public void temperatureSwitchesAreIndependent() {
        HomeLook l = look(HomeLook.KEY_HEADER_TEMPERATURE, true, HomeLook.KEY_WEATHER_LINE, false);
        assertTrue(l.headerTemperature);
        assertFalse(l.weatherLine);
        assertTrue(l.greeting);
        HomeLook after = l.with(HomeLook.KEY_WEATHER_LINE, true);
        assertTrue(after.weatherLine);
        assertTrue(after.headerTemperature);
    }

    // deve ler cada escolha salva de forma independente
    @Test public void readsEverySavedChoiceIndependently() {
        HomeLook l = look(HomeLook.KEY_CARDS, "night", HomeLook.KEY_BACKGROUND, "midnight",
                HomeLook.KEY_GREETING, false, HomeLook.KEY_BIG_CLOCK, true, HomeLook.KEY_SMALL_CLOCK, false,
                HomeLook.KEY_DATE, false, HomeLook.KEY_BIGGER_CARDS, true);
        assertEquals(HomeLook.Cards.NIGHT, l.cards);
        assertEquals(HomeLook.Background.MIDNIGHT, l.background);
        assertFalse(l.greeting);
        assertTrue(l.bigClock);
        assertFalse(l.smallClock);
        assertFalse(l.date);
        assertTrue(l.biggerCards);
    }

    // deve voltar ao padrão quando o valor salvo é desconhecido ou de outro tipo
    @Test public void anUnknownOrBadlyTypedValueFallsBackToTheDefault() {
        HomeLook l = look(HomeLook.KEY_CARDS, "neon", HomeLook.KEY_BACKGROUND, 3, HomeLook.KEY_GREETING, "yes");
        assertEquals(HomeLook.Cards.CLASSIC, l.cards);
        assertEquals(HomeLook.Background.SKY, l.background);
        assertTrue(l.greeting);
    }

    // deve gravar e reler as escolhas sem perder nada
    @Test public void savedValuesRoundTrip() {
        HomeLook l = look(HomeLook.KEY_CARDS, "glass", HomeLook.KEY_BACKGROUND, "graphite", HomeLook.KEY_DATE, false);
        assertEquals(l.cards, HomeLook.from(l.toMap()).cards);
        assertEquals(l.background, HomeLook.from(l.toMap()).background);
        assertEquals(l.date, HomeLook.from(l.toMap()).date);
    }

    // deve trocar um único item sem alterar os outros
    @Test public void changingOneChoiceLeavesTheOthersAlone() {
        HomeLook before = look(HomeLook.KEY_CARDS, "night", HomeLook.KEY_BIG_CLOCK, true);
        HomeLook after = before.with(HomeLook.KEY_GREETING, false);
        assertFalse(after.greeting);
        assertEquals(HomeLook.Cards.NIGHT, after.cards);
        assertTrue(after.bigClock);
        assertTrue(after.smallClock);
    }

    // deve esconder o título grande do app fora do estilo Classic
    @Test public void onlyClassicShowsTheBigAppTitle() {
        assertTrue(look().showsHeroTitle(true));
        assertFalse(look().showsHeroTitle(false));
        assertFalse(look(HomeLook.KEY_CARDS, "night").showsHeroTitle(true));
        assertFalse(look(HomeLook.KEY_CARDS, "glass").showsHeroTitle(true));
    }

    // deve desligar o brilho de fundo no Noite e no fundo Grafite
    @Test public void nightAndGraphiteHaveNoAmbientGlow() {
        assertTrue(look().ambientGlow(true));
        assertFalse(look().ambientGlow(false));
        assertFalse(look(HomeLook.KEY_CARDS, "night").ambientGlow(true));
        assertFalse(look(HomeLook.KEY_BACKGROUND, "graphite").ambientGlow(true));
        assertTrue(look(HomeLook.KEY_BACKGROUND, "midnight").ambientGlow(true));
    }

    // deve usar brilho mais fraco no fundo Meia-noite
    @Test public void midnightGlowIsSofterThanSky() {
        assertTrue(HomeLook.Background.MIDNIGHT.glowStrength < HomeLook.Background.SKY.glowStrength);
        assertEquals(1f, HomeLook.Background.SKY.glowStrength, 0.001f);
    }

    // deve usar contorno cinza no Noite e no Vidro e manter o do launcher no Classic
    @Test public void nightAndGlassUseTheGreyRing() {
        assertEquals(0, look().focusRingColor());
        assertEquals(0xFFA8A8B0, look(HomeLook.KEY_CARDS, "night").focusRingColor());
        assertEquals(0xFFA8A8B0, look(HomeLook.KEY_CARDS, "glass").focusRingColor());
    }

    // deve calcular o contraste no padrão WCAG
    @Test public void contrastFollowsWcag() {
        assertEquals(21.0, HomeLook.contrast(0xFFFFFFFF, 0xFF000000), 0.01);
        assertEquals(1.0, HomeLook.contrast(0xFF777777, 0xFF777777), 0.01);
    }

    // deve gerar tons escuros em que o nome do app fica legível
    @Test public void nightToneKeepsTheNameReadable() {
        int[] vivid = {0xFFE53935, 0xFF4A6CF7, 0xFFF07A25, 0xFF7C4DFF, 0xFFFFFFFF, 0xFF000000, 0xFF20C997, 0xFFFFEB3B};
        for (int c : vivid) {
            int tone = HomeLook.nightTone(c);
            assertTrue(Integer.toHexString(c), HomeLook.contrast(tone, HomeLook.NIGHT_NAME) >= 4.5);
            assertTrue(Integer.toHexString(c), HomeLook.luminance(tone) < 0.08);
        }
    }

    // deve manter a cor dominante do app no tom escuro
    @Test public void nightToneKeepsTheHue() {
        int red = HomeLook.nightTone(0xFFE53935);
        assertTrue(((red >> 16) & 0xFF) > (red & 0xFF));
        int blue = HomeLook.nightTone(0xFF4A6CF7);
        assertTrue((blue & 0xFF) > ((blue >> 16) & 0xFF));
    }

    // deve montar a linha pequena só com os itens ligados e "·" entre eles
    @Test public void smallLineJoinsOnlyTheVisibleParts() {
        assertEquals("h:mm a '·' EEEE d MMMM", HomeLook.smallLinePattern("h:mm a", "EEEE d MMMM", true, true));
        assertEquals("h:mm a", HomeLook.smallLinePattern("h:mm a", "EEEE d MMMM", true, false));
        assertEquals("EEEE d MMMM", HomeLook.smallLinePattern("h:mm a", "EEEE d MMMM", false, true));
        assertEquals("", HomeLook.smallLinePattern("h:mm a", "EEEE d MMMM", false, false));
    }

    // deve aumentar os cards maiores sem passar do espaço da coluna
    @Test public void biggerCardsGrowButStayInsideTheCell() {
        float density = 2f;
        int normal = AppAdapter.tileWidthPx(1660, 4, density);
        int bigger = HomeLook.boostedTileWidthPx(normal, 1740, 4, density, 1.1f);
        assertTrue(bigger > normal);
        int cell = 1740 / 4;
        assertTrue(bigger * AppAdapter.FOCUS_SCALE <= cell - 8 * density + 1);
        assertEquals(normal, HomeLook.boostedTileWidthPx(normal, 1740, 4, density, 1f));
    }
}
