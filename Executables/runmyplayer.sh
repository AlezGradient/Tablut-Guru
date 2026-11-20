#!/bin/bash

if [ "$#" -ne 3 ]; then
    exit 1
fi

java -jar  --enable-native-access=ALL-UNNAMED TablutGuru.jar "$1" "$2" "$3" 4
