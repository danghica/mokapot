#!/bin/sh
ant test-mokapot-whitelist | perl -nle '$|=1; s/     \[java\] // and (/Listening for transport/ or print)'
