#!/bin/sh

# A simple script for making a pair of linked .p12 files.
# Intended for testing purposes only.
# // This should be removed from the repository before
# // public release.

ENDPOINTPASS="testpassword1"
export ENDPOINTPASS
WHITELISTPASS="testpassword2"
export WHITELISTPASS

keytool -genkeypair -alias whitelist -keyalg ec -dname "CN=Whitelist Name" -ext BC:c=ca:TRUE,pathlen:0 -ext KU=kCS -validity "1" -storetype pkcs12 -storepass:env WHITELISTPASS -keystore whitelist-keys.p12

for epname in test1.p12 test2.p12
do
    keytool -genkeypair -alias endpointkey -keyalg ec -dname "CN=Endpoint Name" -ext BC:C=ca:FALSE -ext KU=keyE -ext eKU=sA,cA -validity "1" -storetype pkcs12 -storepass:env ENDPOINTPASS -keystore "$epname"
    (keytool -certreq -alias endpointkey -storepass:env ENDPOINTPASS -keystore "$epname" | keytool -gencert -rfc -alias whitelist -keyalg ec -validity "1" -storepass:env WHITELISTPASS -keystore whitelist-keys.p12 -ext BC:C=ca:FALSE -ext KU=dat -ext eKU=sA,cA; keytool -exportcert -rfc -alias whitelist -storepass:env WHITELISTPASS -keystore whitelist-keys.p12) | keytool -importcert -alias endpointkey -storepass:env ENDPOINTPASS -noprompt -keystore "$epname"
done
