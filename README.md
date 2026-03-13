# trading-bot

Java-21-Maven-Projekt fuer Backtesting und einen technischen Paper-Testnet-Pfad.

## Aktueller Stand

- V1: Backtesting plus technischer `PAPER`-Pfad
- CSV-basierter Datenimport fuer OHLCV-Bars
- Erste Strategien fuer `BACKTEST` und `PAPER`
- JSON als Backtest-Ausgabeformat
- Quarkus als Runtime fuer Bootstrap, Config, Profile und Command-Entry

Nicht Teil dieser V1:

- REST/API
- Health-Checks
- Multi-Bot-Betrieb

## Was Die App Aktuell Effektiv Ist

- Ein einzelner Trading-Bot mit zwei Betriebsarten: `BACKTEST` und `PAPER`
- `BACKTEST` ist heute der fachlich staerkere Pfad: CSV rein, `ema_cross` ueber `ta4j`, simulierte Ausfuehrung, JSON-Report raus
- `PAPER` kann heute einfache ta4j-Strategien oder `queued_actions` gegen Binance Spot Testnet laufen lassen
- `ta4j` ist aktuell Strategy-/Indikator-Schicht fuer den Backtest, nicht die Runtime, nicht das Reporting und nicht der Paper-Bot
- Die App ist damit heute eher ein kleiner Strategy-Bot fuer Backtest und Paper-Testnet als eine voll ausgebaute allgemeine Trading-Plattform

Aktuelle Ausbau-Richtung:

- weitere Strategien und Indikatoren zuerst im `BACKTEST`
- `PAPER` auf robusteren Dauerlauf, Risk-Regeln und Session-Steuerung ausbauen
- Reporting vorerst im eigenen App-Modell halten

## Voraussetzungen

- JDK 21
- Maven 3.9+

## Architektur

- `quarkus`: Command-Entry, Config-Mapping und CDI-Wiring
- `core`: modusunabhaengige Trading-Logik und Ports
- `modes.backtest`: historischer Ablauf, Runtime-Ausfuehrung und fachliche Reportgenerierung
- `modes.paper`: technischer Paper-Flow, Runtime-Wiring und Runner-Loop
- `adapters.config`: typed Config-Records fuer Backtest, Paper und Reporting
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

Die Runtime-Konfiguration liegt in [application.yaml](c:/dev/trading/apps/trading-bot/src/main/resources/application.yaml).

Quarkus-Profile fuer die Backtest-Datensaetze:

- `backtest-1m`
- `backtest-5m`
- `backtest-15m`
- `backtest-1h`
- `paper`

Die App nutzt keine eigene Config-Datei-Auswahl mehr. Profil-Overrides laufen ueber Quarkus `application.yaml` und `quarkus.profile`.

Aktuell unterstuetzte Backtest-Strategien:

- `queued_actions`
- `ema_cross`
- `sma_cross`
- `rsi_reversion`

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

- Technische Logs laufen ueber Quarkus Logging
- Backtest-Ergebnisse werden als JSON auf `stdout` ausgegeben
- Logging und Reporting sind bewusst getrennt
- JSON ist fuer diese V1 der feste standard
- `PAPER` nutzt Binance Spot Testnet REST, validiert Orders oder platziert echte Testnet-Market-Orders und spiegelt erfolgreiche Aktionen in das lokale Paper-Portfolio
- Das aktuelle JSON-Schema wird als `reportVersion: "v5"` ausgegeben
- Backtest-Reports enthalten `metadata.mode: "BACKTEST"` als explizite Ausfuehrungsrealitaet
- Geld- und Prozentwerte werden als numerische JSON-Werte mit 4 Dezimalstellen ausgegeben
- Zaehler bleiben Integer, Statuswerte bleiben Boolean
- `action_bar_close` bedeutet, dass die Ausfuehrung auf dem Close der Action-Bar simuliert wird

Wichtige Report-Felder:

- `barCount`, `closedTradeCount`: Integer
- `totalReturnPercent`, `buyAndHoldReturnPercent`, `maxDrawdownPercent`, `profitFactor`, `winRatePercent`: Kernmetriken zur Backtest-Bewertung
- `averageWinningTrade`, `averageLosingTrade`, `timeInMarketDays`, `exposurePercent`: Qualitaets- und Expositionsmetriken
- `initialCash`, `finalValue`, `totalReturnPercent`, `winRatePercent`: numerische Werte mit 4 Dezimalstellen

## Lokal Starten

```bash
mvn test
mvn package
```

Beispiele:

```powershell
cmd /c "java -Dquarkus.profile=backtest-1h -jar target\\quarkus-app\\quarkus-run.jar backtest"
cmd /c "java -Dquarkus.profile=backtest-1m -jar target\\quarkus-app\\quarkus-run.jar backtest"
cmd /c "java -Dquarkus.profile=backtest-5m -jar target\\quarkus-app\\quarkus-run.jar backtest"
cmd /c "java -Dquarkus.profile=backtest-15m -jar target\\quarkus-app\\quarkus-run.jar backtest"
cmd /c "java -Dquarkus.profile=paper -jar target\\quarkus-app\\quarkus-run.jar paper"
```

Alternativ lokal ohne Paket-Build:

```powershell
mvn quarkus:dev '-Dquarkus.args=backtest' '-Dquarkus.profile=backtest-1h'
```

Fuer die groesseren historischen BTCUSDT-Dateien ist lokal oft ein begrenzter Heap stabiler als die JVM-Defaults:

```powershell
$env:MAVEN_OPTS='-Xms128m -Xmx768m -XX:+UseSerialGC'
cmd /c "java -Xms128m -Xmx768m -XX:+UseSerialGC -Dquarkus.profile=backtest-1h -jar target\\quarkus-app\\quarkus-run.jar backtest"
```

Aktuell muessen die Binance-Testnet-Secrets fuer `PAPER` noch als Prozess-Umgebungsvariablen gesetzt werden.
Wenn du eine lokale `.env` im Projektverzeichnis hast, kannst du sie in PowerShell so in die aktuelle Session laden:

```powershell
Get-Content .env | ForEach-Object {
  if ($_ -match '^\s*#' -or $_ -match '^\s*$') { return }
  $name, $value = $_ -split '=', 2
  [System.Environment]::SetEnvironmentVariable($name.Trim(), $value.Trim(), 'Process')
}
```

Danach startest du den Paper-Bot so:

```powershell
cmd /c "java -Dquarkus.profile=paper -jar target\\quarkus-app\\quarkus-run.jar paper"
```

Fuer `PAPER` mit echten Testnet-Orders:

- `paper.execution.orderMode: PLACE_ORDER`
- `paper.execution.placeOrdersEnabled: true`
- `paper.execution.maxOrderNotional: ...`

Die Tick-Logs bleiben knapp, z. B.:

```text
Paper tick. decision=BUY, execution=PLACED, position=OPEN, price=70178.00, detail=testnet_order#12345
```
