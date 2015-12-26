#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

BOINC_DIR=$DIR/..

java -cp /app/boinc-project/download/%uberjar_name% %assimilator_class% \$@