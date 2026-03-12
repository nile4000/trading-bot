# trading-bot

Java-21-Maven-Projekt fuer Backtesting und einen technischen Paper-Testnet-Pfad.

## Aktueller Stand

- V1: Backtesting plus technischer `PAPER`-Pfad
- CSV-basierter Datenimport fuer OHLCV-Bars
- Erste Strategie im `BACKTEST`
- JSON als Backtest-Ausgabeformat
- Logback fuer technische Laufzeit-Logs
- Anwendungsmodi sind `BACKTEST | PAPER`

Nicht Teil dieser V1:

- REST/API
- Health-Checks
- Multi-Bot-Betrieb

## Was Die App Aktuell Effektiv Ist

- Ein einzelner Trading-Bot mit zwei Betriebsarten: `BACKTEST` und `PAPER`
- `BACKTEST` ist heute der fachlich staerkere Pfad: CSV rein, `ema_cross` ueber `ta4j`, simulierte Ausfuehrung, JSON-Report raus
- `PAPER` ist heute ein technischer Testpfad: Binance Spot Testnet Preis rein, vorkonfigurierte `queued_actions`, `orderTest` validieren, lokales Paper-Portfolio fortschreiben
- `ta4j` ist aktuell Strategy-/Indikator-Schicht fuer den Backtest, nicht die Runtime, nicht das Reporting und nicht der Paper-Bot
- Die App ist damit heute eher ein kleiner Backtest-Bot plus technischer Paper-Execution-Testpfad als eine voll ausgebaute allgemeine Trading-Plattform

Aktuelle Ausbau-Richtung:

- weitere Strategien und Indikatoren zuerst im `BACKTEST`
- `PAPER` spaeter optional von `queued_actions` zu echter Strategieauswertung weiterentwickeln
- Reporting vorerst im eigenen App-Modell halten

## Voraussetzungen

- JDK 21
- Maven 3.9+

## Architektur

- `app`: schlanker Einstiegspunkt und Moduswahl
- `core`: modusunabhaengige Trading-Logik und Ports
- `modes.backtest`: historischer Ablauf, Runtime-Ausfuehrung und fachliche Reportgenerierung
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

## Ausfuehrungsmodi

- `BACKTEST`: historische Simulation gegen CSV- oder andere historische Datensaetze
- `PAPER`: live oder nahe Echtzeit gegen Demo-/Testumgebung ohne echtes Kapital
- Deployment-Ort wie lokal, Server oder Container ist davon getrennt und kein Trading-Modus

## Konfiguration

Die Standardkonfiguration liegt in [application.yml](c:/dev/trading/apps/trading-bot/src/main/resources/application.yml).

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

## Lokal Starten

```bash
mvn clean compile
mvn exec:java
```
