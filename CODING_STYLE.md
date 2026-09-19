# Coding Style

Diese Datei ist die verbindliche Quelle für Projekt-, Architektur- und
Coding-Regeln.

## Projekt

Java-21-Quarkus-Anwendung für Backtests und Paper-Trading. Die Anwendung nutzt
ta4j für Strategie- und Indikatorlogik sowie Binance Spot Demo für Paper-Orders.

## Struktur

- `core` enthält fachliche Modelle, Runtime, Strategien sowie Execution- und
  Portfolio-Ports. Es darf nicht von `modes`, `adapters` oder `quarkus`
  abhängen.
- `modes.backtest` und `modes.paper` orchestrieren die jeweiligen Abläufe.
- `adapters` implementiert externe Schnittstellen: Binance, Marktdaten,
  Ausführung, Portfolio, Reporting und Konfiguration.
- `quarkus` enthält Picocli-Einstiegspunkte, CDI-Wiring und Quarkus-Konfiguration.
- Ausgabeformate gehören in `adapters.reporting`, nicht in Commands oder Logs.
- Neue Abhängigkeiten dürfen nicht aus `core` nach außen führen; die
  Zusammenstellung von Kernlogik und Infrastruktur geschieht außerhalb von `core`.

## Allgemein

- Schreibe einfachen, direkt lesbaren Java-Code; vermeide spekulative
  Abstraktionen, Wrapper und Refactorings außerhalb der Aufgabe.
- Bevorzuge top-level `record`s für fachliche, Konfigurations- und
  Schichtgrenzen-Modelle. Private Hilfszustände dürfen lokal bleiben.
- Benenne Klassen nach ihrer Rolle, nicht nach einem nebensächlichen
  Speicher- oder Implementierungsdetail.
- Kommentare erklären nur Absichten, Annahmen oder Trade-offs. Vereinfache
  Trading- und Backtest-Annahmen nahe am betroffenen Code. Keine
  Klassenkommentare ergänzen, die nur den Namen oder die offensichtliche Aufgabe
  wiederholen.
- Verwende Konstruktor-Injection, außer wenn eine Framework-Schnittstelle die
  Verdrahtung vorgibt.
- Keine toten Pfade, TODOs ohne konkreten Inhalt oder Debug-Ausgaben einchecken.
- Keine API-Schlüssel oder Secrets loggen, in Reports ausgeben oder einchecken.
- Keine Mainnet- oder Echtgeld-Unterstützung hinzufügen und bestehende
  Demo-/Order-Sicherungen nicht ohne ausdrücklichen Auftrag lockern.
- Zeitabhängige fachliche Logik erhält Zeit von außen, etwa über `Clock`, damit
  sie deterministisch testbar bleibt.

## Fachliche Regeln

- Verwende `BigDecimal` für Preise, Mengen, Gebühren, Geld- und Prozentwerte;
  für diese Werte niemals `double` oder `float`. Technische Werte wie
  Zeitfenster dürfen den vom externen API vorgegebenen Typ verwenden.
- Runde erst an einer externen Grenze, etwa für Börsenregeln oder die
  Reportausgabe; intern bleibt die verfügbare Präzision erhalten.
- Halte Ausführungsannahmen explizit. Backtest-Ergebnisse verwenden die
  definierte Ausführung, Gebühren und Slippage. `PLACE_ORDER` verwendet die von
  Binance zurückgegebenen Fills und Gebühren.
- Strategielogik bleibt in `core.strategy`; ta4j ist die Implementierungsbasis
  für Indikatoren und Strategien, nicht das Berichtsmodell.
- Trenne Runtime-Zustand, Strategie-Definition, Portfolio und Order-Ausführung.
- Behandle echte oder Demo-Orders vorsichtig: deterministische Client-Order-IDs
  und vorhandene Schutzmechanismen gegen Doppelplatzierungen beibehalten.

## Konfiguration und Fehlerbehandlung

- Neue verhaltenssteuernde Einstellungen kommen nach `application.yaml`.
  Quarkus-Config-Mappings liegen in `quarkus`; die daraus abgeleiteten kleinen,
  getypten Konfigurationsmodelle liegen unter `adapters.config`.
- Konfiguration nach Concern gruppieren: `trading.app`, `trading.reporting`,
  `trading.backtest` und `trading.paper`.
- Validiere an Systemgrenzen und für fachliche Invarianten. Keine redundanten
  Null-Prüfungen in intern garantierten Abläufen.
- Fehler müssen sagen, was fehlgeschlagen ist und bei Eingabedaten nach
  Möglichkeit Datei, Zeile oder Parameter nennen.
- Wrapping von Exceptions nur dann, wenn zusätzlicher fachlicher Kontext entsteht.

## Logging, Reporting und Tests

- Verwende Quarkus/JBoss-Logging für technische Laufzeitinformationen.
- Backtest-Ergebnisse gehören in Report-Modelle und Renderer, nicht in Logs.
- Das Backtest-JSON bleibt stabil, maschinenlesbar und versioniert; Geld- und
  Prozentwerte werden numerisch mit Skala `4` ausgegeben.
- Ergänze oder aktualisiere bei Verhaltensänderungen fokussierte JUnit-Tests.
- Teste fachliches Verhalten und Regressionen, nicht reine technische
  Verdrahtung. Factory-Tests sind nur sinnvoll, wenn die Factory selbst
  fachliche Auswahl- oder Berechnungslogik enthält.
- Tests rufen keine echten Binance-Endpunkte auf.
- Vor dem Abschluss die betroffenen Tests ausführen; bei größeren Änderungen
  `mvn test`. `mvn compile` genügt nur, wenn kein sinnvoller Testpfad betroffen
  ist.
