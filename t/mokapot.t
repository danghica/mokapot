#!/bin/sh
ant test-mokapot | perl -nle '$|=1; s/     \[java\] // and (/Listening for transport/ or print)'
