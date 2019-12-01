#!/bin/bash

# Sets up GPG for artifact and commit signing
#
# Specifically:
#   - imports the GPG key from the GPG_KEY environment variable into the keyring, and
#   - pre-caches the passphrase in gpg-agent.
#

set -e

if [ -z ${GPG_KEY+x} ] ; then
  echo "GPG_KEY environment variable must contain the ascii-armored GPG key" 1>&2
  exit 1
fi

if [ -z ${GPG_PASSPHRASE+x} ] ; then
  echo "GPG_PASSPHRASE environment variable must contain passphrase for the GPG key" 1>&2
  exit 1
fi

# Import the GPG_KEY into the keyring
echo Importing GPG key into keyring...
gpg --batch --import <<EOF
${GPG_KEY}
EOF
echo ...key imported

# Enable pre-caching of passphrases
echo Enabling pre-caching of passphrase...
echo allow-preset-passphrase >> ~/.gnupg/gpg-agent.conf
gpg-connect-agent reloadagent /bye
echo ...pre-caching passphrases enabled

# Find the keygrip of the key.
# This uses the last key in the keyring.
echo Identifying keygrip...
GPG_KEYGRIP=$(gpg --batch --list-keys --with-keygrip --with-colons | grep ^grp | cut -d : -f 10 | tail -n 1)
echo ...identified keygrip "${GPG_KEYGRIP}"

# Create a hexadecimal string of the GPG_PASSPHRASE
echo Encoding GPG_PASSPHRASE into hexadecimal
GPG_PASSPHRASE_HEX=$((tr -d '\n' | hexdump -v -e '/1 "%02X"' && echo)<<EOF
${GPG_PASSPHRASE}
EOF
)
echo ...encoded GPG_PASSPHRASE into hexadecimal

# Pre-cache the passphrase
# https://gnupg.org/documentation/manuals/gnupg/Agent-PRESET_005fPASSPHRASE.html
echo Caching GPG_PASSPHRASE...
gpg-connect-agent <<EOF
PRESET_PASSPHRASE ${GPG_KEYGRIP} -1 ${GPG_PASSPHRASE_HEX}
EOF
echo ...cached GPG_PASSPHRASE
