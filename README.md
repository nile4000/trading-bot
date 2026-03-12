# trading-bot

Kleines Java-21-Maven-Projekt fuer lokales Backtesting mit `ta4j`.

## Aktueller Stand

- Phase 1: nur lokales Backtesting
- CSV-basierter Datenimport fuer OHLCV-Bars
- EMA-Cross-Strategie
- JSON-Report als Ergebnisformat
- Logback fuer technische Laufzeit-Logs

## Voraussetzungen

- JDK 21
- Maven 3.9+

## Architektur

- `App`: duennter Einstiegspunkt
- `application`: Konfigurationsladen und Use-Case-Orchestrierung
- `backtest`: CSV-Loading, Strategieausfuehrung und Kennzahlen
- `reporting`: JSON-Rendering des `BacktestReport`
- `strategy`: Strategieaufbau fuer ta4j

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
    shortEma: 9
    longEma: 21
  portfolio:
    initialCash: 10000.0

reporting:
  format: json
  prettyPrint: true
  includeNotes: true

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

## Starten

```bash
mvn clean compile
mvn exec:java
```
