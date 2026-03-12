# trading-bot

Kleines Java-21-Maven-Projekt fuer Backtesting und einen technischen Paper-Testnet-Pfad.

## Aktueller Stand

- V1 fuer Phase 1/Phase 2: Backtesting plus technischer `PAPER`-Integrationspfad
- CSV-basierter Datenimport fuer OHLCV-Bars
- EMA-Cross-Strategie
- JSON ist das feste Ergebnisformat
- Logback fuer technische Laufzeit-Logs
- Anwendungsmodi sind `BACKTEST | PAPER`

Nicht Teil dieser V1:

- REST/API
- Health-Checks
- Multi-Bot-Betrieb

## Voraussetzungen

- JDK 21
- Maven 3.9+

## Architektur

- `App`: Einstiegspunkt
- `application`: Konfigurationsladen, Launcher, Paper-Bootstrap und Use-Case-Orchestrierung
- `backtest`: CSV-Loading, Strategieausfuehrung und Kennzahlen
- `bot`: Single-Bot-Runtime, Status, letzter Lauf und Tick-Ergebnisse
- `execution`: mode-neutrale Ausfuehrungsabstraktionen
- `integration`: modusspezifische Infrastruktur fuer Backtest-nahe und Exchange-nahe Pfade
- `portfolio`: mode-neutrale Positions- und Portfolio-Modelle
- `reporting`: JSON-Rendering des `BacktestReport`
- `strategy`: Strategieaufbau fuer ta4j
- modusspezifische Adapter liegen bewusst unter `integration/*`

## Ausfuehrungsmodi

- `BACKTEST`: historische Simulation gegen CSV- oder andere historische Datensaetze
- `PAPER`: live oder nahe Echtzeit gegen Demo-/Testumgebung ohne echtes Kapital
- `LIVE`: Ausfuehrung gegen die produktive Exchange-API mit echtem Kapital
- Deployment-Ort wie lokal, Server oder Container ist davon getrennt und kein Trading-Modus

## Konfiguration

Die Standardkonfiguration liegt in [application.yml](c:/dev/trading/apps/trading-bot/src/main/resources/application.yml).

Aktuelle Bereiche:

- `mode`
- `backtest`
- `paper.bot`
- `paper.execution`
- `paper.actionSource`
- `paper.binance`
- `reporting`
- `logging`

Beispiel:

```yaml
mode: BACKTEST

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

paper:
  bot:
    botId: btcusdt-paper-testnet
    botVersion: v1
    symbol: BTCUSDT
    timeframe: 1m
  execution:
    exchange: BINANCE_SPOT_TESTNET
    orderMode: VALIDATE_ONLY
    tickIntervalMillis: 10000
    initialCash: 1000.0
    orderQuantity: 0.0001
  actionSource:
    strategyName: queued_actions
    actions:
      - BUY
      - HOLD
      - SELL
  binance:
    apiKeyEnv: BINANCE_TESTNET_API_KEY
    secretKeyEnv: BINANCE_TESTNET_SECRET_KEY
    recvWindowMillis: 15000

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
- `PAPER` in Phase 1 nutzt Binance Spot Testnet REST, sendet signierte `orderTest`-Requests und fuehrt keine lokalen Positionsaenderungen aus
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
