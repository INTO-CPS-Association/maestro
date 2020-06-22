#!/usr/bin/env bash
set -euo pipefail

sphinx-build -b html ./ buildDir
