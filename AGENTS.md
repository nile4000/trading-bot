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

## Application Structure

- `App` should stay a thin entry point.
- Use application-layer services for end-to-end orchestration.
- Keep backtest calculation in dedicated backtest classes.
- Keep output formatting in reporting classes.

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
