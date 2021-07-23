#!/bin/bash
set -e # if something goes wrong, error out, don't try to continue
set +a # keep variables, e.g. passwords, private

if [ "x$1" = "x" ] || [ "x$2" = "x" ] || [ "$1" = "--help" ] \
   || [ '!' -d "$1" ] || [ "x$3" = "x" ] || [ -e "$3" ]
then
  echo 'Usage: addwhitelistentry.sh directory duration outfile'
  echo 'The directory should be an existing whitelist controller.'
  echo 'The duration is measured in days; the entry will stop working after'
  echo 'the given duration (forcing a new one to be added).'
  echo 'The output file (e.g. "endpoint.p12") is an endpoint ID file that'
  echo 'should be moved to the machine which will run the program.'
  exit 0
fi

# Read password.
echo 'Enter a password that will be used to protect the whitelist entry.'
echo 'This password will be required to use the generated .p12 file.'
echo -n 'Password: '
IFS= read -r -s password1
echo
echo -n 'Repeat password: '
IFS= read -r -s password2
echo

if [ "x$password1" != "x$password2" ]
then
  echo 'Passwords do not match. The entry was not created.'
  exit 1
fi

# Read name.
echo 'Enter a name for the whitelist entry (i.e. endpoint).'
echo 'This will be used to identify an individual running process.'
echo 'It may contain spaces, capital letters, etc.; be descriptive!'
echo 'Some special characters, like " and /, may not be supported.'
echo -n 'Endpoint name: '
IFS= read -r wlename
echo

# Read whitelist unlock password.
echo 'Enter the password that protects the whitelist controller.'
echo '(You set this password earlier, when creating the whitelist controller.)'
echo -n 'Whitelist controller password: '
IFS= read -r -s password3
echo

# Create config file. For some reason, the DN is hardcoded in the config...
cat > "$1/tmp/latest-config" << 'EOF'
[ req ]
default_md = sha256
distinguished_name = appended_dn
prompt = no

[ req_extensions ]
basicConstraints = CA:FALSE
subjectKeyIdentifier = hash

[ appended_dn ]
EOF
echo "CN = $wlename" >> "$1/tmp/latest-config"

(
  set -e
  cd "$1"

  openssl req -config ./tmp/latest-config -passout fd:3 -newkey ec \
    -pkeyopt ec_paramgen_curve:prime256v1 -pkeyopt ec_param_enc:named_curve \
    -extensions req_extensions -out tmp/latest-endpoint-csr.pem \
    -keyout privatekeys/tmp/latest-endpoint-privatekey.pem -days "$2" \
    3<<<"$password1"

  openssl ca -config ./config -passin fd:3 -out tmp/latest-endpoint-signed.pem \
    -days "$2" -batch -infiles tmp/latest-endpoint-csr.pem 3<<<"$password3"

  openssl pkcs12 -export -passin fd:3 -passout fd:4 \
    -in tmp/latest-endpoint-signed.pem \
    -inkey privatekeys/tmp/latest-endpoint-privatekey.pem \
    -certfile whitelist-cert.pem -name endpointkey \
    -out privatekeys/tmp/latest-endpoint.p12 3<<<"$password1" 4<<<"$password1"
) || true

# Cleanup all temporary auth material, and move the endpoint.p12 into the
# place the user wanted it.
(
  set -e
  chmod og= "$1/privatekeys/tmp/latest-endpoint.p12"
  mv -f "$1/privatekeys/tmp/latest-endpoint.p12" "$3" || \
    echo "Could not output the .p12 file; something must have gone wrong."
  cd "$1"
  rm -f tmp/latest-config
  rm -f tmp/latest-endpoint-csr.pem
  rm -f tmp/latest-endpoint-signed.pem
  rm -f privatekeys/tmp/latest-endpoint-privatekey.pem
  rm -f privatekeys/tmp/latest-endpoint.p12 # in case mv failed
)
