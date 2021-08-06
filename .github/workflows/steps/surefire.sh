#!/bin/bash

# Post-process maven-surefire-plugin report files for consumption by "scacap/action-surefire-report" 
# Gitthub action.
#
# The post processing copies <system-out> into the <failure> message so it is available

set -e

# Fast exit
[ -d target/surefire-reports ] || exit 0

# Create targt directory with post-processed files
mkdir -p target/surefire-reports-github

# Apply XSLT transformation to XML report files
XSLT=$(dirname $0)/surefire.xslt
for f in $(find target/surefire-reports -name '*.xml'); do
	xsltproc $XSLT $f > target/surefire-reports-github/$(basename $f)
done

# Copy othr files (witthout override of XML files already processed by the previous step)
cp -an target/surefire-reports/ target/surefire-reports-github
