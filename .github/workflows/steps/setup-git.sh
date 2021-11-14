#!/bin/bash

# Sets up git for making commits and signing commits / tags.
#
# Expects that setup-gpg.sh has previously been executed to import GPG signing keys
# and pre-cache GPG key passphrase

set -e

echo Enabling auto-signing of git commits and tags
GPG_USER=$(gpg --batch --list-keys --with-colons | grep ^uid | cut -d : -f 10 | head -n 1)
GPG_USER_NAME=$(echo "${GPG_USER}" | sed 's/\(.*\) <.*>/\1/g')
GPG_USER_EMAIL=$(echo "${GPG_USER}" | sed 's/.* <\(.*\)>/\1/g')
GPG_KEYID=$(gpg --batch --list-keys --with-colons | grep ^fpr | cut -d : -f 10 | head -n 1)
git config --local user.signingkey "${GPG_KEYID}"
git config --local commit.gpgsign true
git config --local tag.forceSignAnnotated true

echo Setting git user to "${GPG_USER_NAME}" with email "${GPG_USER_EMAIL}"
git config --local user.name "${GPG_USER_NAME}"
git config --local user.email "${GPG_USER_EMAIL}"
