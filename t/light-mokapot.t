#!/bin/sh
ant test-mokapot-light | perl -nle '$|=1; s/     \[java\] // and (/Listening for transport/ or print)'
