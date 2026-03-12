# trading-bot

Kleines Java-21-Maven-Projekt fuer lokales Backtesting mit `ta4j`.

## Aktueller Stand

- V1 fuer Phase 1: nur lokales Backtesting
- CSV-basierter Datenimport fuer OHLCV-Bars
- EMA-Cross-Strategie
- JSON ist das feste Ergebnisformat
- Logback fuer technische Laufzeit-Logs
- Ausfuehrungsmodi sind systemweit als `BACKTEST | PAPER | LIVE` definiert

Nicht Teil dieser V1:

- Live-Trading
- Scheduling
- REST/API
- Health-Checks
- Multi-Bot-Betrieb

## Voraussetzungen

- JDK 21
- Maven 3.9+

## Architektur

- `App`: Einstiegspunkt
- `application`: Konfigurationsladen und Use-Case-Orchestrierung
- `backtest`: CSV-Loading, Strategieausfuehrung und Kennzahlen
- `bot`: Single-Bot-Runtime, Status, letzter Lauf und Tick-Ergebnisse
- `execution`: Ausfuehrungsabstraktionen fuer Backtest, Paper und spaeter Live
- `portfolio`: Positions- und Portfoliostand fuer laufende Bots
- `reporting`: JSON-Rendering des `BacktestReport`
- `strategy`: Strategieaufbau fuer ta4j
- bevorzugt werden fachliche Klassennamen wie `PaperExecutionService`, `PaperPortfolioService`, `SequenceMarketSnapshotProvider`

## Ausfuehrungsmodi

- `BACKTEST`: historische Simulation gegen CSV- oder andere historische Datensaetze
- `PAPER`: live oder nahe Echtzeit gegen Demo-/Testumgebung ohne echtes Kapital
- `LIVE`: Ausfuehrung gegen die produktive Exchange-API mit echtem Kapital
- Deployment-Ort wie lokal, Server oder Container ist davon getrennt und kein Trading-Modus

## Konfiguration

Die Standardkonfiguration liegt in [application.yml](c:/dev/trading/apps/trading-bot/src/main/resources/application.yml).

Aktuelle Bereiche:

- `backtest`
- `reporting`
- `logging`

Beispiel:

```yaml
backtest:
  csvPath: data/historical/BTCUSDT-1h.csv
  symbol: BTCUSDT
  timeframe: 1h
  strategy:
    name: ema_cross
    parameters:
      shortEma: 3
      longEma: 7
  portfolio:
    initialCash: 10000.0

reporting:
  prettyPrint: true
  includeNotes: false

logging:
  lifecycleEvents: true
```

## CSV-Format

Erwarteter Header:

```text
timestamp,open,high,low,close,volume
```

Beispielpfad:

```text
data/historical/BTCUSDT-1h.csv
```

## Ausgabe

- Technische Logs laufen ueber Logback
- Backtest-Ergebnisse werden als JSON auf `stdout` ausgegeben
- Logging und Reporting sind bewusst getrennt
- JSON ist fuer diese V1 der feste standard
- Das aktuelle JSON-Schema wird als `reportVersion: "v3"` ausgegeben
- Backtest-Reports enthalten `metadata.mode: "BACKTEST"` als explizite Ausfuehrungsrealitaet
- Geld- und Prozentwerte werden als numerische JSON-Werte mit 4 Dezimalstellen ausgegeben
- Zaehler bleiben Integer, Statuswerte bleiben Boolean
- Positionsdetails werden immer mit ausgegeben; offene Positionen haben `exitTime` und `exitPrice` auf `null`

Wichtige Report-Felder:
- `barCount`, `executedSignalCount`, `closedTradeCount`: Integer
- `hasOpenPosition`: Boolean
- `entryPrice`, `exitPrice`, `quantity`, `profitLoss`, `profitLossPercent`: Positionsdetails pro Position
- `initialCash`, `finalValue`, `totalReturnPercent`, `winRatePercent`: numerische Werte mit 4 Dezimalstellen

## Starten

```bash
mvn clean compile
mvn exec:java
```
