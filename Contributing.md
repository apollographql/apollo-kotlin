Contributing to Apollo Android GraphQL 
======================================

The Apollo team welcomes contributions of all kinds, including bug reports, documentation, test cases, bug fixes, and features.

If you have a usage question, please ask on [Stack Overflow](https://stackoverflow.com/) using the tag `apollo-android`.

Project Setup
-------------

This project is developed using either IntelliJ IDEA or Android Studio. To build multiplatform projects, you need MacOS and the Xcode developer tools.
 
To build the integration tests, use the `tests` build. It's a composite build that includes the main build so that it's possible to use `apollo-gradle-plugin` with dependency substitution.

DOs and DON'Ts
--------------

### DO:

* Follow our [coding style](#coding-style)
* Add labels to your issues and pull requests (at least one label for each of Status/Type/Priority).
* Give priority to the current style of the project or file you're changing, even if it diverges from the general guidelines.
* Include tests when adding new features. When fixing bugs, start with adding a test that highlights how the current behavior is broken.
* Keep the discussions focused. When a new or related topic comes up, it's often better to create a new issue than to side track the discussion.
* Run all Gradle verification tasks (`./gradlew check`) before submitting a pull request.
* Run `./gradlew apiDump` when changing the public API so that API compatibility can be enforced.

### DON'T:

* Send PRs for style changes.
* Surprise us with big pull requests. Instead, file an issue and start a discussion so we can agree on a direction before you invest a large amount of time.
* Commit code that you didn't write. If you find code that you think is a good fit, file an issue and start a discussion before proceeding.
* Submit PRs that alter licensing related files or headers. If you believe there's a problem with them, file an issue and we'll be happy to discuss it.


Coding Style
------------

The coding style employed here is fairly conventional Kotlin - indentations are 2 spaces, class
names are PascalCased, identifiers and methods are camelCased.    

Workflow
--------

We love Github issues!  Before working on any new features, please open an issue so that we can agree on the
direction, and hopefully avoid investing a lot of time on a feature that might need reworking.

Small pull requests for things like typos, bugfixes, etc are always welcome.

Please note that we will not accept pull requests for style changes.

Releasing
--------

To create a new tag, use the script:
```bash
scripts/tag.main.kts <version-name>
```
and then push it.

The CI contains credentials and will push artifacts to sonatype/gradlePortal when a tag is pushed. Every tag will trigger a new release.

After a successful release, do not forget to add a changelog to the [releases page](https://github.com/apollographql/apollo-android/releases).
