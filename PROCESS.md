Development Processes
=====================

This page covers the processes used by the maintainers of logstash-logback-encoder.

* [Branching Strategy](#branching-strategy)
* [Version Support](#version-support)
* [Releasing](#releasing)

Branching Strategy
------------------

The `master` branch is the head for the next release.

The project version referenced in the `pom.xml` file on the `master` branch is
the next release version appended with `-SNAPSHOT`.

PRs are submitted against `master`.


Version Support
---------------

Only the latest version is "supported" and actively maintained.

All changes and improvements are made against the latest version.

In other words, older releases will not be hotfixed, and backports will not be performed to older releases.


Releasing
---------

To perform a release, push a commit ([like this one](https://github.com/logstash/logstash-logback-encoder/commit/aa942e9fe59320fa1b39f1b54f8a742dd8fd9930))
to the `master` branch that:

1. Bumps the version references in the README.md
2. Contains a commit message that starts with `[release]`
    
The [build workflow](.github/workflows/build.yml) sees `[release]` in the commit message
and uses the `maven-release-plugin` to perform the release.
The `maven-release-plugin` strips the `-SNAPSHOT` from the pom version,
performs the release, and bumps the version to the next `-SNAPSHOT` version.

During the release process, the `nexus-staging-maven-plugin` deploys the artifact to
a staging repository hosted at https://oss.sonatype.org/,
and automatically [releases](https://central.sonatype.org/pages/releasing-the-deployment.html)
the staging repository if no errors occur.
After the staging repository is released, the new artifacts will eventually propagate to maven central.

After releasing, create a [release](https://github.com/logstash/logstash-logback-encoder/releases) for the tag
that includes release notes of all the changes in the new version.
