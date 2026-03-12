package ch.lueem.tradingbot.adapters.portfolio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import ch.lueem.tradingbot.core.portfolio.PortfolioSnapshot;
import org.junit.jupiter.api.Test;

class PaperPortfolioServiceTest {

    @Test
    void getSnapshot_returnsInitialFlatPortfolio() {
        PaperPortfolioService service = new PaperPortfolioService("BTCUSDT", new BigDecimal("1000.0000"));

        PortfolioSnapshot snapshot = service.getSnapshot("BTCUSDT");

        assertEquals("BTCUSDT", snapshot.symbol());
        assertEquals(new BigDecimal("1000.0000"), snapshot.availableCash());
        assertFalse(snapshot.position().open());
    }

    @Test
    void openAndClosePosition_updatesCashAndPositionState() {
        PaperPortfolioService service = new PaperPortfolioService("BTCUSDT", new BigDecimal("1000.0000"));

        service.openPosition(
                "BTCUSDT",
                new BigDecimal("0.01000000"),
                new BigDecimal("80000.0000"),
                OffsetDateTime.parse("2026-03-12T10:15:30Z"));
        PortfolioSnapshot openSnapshot = service.getSnapshot("BTCUSDT");

        assertTrue(openSnapshot.position().open());
        assertEquals(new BigDecimal("200.0000"), openSnapshot.availableCash());
        assertEquals(new BigDecimal("0.01000000"), openSnapshot.position().quantity());
        assertEquals(new BigDecimal("80000.0000"), openSnapshot.position().entryPrice());

        service.closePosition("BTCUSDT", new BigDecimal("81000.0000"));
        PortfolioSnapshot closedSnapshot = service.getSnapshot("BTCUSDT");

        assertFalse(closedSnapshot.position().open());
        assertEquals(new BigDecimal("1010.0000"), closedSnapshot.availableCash());
    }
}
