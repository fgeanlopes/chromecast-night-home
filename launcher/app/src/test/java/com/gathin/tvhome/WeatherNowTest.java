package com.gathin.tvhome;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class WeatherNowTest {
    private static final String RESPONSE = "{\"latitude\":-23.55,\"longitude\":-46.63,\"current_units\":{\"time\":\"iso8601\","
            + "\"temperature_2m\":\"°C\"},\"current\":{\"time\":\"2026-10-23T05:45\",\"interval\":900,"
            + "\"temperature_2m\":17.6,\"weather_code\":3,\"is_day\":0}}";

    // deve ler temperatura, código do tempo e dia/noite da resposta da Open-Meteo
    @Test public void readsTheCurrentBlockOfAnOpenMeteoResponse() {
        WeatherNow w = WeatherNow.parse(RESPONSE, 1000L);
        assertEquals(18, w.temperature);
        assertEquals(3, w.code);
        assertFalse(w.day);
        assertEquals(1000L, w.fetchedAt);
    }

    // deve arredondar temperaturas negativas e decimais
    @Test public void roundsNegativeAndDecimalTemperatures() {
        assertEquals(-2, WeatherNow.parse(RESPONSE.replace("17.6", "-2.4"), 0).temperature);
        assertEquals(31, WeatherNow.parse(RESPONSE.replace("17.6", "30.5"), 0).temperature);
    }

    // deve ignorar resposta vazia, de erro ou sem o bloco atual
    @Test public void ignoresAnEmptyErrorOrIncompleteResponse() {
        assertNull(WeatherNow.parse(null, 0));
        assertNull(WeatherNow.parse("", 0));
        assertNull(WeatherNow.parse("{\"error\":true,\"reason\":\"Invalid\"}", 0));
        assertNull(WeatherNow.parse(RESPONSE.replace("\"weather_code\":3,", ""), 0));
    }

    // deve considerar o dado válido por até 2 horas
    @Test public void aReadingStaysFreshForTwoHours() {
        WeatherNow w = WeatherNow.parse(RESPONSE, 0);
        assertTrue(w.fresh(2 * 60 * 60 * 1000L));
        assertFalse(w.fresh(2 * 60 * 60 * 1000L + 1));
        assertFalse(w.fresh(-1));
    }

    // deve escolher o ícone pela condição e pelo período do dia
    @Test public void picksTheIconFromTheCodeAndTheTimeOfDay() {
        assertEquals(WeatherNow.Icon.SUN, WeatherNow.icon(0, true));
        assertEquals(WeatherNow.Icon.MOON, WeatherNow.icon(0, false));
        assertEquals(WeatherNow.Icon.CLOUD_SUN, WeatherNow.icon(2, true));
        assertEquals(WeatherNow.Icon.CLOUD_MOON, WeatherNow.icon(1, false));
        assertEquals(WeatherNow.Icon.CLOUD, WeatherNow.icon(3, true));
        assertEquals(WeatherNow.Icon.FOG, WeatherNow.icon(45, true));
        assertEquals(WeatherNow.Icon.RAIN, WeatherNow.icon(61, false));
        assertEquals(WeatherNow.Icon.RAIN, WeatherNow.icon(81, true));
        assertEquals(WeatherNow.Icon.STORM, WeatherNow.icon(95, true));
        assertEquals(WeatherNow.Icon.CLOUD, WeatherNow.icon(73, true));
        assertEquals(WeatherNow.Icon.CLOUD, WeatherNow.icon(999, true));
    }

    // deve tratar garoa, chuva, pancadas e tempestade como chuva
    @Test public void drizzleRainShowersAndStormsCountAsRain() {
        for (int code : new int[]{51, 55, 61, 65, 67, 80, 82, 95, 99}) assertTrue(String.valueOf(code), WeatherNow.raining(code));
        for (int code : new int[]{0, 1, 3, 45, 71, 86}) assertFalse(String.valueOf(code), WeatherNow.raining(code));
    }

    // deve escolher a faixa da frase pelos limites 13, 20 e 28 graus, com chuva tendo prioridade
    @Test public void phraseBandsFollowTheSpecLimits() {
        assertEquals(WeatherNow.Band.COLD, WeatherNow.band(12, false));
        assertEquals(WeatherNow.Band.COOL, WeatherNow.band(13, false));
        assertEquals(WeatherNow.Band.COOL, WeatherNow.band(19, false));
        assertEquals(WeatherNow.Band.PLEASANT, WeatherNow.band(20, false));
        assertEquals(WeatherNow.Band.PLEASANT, WeatherNow.band(27, false));
        assertEquals(WeatherNow.Band.HOT, WeatherNow.band(28, false));
        assertEquals(WeatherNow.Band.RAIN, WeatherNow.band(31, true));
    }

    // deve pedir só os três campos atuais, com a posição arredondada a ~1 km
    @Test public void theRequestAsksOnlyForTheCurrentFieldsAtARoundedPosition() {
        String url = WeatherNow.requestUrl(-23.550520, -46.633308);
        assertEquals("https://api.open-meteo.com/v1/forecast?latitude=-23.55&longitude=-46.63"
                + "&current=temperature_2m,weather_code,is_day", url);
    }
}
