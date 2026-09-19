# trading-bot

Java-21-Trading-Bot auf Quarkus mit zwei Modi:

- `BACKTEST` simuliert eine Strategie mit historischen CSV-Daten und erzeugt einen JSON-Report.
- `PAPER` wertet abgeschlossene Binance-Klines aus und kann Orders im Binance Spot Demo Mode validieren oder platzieren. Es wird kein echtes Kapital verwendet.

## Aktueller Stand

Der Paper-Bot kann aktuell:

- `BTCUSDT` oder ein anderes konfiguriertes Binance-Symbol verarbeiten
- abgeschlossene Binance-OHLCV-Klines im konfigurierten Timeframe laden
- beim Start die Strategie-Historie mit etwa `3 x` der laengsten Indikatorperiode aufwaermen
- jede abgeschlossene Kline nur einmal bewerten
- mit `ema_cross`, `sma_cross`, `rsi_reversion` oder `queued_actions` `BUY`, `SELL` und `HOLD` erzeugen
- Market-Orders entweder nur validieren oder im Binance Spot Demo Mode platzieren
- eine offene Bot-Position aus den eigenen Binance-Orders und deren Ausfuehrungen rekonstruieren
- nach einem Neustart ohne lokale Datenbank weiterarbeiten

Noch nicht vorhanden:

- produktionsfertige Fly.io-Dateien wie `Dockerfile` und `fly.toml`
- Health-/Management-Endpunkte
- Pagination beim Kontoabgleich ab 1000 Orders oder Trades pro Symbol
- Schutz gegen zwei gleichzeitig laufende Instanzen mit derselben `bot-id`

Damit ist der fachliche Demo-Trading-Pfad vorhanden. Fuer ein Fly.io-Deployment fehlt noch die Deployment-Verpackung.

## Voraussetzungen

- JDK 21
- Maven 3.9+
- Binance-Demo-API-Key mit Spot-Trading-Berechtigung fuer `PAPER`
- optional Python 3.10+ fuer den Download historischer Backtest-Daten

## Paper-Modus

Beide Order-Modi sehen dieselben abgeschlossenen Binance-Klines. Der Order-Modus aendert nur, was nach einem `BUY`- oder `SELL`-Signal passiert.

### `VALIDATE_ONLY` (Standard)

- sendet eine Binance-Ordervalidierung (`order/test`)
- erstellt keine Order im Demo-Konto
- simuliert die Position anschliessend lokal im laufenden Prozess
- verliert diese simulierte Position bei einem Neustart

Ein Log mit `decision=HOLD`, `execution=SKIPPED` und `detail=no_action` bedeutet, dass nichts gekauft oder verkauft wurde. `detail=bar_already_processed` bedeutet, dass seit dem letzten Poll noch keine neue abgeschlossene Kline vorhanden ist.

### `PLACE_ORDER`

- platziert eine echte Market-Order im Binance Spot Demo Mode
- verwendet eine deterministische `clientOrderId` aus `bot-id`, Symbol, Timeframe, Kline und Aktion
- sucht vor dem Platzieren nach derselben Order, damit ein unsicherer Request nicht einfach doppelt ausgefuehrt wird
- laedt nach einer Order die tatsaechlichen Binance-Ausfuehrungen (Fills) und beruecksichtigt Mengen, Preise und Gebuehren
- rundet SELL-Mengen nach Binances `LOT_SIZE` ab und behandelt einen danach nicht mehr verkaufbaren Rest als `AngelsShare`
- rekonstruiert die Bot-Position beim Start aus den Orders, deren `clientOrderId` zur konfigurierten `bot-id` gehoert
- gleicht den rekonstruierten Bestand mit den Binance-Kontostaenden ab

`initial-cash` ist auch in diesem Modus das dem Bot zugewiesene Startbudget. Frei verfuegbares Guthaben im Binance-Konto allein erhoeht dieses Budget nicht.

Pro `bot-id` darf nur eine Instanz gleichzeitig laufen. Der Code erzwingt diese Betriebsregel derzeit nicht. Werden bei der Synchronisation 1000 Orders oder 1000 Trades zurueckgegeben, bricht der Bot ab, da noch keine Pagination implementiert ist.

## Konfiguration

Die Defaults liegen in [application.yaml](C:/dev/trading/apps/trading-bot/src/main/resources/application.yaml). Das Profil `paper` aktiviert die Laufzeit-Logs. Standardmaessig ist `VALIDATE_ONLY` konfiguriert.

Die Binance-Demo-Zugangsdaten werden aus diesen Umgebungsvariablen gelesen:

```env
BINANCE_DEMO_API_KEY=...
BINANCE_DEMO_SECRET_KEY=...
```

Lokal kann dafuer eine nicht eingecheckte `.env` im Projektverzeichnis verwendet werden. API-Schluessel gehoeren nicht in `application.yaml` oder ins Repository.

Fuer echte Demo-Orders muessen in `application.yaml` diese Werte gesetzt sein:

```yaml
trading:
  paper:
    execution:
      order-mode: PLACE_ORDER
      place-orders-enabled: true
      max-order-notional: 25.0
```

Wichtige Paper-Einstellungen:

- `trading.paper.bot.bot-id`: stabile Identitaet des Bots; nicht nach dem ersten Trade wechseln
- `trading.paper.bot.symbol`: Handelspaar, zum Beispiel `BTCUSDT`
- `trading.paper.bot.timeframe`: Kline-Intervall, zum Beispiel `1m`
- `trading.paper.execution.order-quantity`: Kaufmenge und initiale Validierungsmenge
- `trading.paper.execution.initial-cash`: dem Bot zugewiesenes Startbudget
- `trading.paper.execution.max-order-notional`: maximale Kauf-Ordergroesse in der Quote-Waehrung
- `trading.paper.execution.tick-interval-millis`: Polling-Intervall; eine Kline wird trotzdem nur einmal verarbeitet

Verfuegbare Demo-Symbole liefert:
[Binance Demo exchangeInfo](https://demo-api.binance.com/api/v3/exchangeInfo)

## Lokal starten

Tests und Paket bauen:

```powershell
mvn clean test
mvn package -DskipTests
```

Paper-Bot im Quarkus-Dev-Modus starten:

```powershell
mvn quarkus:dev '-Dquarkus.args=paper' '-Dquarkus.profile=paper'
```

Gepackten Paper-Bot starten:

```powershell
java -Dquarkus.profile=paper -jar target/quarkus-app/quarkus-run.jar paper
```

Ein erfolgreicher Start zeigt unter anderem `exchange=BINANCE_SPOT_DEMO`, den konfigurierten Order-Modus und `restBaseUrl=https://demo-api.binance.com`.

## Backtest-Modus

Unterstuetzte Strategien:

- `queued_actions`
- `ema_cross`
- `sma_cross`
- `rsi_reversion`

CSV-Format:

```text
timestamp,open,high,low,close,volume
```

Beispiel fuer historische Daten:

```powershell
python scripts/download_binance_klines.py `
  --symbol BTCUSDT `
  --interval 1h `
  --start 2024-01 `
  --end 2024-03 `
  --output data/historical/BTCUSDT-1h.csv
```

Backtest im Dev-Modus:

```powershell
mvn quarkus:dev '-Dquarkus.args=backtest' '-Dquarkus.profile=backtest-1h'
```

Verfuegbare Backtest-Profile sind `backtest-1m`, `backtest-5m`, `backtest-15m` und `backtest-1h`.

## Architektur

- `quarkus`: Picocli-Kommandos, Config-Mapping und CDI-Wiring
- `core`: Trading-Runtime, Strategien, Portfolio- und Execution-Ports
- `modes.backtest`: Backtest-Ablauf und Reportgenerierung
- `modes.paper`: Paper-Bot-Setup und kontinuierlicher Loop
- `adapters.binance`: Binance-REST-Client
- `adapters.market`: CSV- und Binance-Kline-Marktdaten
- `adapters.execution`: simulierte und Binance-basierte Orderausfuehrung
- `adapters.portfolio`: konkrete Portfolio-Zustaende
- `adapters.reporting`: JSON-Ausgabe des Backtest-Reports
