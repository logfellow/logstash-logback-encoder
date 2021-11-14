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

To perform a release, push a commit ([like this one](https://github.com/logfellow/logstash-logback-encoder/commit/aa942e9fe59320fa1b39f1b54f8a742dd8fd9930))
to the `main` branch that:

1. Bumps the version references in the README.md
2. Contains a commit message that starts with `[release]`
    
The [build workflow](.github/workflows/build.yml) sees `[release]` in the commit message
and uses the `maven-release-plugin` to perform the release, which then:

1. Strips the `-SNAPSHOT` from the pom version,
2. Creates the git tag, and builds the artifacts
3. Uses the `maven-gpg-plugin` to sign the artifacts with [this GPG signing key](http://keyserver.ubuntu.com/pks/lookup?search=0x794038C5C4DF6A3F&fingerprint=on&op=index)
   using the [`GPG_KEY` and `GPG_PASSPHRASE` secrets](https://github.com/logfellow/logstash-logback-encoder/settings/secrets/actions)
4. Uses the `nexus-staging-maven-plugin` to:
   1. Deploy the artifact to a staging repository hosted at https://oss.sonatype.org/
      using the [`OSSRH_USERNAME` and `OSSRH_PASSWORD` secrets](https://github.com/logfellow/logstash-logback-encoder/settings/secrets/actions)
   2. Automatically [release](https://central.sonatype.org/pages/releasing-the-deployment.html) the staging repository if no errors occur.
      * After the staging repository is released, the new artifacts will eventually propagate to maven central. 
5. Bumps the version to the next `-SNAPSHOT` version.

After releasing, create a [release](https://github.com/logfellow/logstash-logback-encoder/releases) for the tag
that includes release notes of all the changes in the new version.
