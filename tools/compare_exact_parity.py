#!/usr/bin/env python3
"""Compare legacy-reference and promoted-EXACT result snapshots.

Input JSON shape (either side):
{
  "positive_area_um2": 1.0,
  "negative_area_um2": 2.0,
  "positive_percent": 33.333333,
  "chunks_completed": 10,
  "chunks_failed": 0,
  "total_chunks": 10,
  "chunk_size_um": 2000.0
}

Runtime is intentionally excluded: it is not a scientific parity observable.
"""
import argparse
import json
import math
from pathlib import Path

FLOAT_FIELDS = (
    'positive_area_um2',
    'negative_area_um2',
    'positive_percent',
    'chunk_size_um',
)
INT_FIELDS = (
    'chunks_completed',
    'chunks_failed',
    'total_chunks',
)


def load(path):
    with Path(path).open() as f:
        return json.load(f)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('reference')
    ap.add_argument('promoted')
    ap.add_argument('--rel-tol', type=float, default=1e-12)
    ap.add_argument('--abs-tol', type=float, default=1e-9)
    args = ap.parse_args()

    a, b = load(args.reference), load(args.promoted)
    failures = []
    for key in INT_FIELDS:
        if int(a[key]) != int(b[key]):
            failures.append(f'{key}: {a[key]} != {b[key]}')
    for key in FLOAT_FIELDS:
        if not math.isclose(float(a[key]), float(b[key]), rel_tol=args.rel_tol, abs_tol=args.abs_tol):
            failures.append(f'{key}: {a[key]} != {b[key]}')

    if failures:
        print('PARITY FAIL')
        for failure in failures:
            print('  ' + failure)
        raise SystemExit(1)

    print('PARITY PASS')
    print(f"  chunks: {a['chunks_completed']}/{a['total_chunks']}")
    print(f"  positive area µm^2: {a['positive_area_um2']}")
    print(f"  negative area µm^2: {a['negative_area_um2']}")
    print(f"  positive %: {a['positive_percent']}")

if __name__ == '__main__':
    main()
