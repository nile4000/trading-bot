package ch.lueem.tradingbot.integration.paper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.math.BigDecimal;

import ch.lueem.tradingbot.portfolio.PortfolioSnapshot;
import org.junit.jupiter.api.Test;

class StaticPortfolioServiceTest {

    @Test
    void getSnapshot_returnsStableFlatPortfolio() {
        StaticPortfolioService service = new StaticPortfolioService("BTCUSDT", new BigDecimal("1000.0000"));

        PortfolioSnapshot first = service.getSnapshot("BTCUSDT");
        PortfolioSnapshot second = service.getSnapshot("BTCUSDT");

        assertSame(first, second);
        assertEquals("BTCUSDT", first.symbol());
        assertEquals(new BigDecimal("1000.0000"), first.availableCash());
        assertEquals(false, first.position().open());
    }
}
