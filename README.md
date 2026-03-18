# trading-bot

Ein Java-Bot mit `BACKTEST`- und `PAPER`-Modus. Die App verarbeitet pro Lauf ein konfiguriertes Symbol, bewertet eine Strategie und entscheidet ueber `BUY`, `SELL` oder `HOLD`.

Backtest-Ergebnisse werden als JSON ausgegeben, der `PAPER`-Modus laeuft als technischer Testpfad gegen das Binance Spot Testnet.

## Voraussetzungen

- JDK 21
- Maven 3.9+

## Ausfuehrungsmodi

- `BACKTEST`: historische Simulation gegen einen CSV-Datensatz

  Ablauf:
  1. Historische CSV-Daten pro Symbol und Intervall bereitstellen oder herunterladen
  2. CSV-Daten einlesen
  3. Strategielogik ueber `ta4j` bewerten
  4. Orders simulieren
  5. JSON-Report ausgeben

  Historische Daten koennen fuer ein Symbol wie `BTCUSDT` ueber das Skript `scripts/download_binance_klines.py` aus den Binance-Archivdaten erzeugt werden. Das Skript laedt monatliche Kline-Daten herunter und konvertiert sie in das von der App erwartete CSV-Format.

  Beispiel:

  ```powershell
  python scripts/download_binance_klines.py `
    --symbol BTCUSDT `
    --interval 1h `
    --start 2024-01 `
    --end 2024-03 `
    --output data/historical/BTCUSDT-1h.csv
  ```

  Das erwartete CSV-Format ist im Abschnitt `CSV-Header-Format` beschrieben.

- `PAPER`: technischer Lauf gegen eine Demoumgebung ohne echtes Kapital

  Ablauf:
  1. Das konfigurierte Symbol fuer das Binance Spot Testnet festlegen
  2. Den aktuellen Preis regelmaessig abrufen
  3. Daraus eine laufende Preisserie fuer die Strategiebewertung aufbauen
  4. Strategielogik bewerten und ueber `BUY`, `SELL` oder `HOLD` entscheiden
  5. Die Aktion entweder nur validieren oder als Testnet-Order platzieren
  6. Das lokale Paper-Portfolio aktualisieren und Tick-Logs ausgeben

  Verfuegbare Symbole fuer den `PAPER`-Modus lassen sich ueber das Binance Spot Testnet pruefen:
  `https://testnet.binance.vision/api/v3/exchangeInfo`

  Dort enthaelt das Feld `symbols` die aktuell verfuegbaren Handelspaare, zum Beispiel `BTCUSDT`.

- Der Deployment-Ort, zum Beispiel lokal, auf einem Server oder im Container, ist davon getrennt und kein Trading-Modus.

## Architektur

- `quarkus`: Command-Entry, Config-Mapping und CDI-Wiring
- `core`: modusunabhaengige Trading-Logik und Ports
- `modes.backtest`: historischer Ablauf, Runtime-Ausfuehrung und fachliche Reportgenerierung
- `modes.paper`: technischer Paper-Flow, Runtime-Wiring und Runner-Loop
- `adapters.config`: typisierte Konfigurationsobjekte fuer Backtest, Paper und Reporting
- `adapters.market`: CSV- und Marktpreis-Adapter
- `adapters.execution`: simulierte und Exchange-nahe Ausfuehrung
- `adapters.portfolio`: konkrete Portfolio-Implementierungen
- `adapters.reporting`: JSON-Rendering des `BacktestReport`

## Konfiguration

Die Runtime-Konfiguration liegt in [application.yaml](/c:/dev/trading/apps/trading-bot/src/main/resources/application.yaml).

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

## CSV-Header-Format

```text
timestamp,open,high,low,close,volume
```

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

Fuer `PAPER` koennen die Binance-Testnet-Secrets in einer lokalen `.env` im aktuellen Arbeitsverzeichnis hinterlegt werden.
Die Werte werden ueber `application.yaml` aus den Umgebungsvariablen gelesen.

Beispiel:

```env
BINANCE_TESTNET_API_KEY=...
BINANCE_TESTNET_SECRET_KEY=...
```

Danach startest du den Paper-Bot so:

```powershell
cmd /c "java -Dquarkus.profile=paper -jar target\\quarkus-app\\quarkus-run.jar paper"
```

Fuer `PAPER` mit echten Testnet-Orders:

- `paper.execution.orderMode: PLACE_ORDER`
- `paper.execution.placeOrdersEnabled: true`
- `paper.execution.maxOrderNotional: ...`
