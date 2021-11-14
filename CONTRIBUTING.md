Contributing to logstash-logback-encoder
========================================

* [Code of Conduct](#code-of-conduct)
* [How to Contribute](#how-to-contribute)
  * [Discuss](#discuss)
  * [Create an Issue](#create-an-issue)
  * [Submit a Pull Request](#submit-a-pull-request)
  * [Participate in Reviews](#participate-in-reviews)
  * [Become a Maintainer](#become-a-maintainer)
* [Build from Source](#build-from-source)
* [Source Code Style](#source-code-style)
* [License](#license)

Code of Conduct
---------------

[![Contributor Covenant](https://img.shields.io/badge/Contributor%20Covenant-v2.0%20adopted-ff69b4.svg)](CODE_OF_CONDUCT.md)

How to Contribute
-----------------

### Discuss

If you have a question, check Stack Overflow using the
[logstash-logback-encoder](https://stackoverflow.com/questions/tagged/logstash-logback-encoder) tag.

If you believe there is an issue, search through
[existing issues](https://github.com/logfellow/logstash-logback-encoder/issues) trying a
few different ways to find discussions, past or current, that are related to the issue.
Reading those discussions helps you to learn about the issue, and helps us to make a decision.

### Create an Issue

Reporting an issue or making a feature request is a great way to contribute.
Your feedback and the conversations that result from it provide a continuous flow of ideas.
However, before creating an issue, please take the time to [discuss and research](#discuss) first.

If creating an issue after a discussion on Stack Overflow, please provide a description
in the issue instead of simply referring to Stack Overflow.
The issue tracker is an important place of record for design discussions and should be self-sufficient.

Once you're ready, create an [issue](https://github.com/logfellow/logstash-logback-encoder/issues).

### Submit a Pull Request

1. Should you create an issue first? No, just create the pull request and use the
   description to provide context and motivation, as you would for an issue. If you want
   to start a discussion first or have already created an issue, once a pull request is
   created, we will close the issue as superseded by the pull request, and the discussion
   about the issue will continue under the pull request.

2. Always check out the `main` branch and submit pull requests against it.

3. Choose the granularity of your commits consciously and squash commits that represent
   multiple edits or corrections of the same logical change. See
   [Rewriting History section of Pro Git](https://git-scm.com/book/en/Git-Tools-Rewriting-History)
   for an overview of streamlining the commit history.

4. Format commit messages using 55 characters for the subject line, 72 characters per line
   for the description, followed by the issue fixed, e.g. `Fixes #351`. See the
   [Commit Guidelines section of Pro Git](https://git-scm.com/book/en/Distributed-Git-Contributing-to-a-Project#Commit-Guidelines)
   for best practices around commit messages.

If accepted, your contribution might be heavily modified as needed prior to merging.
You will likely retain author attribution for your Git commits granted that the bulk of
your changes remain intact. You may also be asked to rework the submission.

If asked to make corrections, simply push the changes against the same branch, and your
pull request will be updated. In other words, you do not need to create a new pull request
when asked to make changes.

### Participate in Reviews

Helping to review pull requests is another great way to contribute. Your feedback
can help to shape the implementation of new features. When reviewing pull requests,
however, please refrain from approving or rejecting a PR unless you are a core
committer for logstash-logback-encoder.

### Become a Maintainer

If you are interested in becoming a maintainer, show your support by making contributions
using any of the above means.  After you have been involved in the project for a while,
you may request to become a maintainer by filing an issue.

Build from Source
-----------------

JDK 8 is required to build from source.

Use `mvnw` (\*nix) or `mvnw.cmd` (windows) to build.

```
./mvnw clean install
```

Source Code Style
-----------------

Please follow the style used by the existing code in the repository.
Rules are enforced by [checkstyle](src/checkstyle/checkstyle.xml).

Java source files must include the following header at the top of the file, before the _package_ declaration:

```
/*
 * Copyright 2013-${year} the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
```

The project makes use of the great [Mycila Maven License Plugin](https://github.com/mathieucarbou/license-maven-plugin) to check for the presence of a valid header in source files during the build process.

You can manually check your source files by invoking the plugin manually on the command line as follows:

```
mvn license:check
```

You can also ask the plugin to automatically update the header for you like this:

```
mvn license:format
```


License
-------

By contributing, you agree that the contributions will be licensed under the
[Apache License 2.0](https://github.com/logfellow/logstash-logback-encoder/blob/main/LICENSE).

