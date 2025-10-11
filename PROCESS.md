Development Processes
=====================

This page covers the processes used by the maintainers of logstash-logback-encoder.

* [Branching Strategy](#branching-strategy)
* [Version Support](#version-support)
* [Releasing](#releasing)

Branching Strategy
------------------

The `main` branch is the head for the next release.

The project version referenced in the `pom.xml` file on the `main` branch is
the next release version appended with `-SNAPSHOT`.

PRs are submitted against `main`.


Version Support
---------------

Only the latest version is "supported" and actively maintained.

All changes and improvements are made against the latest version.

In other words, older releases will not be hotfixed, and backports will not be performed to older releases.


Releasing
---------

To perform a release, first, prepare the `main` branch:
1. Set the version in the [pom.xml](pom.xml) to `X.Y-SNAPSHOT`, where `X.Y` is the version to be released. 
2. Bump the version references in the [README.md](README.md)

Then trigger the https://github.com/logfellow/logstash-logback-encoder/actions/workflows/release.yml on the main branch.
    
The [release workflow](.github/workflows/release.yml) triggers a maven release via the `maven-release-plugin`, which then:

1. Strips the `-SNAPSHOT` from the pom version,
2. Creates the git tag, and builds the artifacts
3. Uses the `maven-gpg-plugin` to sign the artifacts with [this GPG signing key](http://keyserver.ubuntu.com/pks/lookup?search=0x794038C5C4DF6A3F&fingerprint=on&op=index)
   using the [`GPG_KEY` and `GPG_PASSPHRASE` secrets](https://github.com/logfellow/logstash-logback-encoder/settings/secrets/actions)
4. Uses the `central-publishing-maven-plugin` to:
   1. Upload the artifact to https://central.sonatype.com/
      using the [`CENTRAL_USERNAME` and `CENTRAL_PASSWORD` secrets](https://github.com/logfellow/logstash-logback-encoder/settings/secrets/actions)
   2. Automatically [publish](https://central.sonatype.org/pages/releasing-the-deployment.html) to maven central if no errors occur.
5. Bumps the version to the next `-SNAPSHOT` version.

After releasing, create a [release](https://github.com/logfellow/logstash-logback-encoder/releases) for the tag
that includes release notes of all the changes in the new version.
