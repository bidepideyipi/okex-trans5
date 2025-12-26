package com.supermancell.server.websocket;

import com.supermancell.common.model.Candle;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class OkexMessageParserTest {

    private final OkexMessageParser parser = new OkexMessageParser();

    @Test
    void shouldParseValidCandleMessageArrayFormat() {
        String message = "{\n" +
                "  \"arg\": {\n" +
                "    \"channel\": \"candle1m\",\n" +
                "    \"instId\": \"BTC-USDT-SWAP\"\n" +
                "  },\n" +
                "  \"data\": [\n" +
                "    [\n" +
                "      \"1703505600000\",\n" +
                "      \"42000.5\",\n" +
                "      \"42100.8\",\n" +
                "      \"41950.2\",\n" +
                "      \"42050.3\",\n" +
                "      \"1250.8\",\n" +
                "      \"1250.8\",\n" +
                "      \"52650400\",\n" +
                "      \"1\"\n" +
                "    ]\n" +
                "  ]\n" +
                "}";

        Candle candle = parser.parseCandle(message);

        Assertions.assertNotNull(candle);
        Assertions.assertEquals("BTC-USDT-SWAP", candle.getSymbol());
        Assertions.assertEquals("1m", candle.getInterval());
        Assertions.assertEquals(42000.5, candle.getOpen(), 0.01);
        Assertions.assertEquals(42100.8, candle.getHigh(), 0.01);
        Assertions.assertEquals(41950.2, candle.getLow(), 0.01);
        Assertions.assertEquals(42050.3, candle.getClose(), 0.01);
        Assertions.assertEquals(1250.8, candle.getVolume(), 0.01);
        Assertions.assertEquals("1", candle.getConfirm());
    }

    @Test
    void shouldParseOnlyFirstCandleInData() {
        // OKEx always sends only one candle, but even if multiple are present, only parse the first
        String message = "{\n" +
                "  \"arg\": {\n" +
                "    \"channel\": \"candle1H\",\n" +
                "    \"instId\": \"ETH-USDT-SWAP\"\n" +
                "  },\n" +
                "  \"data\": [\n" +
                "    [\"1703505600000\", \"2000\", \"2100\", \"1950\", \"2050\", \"500\", \"500\", \"1025000\", \"1\"],\n" +
                "    [\"1703509200000\", \"2050\", \"2150\", \"2000\", \"2100\", \"600\", \"600\", \"1260000\", \"1\"]\n" +
                "  ]\n" +
                "}";

        Candle candle = parser.parseCandle(message);

        Assertions.assertNotNull(candle);
        Assertions.assertEquals("ETH-USDT-SWAP", candle.getSymbol());
        Assertions.assertEquals("1H", candle.getInterval());
        Assertions.assertEquals(2000.0, candle.getOpen(), 0.01);
        Assertions.assertEquals("1", candle.getConfirm());
    }

    @Test
    void shouldReturnNullForNonCandleMessage() {
        String message = "{\"event\":\"subscribe\",\"arg\":{\"channel\":\"candle1m\",\"instId\":\"BTC-USDT-SWAP\"}}";

        Candle candle = parser.parseCandle(message);

        Assertions.assertNull(candle);
    }

    @Test
    void shouldReturnNullForInvalidJson() {
        String message = "invalid json{{{";

        Candle candle = parser.parseCandle(message);

        Assertions.assertNull(candle);
    }

    @Test
    void shouldExtractCorrectInterval() {
        // Test with array format
        String message1m = "{\"arg\":{\"channel\":\"candle1m\",\"instId\":\"BTC-USDT-SWAP\"},\"data\":[[\"1703505600000\",\"42000\",\"42100\",\"41950\",\"42050\",\"1250\",\"1250\",\"52650000\",\"1\"]]}";
        String message1H = "{\"arg\":{\"channel\":\"candle1H\",\"instId\":\"BTC-USDT-SWAP\"},\"data\":[[\"1703505600000\",\"42000\",\"42100\",\"41950\",\"42050\",\"1250\",\"1250\",\"52650000\",\"1\"]]}";

        Candle candle1m = parser.parseCandle(message1m);
        Candle candle1H = parser.parseCandle(message1H);

        Assertions.assertEquals("1m", candle1m.getInterval());
        Assertions.assertEquals("1H", candle1H.getInterval());
    }
}
