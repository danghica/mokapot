#!/bin/bash

set -e

echo "Downloading dependencies..."

# Create an output directory if not one already
[ -d contrib ] || echo "Making contrib directory..." && mkdir -p contrib

# Download the dependencies if they do not already exist
([ -f contrib/junit.jar ] && echo "Junit already extracted, skipping download") \
    || (echo "Junit not downloaded yet, downloading it now..." \
        && wget https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/1.2.0/junit-platform-console-standalone-1.2.0.jar \
        -O contrib/junit.jar)




    
