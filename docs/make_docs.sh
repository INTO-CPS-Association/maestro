#!/usr/bin/env bash
set -euo pipefail

sphinx-build -b html . ./build/html
sphinx-build -M latexpdf . ./build/latex