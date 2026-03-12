# trading-bot

Kleines Java-21-Maven-Projekt fuer Backtesting und einen technischen Paper-Testnet-Pfad.

## Aktueller Stand

- V1: Backtesting plus technischer `PAPER`-Pfad
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

- `app`: schlanker Einstiegspunkt und Modus-Dispatch
- `core`: modusunabhaengige Trading-Logik und Ports
- `modes.backtest`: historischer Ablauf, Runtime-Ausfuehrung und fachlicher Backtest-Report
- `modes.paper`: technischer Paper-Flow, Runtime-Wiring und Runner-Loop
- `adapters.config`: YAML-Loading und typed Config-Records
- `adapters.market`: CSV- und Marktpreis-Adapter
- `adapters.execution`: simulierte und Exchange-nahe Ausfuehrung
- `adapters.portfolio`: konkrete Portfolio-Implementierungen
- `adapters.reporting`: JSON-Rendering des `BacktestReport`

ta4j-Entscheidung:

- `ta4j` wird fuer Strategie- und Indikatorlogik im `BACKTEST` genutzt.
- Reporting bleibt vorerst im eigenen Backtest-Modell.
- Grund: Das Reportformat ist app-spezifisch und basiert auf den eigenen Simulationsannahmen wie `action_bar_close`, `all_in_spot` und Mark-to-Market fuer offene Positionen.
- Ein ta4j-basierter Reporting-Pfad wird erst relevant, wenn mehrere Strategien systematisch verglichen oder weitere Standardmetriken benoetigt werden.

Kurzmodell:

- `core` kennt keine `modes` oder `adapters`
- `modes` orchestrieren Use-Cases
- `adapters` sprechen Datei, YAML, JSON oder externe APIs

Aktuelle Hauptverzeichnisse:

```text
src/main/java/ch/lueem/tradingbot
|- app
|- core
|  |- execution
|  |- portfolio
|  |- runtime
|  `- strategy
|- modes
|  |- backtest
|  `- paper
`- adapters
   |- config
   |- execution
   |- market
   |- portfolio
   `- reporting
```

## Guard-Regeln

- Guards bleiben an echten Aussenraendern: YAML-Loading, CSV-Parsing, Binance-Adapter, JSON-Rendering.
- Guards bleiben fuer fachliche Invarianten: ungueltige Config, Symbol-/Timeframe-Mismatch, ungueltige Portfolio-Zustaende.
- Guards in internen Orchestrierungs- und Wiring-Klassen werden sparsam gehalten.
- Konstruktor-Nullchecks in `app` und `modes` sind optional und werden nicht reflexartig eingebaut.
- Wenn ein Fehler bereits durch vorgelagerte Config- oder Adapter-Validierung klar abgefangen wird, wird derselbe Guard nicht nochmal in jeder internen Methode wiederholt.

## Ausfuehrungsmodi

- `BACKTEST`: historische Simulation gegen CSV- oder andere historische Datensaetze
- `PAPER`: live oder nahe Echtzeit gegen Demo-/Testumgebung ohne echtes Kapital
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
- `PAPER` nutzt Binance Spot Testnet REST, validiert `orderTest`-Requests und spiegelt validierte Orders in das lokale Paper-Portfolio
- Das aktuelle JSON-Schema wird als `reportVersion: "v3"` ausgegeben
- Backtest-Reports enthalten `metadata.mode: "BACKTEST"` als explizite Ausfuehrungsrealitaet
- Geld- und Prozentwerte werden als numerische JSON-Werte mit 4 Dezimalstellen ausgegeben
- Zaehler bleiben Integer, Statuswerte bleiben Boolean
- Positionsdetails werden immer mit ausgegeben; offene Positionen haben `exitTime` und `exitPrice` auf `null`
- `action_bar_close` bedeutet, dass die Ausfuehrung auf dem Close der Action-Bar simuliert wird

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
