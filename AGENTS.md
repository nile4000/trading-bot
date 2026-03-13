# AGENTS.md

## Purpose

- This project is a small Java 21 Maven application for backtesting and a technical paper-trading path.
- Prefer simple, readable code over generic abstractions.
- Keep runtime concerns, use-case orchestration, reporting, and strategy logic separated.

## Configuration

- Application defaults live in `src/main/resources/application.yaml`.
- Group configuration by concern: `trading.app`, `trading.reporting`, `trading.backtest`, `trading.paper`.
- Keep configuration classes small, typed, and under `adapters.config`.
- Prefer Quarkus config mapping and profile overrides over custom config loaders or resource switching.
- If a new setting affects behavior, prefer adding it to YAML and mapping it explicitly.

## Logging And Reporting

- Use Quarkus/JBoss logging for technical runtime logs.
- Keep technical logs and business output separate.
- Backtest results belong in report objects and report renderers, not in log statements.
- Report rendering should not happen in commands or setup classes unless the renderer is injected directly there as the final output step.
- JSON is the current default report format and should stay stable and machine-readable.
- The current backtest JSON schema is versioned and should evolve deliberately.
- Monetary and percentage values in backtest reports should be emitted as numeric JSON values with scale `4`.
- Report summaries and position details belong in dedicated report models, not in ad-hoc maps or log lines.
- Versioned or reusable report-schema records should live in dedicated files under the reporting layer.
- Keep nested or local records only for truly private helper state with no reuse outside one class when they improve readability.

## Application Structure

- Use Quarkus command mode / Picocli as the runtime entrypoint.
- Structure the app as `quarkus`, `core`, `modes`, and `adapters`.
- Keep `core` free of dependencies on `modes` and `adapters`.
- Keep end-to-end orchestration in `modes.backtest` and `modes.paper`.
- Keep output formatting in `adapters.reporting`.
- Keep strategy definition, runtime, execution, and portfolio concerns in separate core packages.
- Prefer top-level records for domain, config, runtime-state, and cross-layer transport models.
- Avoid extracting one-field local helper records when a simple primitive or existing model keeps the code clearer.
- Prefer role-based class names over storage-detail names like `InMemory` unless the storage mechanism is the main distinction.
- Avoid thin forwarding services or wrappers without their own behavior; prefer direct orchestration in commands or mode classes.

## Backtest Modeling

- Backtest reports should include simulation metadata and position-level details.
- Backtest metadata should include the shared execution mode and use `BACKTEST` for historical runs.
- Keep simulation assumptions explicit in metadata, especially `executionModel` and `positionSizingModel`.
- `action_bar_close` means the fill is simulated on the close of the action bar.
- `all_in_spot` means the full available cash balance is allocated on entry and fully closed on exit.
- Prefer position reports over raw trade logs when exposing backtest details.
- Use `ta4j` for strategy and indicator logic in `BACKTEST`, not as the primary reporting model.
- Keep backtest reporting in the app-specific report layer unless multiple strategies or additional standard metrics justify a ta4j-based analysis layer.

## Bot And Strategy Models

- Use `BACKTEST | PAPER` as the current execution-mode vocabulary across runtime and backtest reporting.
- Do not use `LOCAL` as a primary trading mode; it is a deployment concern, not an execution reality.
- Strategy-facing identifiers should stay separate from bot runtime state.
- Strategy definitions should carry `symbol`, `timeframe`, `strategyName`, and typed strategy parameters.
- The manager layer is for bot health and coordination, not for portfolio calculation or strategy evaluation.

## Comments And Documentation

- Add a short class-level comment to worker classes so their responsibility is obvious at a glance.
- Use comments sparingly and only where intent or tradeoffs are not immediately obvious.
- Document simplified trading or backtesting assumptions close to the relevant code.

## Error Handling

- Error messages must tell the user what failed and, when useful, which file, row, or parameter caused it.
- Wrap lower-level exceptions only when the wrapper adds context.
- Keep guards at system boundaries and on domain invariants.
- Avoid repetitive null-check boilerplate in internal orchestration and wiring classes.
- Do not duplicate guards that are already guaranteed by earlier config validation or adapter validation unless the later check adds new domain context.

## Linting And Build Hygiene

- Code should compile cleanly with Maven before considering a task complete.
- Prefer no avoidable compiler warnings in IDE or Maven output.
- Do not introduce formatting-only churn in unrelated files.

## Testing

- Keep production code testable and extend tests when behavior or structure changes.
- Preserve green Maven compile and test runs after non-trivial refactors.

## Current Commands

- Build: `mvn clean compile`
- Run backtest in dev mode: `mvn quarkus:dev -Dquarkus.args=\"backtest\"`
- Run paper bot in dev mode: `mvn quarkus:dev -Dquarkus.args=\"paper\"`
- Run packaged app: `java -jar target/quarkus-app/quarkus-run.jar backtest`
