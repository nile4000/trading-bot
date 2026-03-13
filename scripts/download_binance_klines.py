#!/usr/bin/env python3
import argparse
import csv
import io
import sys
import urllib.request
import zipfile
from datetime import datetime, timezone
from pathlib import Path


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Download Binance monthly kline archives and convert them to the app CSV format."
    )
    parser.add_argument("--symbol", required=True, help="Trading symbol, e.g. BTCUSDT")
    parser.add_argument("--interval", required=True, help="Kline interval, e.g. 1m, 5m, 15m, 1h")
    parser.add_argument("--start", required=True, help="Inclusive start month in YYYY-MM format")
    parser.add_argument("--end", required=True, help="Inclusive end month in YYYY-MM format")
    parser.add_argument("--output", required=True, help="Target CSV path")
    return parser.parse_args()


def month_range(start_month: str, end_month: str) -> list[str]:
    start = datetime.strptime(start_month, "%Y-%m")
    end = datetime.strptime(end_month, "%Y-%m")
    if start > end:
        raise ValueError("start must not be after end")

    months = []
    year = start.year
    month = start.month
    while (year, month) <= (end.year, end.month):
        months.append(f"{year:04d}-{month:02d}")
        month += 1
        if month == 13:
            month = 1
            year += 1
    return months


def archive_url(symbol: str, interval: str, month: str) -> str:
    return (
        f"https://data.binance.vision/data/spot/monthly/klines/"
        f"{symbol}/{interval}/{symbol}-{interval}-{month}.zip"
    )


def iso_from_millis(value: str) -> str:
    timestamp = int(value)
    if timestamp >= 10**15:
        seconds = timestamp / 1_000_000
    elif timestamp >= 10**12:
        seconds = timestamp / 1_000
    else:
        seconds = timestamp
    instant = datetime.fromtimestamp(seconds, tz=timezone.utc)
    return instant.isoformat().replace("+00:00", "Z")


def open_archive_rows(url: str) -> list[list[str]]:
    with urllib.request.urlopen(url) as response:
        payload = response.read()

    with zipfile.ZipFile(io.BytesIO(payload)) as archive:
        csv_names = [name for name in archive.namelist() if name.endswith(".csv")]
        if len(csv_names) != 1:
            raise ValueError(f"Expected exactly one CSV in archive {url}, found {csv_names}")
        with archive.open(csv_names[0], "r") as raw_file:
            text_stream = io.TextIOWrapper(raw_file, encoding="utf-8")
            return list(csv.reader(text_stream))


def convert_row(source_row: list[str]) -> list[str]:
    if len(source_row) < 6:
        raise ValueError(f"Unexpected Binance row shape: {source_row}")
    return [
        iso_from_millis(source_row[6]),
        source_row[1],
        source_row[2],
        source_row[3],
        source_row[4],
        source_row[5],
    ]


def main() -> int:
    args = parse_args()
    months = month_range(args.start, args.end)
    output_path = Path(args.output)
    output_path.parent.mkdir(parents=True, exist_ok=True)

    row_count = 0
    with output_path.open("w", newline="", encoding="utf-8") as output_file:
        writer = csv.writer(output_file, lineterminator="\n")
        writer.writerow(["timestamp", "open", "high", "low", "close", "volume"])

        for month in months:
            url = archive_url(args.symbol, args.interval, month)
            print(f"Downloading {url}", file=sys.stderr)
            for row in open_archive_rows(url):
                if not row:
                    continue
                if row[0].lower() == "open_time":
                    continue
                writer.writerow(convert_row(row))
                row_count += 1

    print(f"Wrote {row_count} rows to {output_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
