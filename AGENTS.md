# AGENTS.md

## Purpose

- This project is a small Java 21 Maven application for local strategy backtesting.
- Prefer simple, readable code over generic abstractions.
- Keep runtime concerns, use-case orchestration, reporting, and strategy logic separated.

## Configuration

- Application defaults live in `src/main/resources/application.yml`.
- Group configuration by concern: `backtest`, `reporting`, `logging`.
- Keep configuration classes small, typed, and close to the application layer.
- If a new setting affects behavior, prefer adding it to YAML and mapping it explicitly.

## Logging And Reporting

- Use Logback for technical runtime logs.
- Keep technical logs and business output separate.
- Backtest results belong in report objects and report renderers, not in log statements.
- Report rendering should not happen in `App`.
- JSON is the current default report format and should stay stable and machine-readable.
- The current backtest JSON schema is versioned and should evolve deliberately.
- Monetary and percentage values in backtest reports should be emitted as numeric JSON values with scale `4`.
- Report summaries and position details belong in dedicated report models, not in ad-hoc maps or log lines.
- Versioned or reusable report-schema records should live in dedicated files under the reporting layer.
- Keep nested or local records only for truly private helper state with no reuse outside one class.

## Application Structure

- `App` should stay a thin entry point.
- Use application-layer services for end-to-end orchestration.
- Keep backtest calculation in dedicated backtest classes.
- Keep output formatting in reporting classes.
- Keep strategy definition, bot runtime state, execution, and portfolio state in separate models.
- Prefer top-level records for domain, config, runtime-state, and cross-layer transport models.
- Avoid extracting one-field local helper records when a simple primitive or existing model keeps the code clearer.
- Prefer role-based class names over storage-detail names like `InMemory` unless the storage mechanism is the main distinction.

## Backtest Modeling

- Backtest reports should include simulation metadata and position-level details.
- Backtest metadata should include the shared execution mode and use `BACKTEST` for historical runs.
- Keep simulation assumptions explicit in metadata, especially `executionModel` and `positionSizingModel`.
- `signal_bar_close` means the fill is simulated on the close of the signal bar.
- `all_in_spot` means the full available cash balance is allocated on entry and fully closed on exit.
- Prefer position reports over raw trade logs when exposing backtest details.

## Bot And Strategy Models

- Use `BACKTEST | PAPER | LIVE` as the canonical execution-mode vocabulary across bot runtime, backtest reporting, and future APIs.
- Do not use `LOCAL` as a primary trading mode; it is a deployment concern, not an execution reality.
- Bot runtime state should stay operational and minimal: `botId`, `botVersion`, `mode`, `status`, `lastRunAt`, `lastSuccessAt`, `lastError`, `openPosition`.
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

## Linting And Build Hygiene

- Code should compile cleanly with Maven before considering a task complete.
- Prefer no avoidable compiler warnings in IDE or Maven output.
- Do not introduce formatting-only churn in unrelated files.

## Testing

- No test classes are required yet.
- Still write production code so logic can be tested later without major refactoring.

## Current Commands

- Build: `mvn clean compile`
- Run app: `mvn exec:java`
