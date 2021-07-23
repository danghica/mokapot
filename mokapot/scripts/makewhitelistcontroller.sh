#!/bin/bash
set -e # if something goes wrong, error out, don't try to continue
set +a # keep variables, e.g. passwords, private

if [ "x$1" = "x" ] || [ "x$2" = "x" ] || [ "$1" = "--help" ] || [ -e "$1" ]
then
  echo 'Usage: makewhitelistcontroller.sh directory duration'
  echo 'The directory should not exist; it will be created.'
  echo 'The duration is measured in days; the whitelist will stop working after'
  echo 'the given duration (forcing a new one to be issued).'
  echo 'The directory will store a whitelist controller; this contains'
  echo 'the cryptographic material for updating the whitelist.'
  echo 'Keep the controller secret! It is not needed to use the whitelist.'
  exit 0
fi

# Read password.
echo 'Enter a password that will be used to protect the whitelist controller.'
echo 'This is a password for the controller (i.e. it is needed to add new'
echo 'entries to the whitelist); it is not needed to use the whitelist.'
echo -n 'Password: '
IFS= read -r -s password1
echo
echo -n 'Repeat password: '
IFS= read -r -s password2
echo

if [ "x$password1" != "x$password2" ]
then
  echo 'Passwords do not match. The controller was not created.'
  exit 1
fi

# Read name.
echo 'Enter a name for the whitelist.'
echo 'This will be used to identify the whitelist.'
echo 'It may contain spaces, capital letters, etc.; be descriptive!'
echo 'Some special characters, like " and /, may not be supported.'
echo -n 'Whitelist name: '
IFS= read -r wlname
echo

# Create the whitelist controller directory structure.
mkdir -p "$1"
chmod go= "$1" # i.e. remove all permissions with this dir from third parties
mkdir -p "$1/privatekeys/tmp" # a separate tree for private keys
mkdir -p "$1/state/certificates"
mkdir -p "$1/tmp"

# Create initial state files.
touch "$1/state/index"
echo 100000 > "$1/state/uid"

# Create config file.
cat > "$1/config" << 'EOF'
[ ca ]
default_ca = whitelist_ca

[ whitelist_ca ]
new_certs_dir = ./state/certificates
certificate = ./whitelist-cert.pem
private_key = ./privatekeys/whitelist-privatekey.pem
database = ./state/index
serial = ./state/uid
default_md = sha256
preserve = no
unique_subject = no
name_opt = ca_default
cert_opt = ca_default
policy = only_cn_required

[ only_cn_required ]
commonName = supplied
countryName = optional
stateOrProvinceName = optional
organizationName = optional
organizationalUnitName = optional
emailAddress = optional

[ req ]
default_keyfile = ./privatekeys/tmp/latest.pem
default_md = sha256
distinguished_name = appended_dn
prompt = no

[ ca_extensions ]
basicConstraints = critical,CA:TRUE,pathlen:0
subjectKeyIdentifier = hash

[ req_extensions ]
basicConstraints = CA:FALSE
subjectKeyIdentifier = hash

[ appended_dn ]
EOF
echo "CN = $wlname" >> "$1/config"

# Create the whitelist keys/certificate (public and private).
(
  set -e
  cd "$1"
  openssl req -config ./config -x509 -passout fd:3 -newkey ec \
    -pkeyopt ec_paramgen_curve:prime256v1 -pkeyopt ec_param_enc:named_curve \
    -extensions ca_extensions -out whitelist-cert.pem \
    -keyout privatekeys/whitelist-privatekey.pem -days "$2" 3<<<"$password1"
)

echo 'The whitelist controller has been created.'
echo 'Use addwhitelistentry.sh to add entries to the whitelist.'
