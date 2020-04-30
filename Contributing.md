Contributing to Apollo Android GraphQL 
======================================

The Apollo team welcomes contributions of all kinds, from simple bug reports through documentation, test cases,
bugfixes, and features.

If you instead have a usage question kindly ask on Stackoverflow.com using the tag [apollo-android]

Project Setup
-------------

This project is being developed using IntelliJ IDEA or Android Studio. With [introduction of Kotlin Multiplatform](https://github.com/apollographql/apollo-android/blob/master/apollo-api/build.gradle.kts#L10-L21),
build may require installation of xcodebuild tools and Xcode 11.
 
It is recommended to import/open `composite` instead of root folder since that includes `apollo-integration` tests
and all sample projects under `samples` folder by using Gradle Composite builds.

DOs and DON'Ts
--------------

* DO follow our coding style (as described below)
* Do add labels to your issues and pull requests:  At least 1 for each Status/Type/Priority
* DO give priority to the current style of the project or file you're changing even if it diverges from the general guidelines.
* DO include tests when adding new features. When fixing bugs, start with adding a test that highlights how the current behavior is broken.
* DO keep the discussions focused. When a new or related topic comes up it's often better to create new issue than to side track the discussion.
* DO run all Gradle verification tasks (`./gradlew check`) before submitting a pull request

* DO NOT send PRs for style changes.
* DON'T surprise us with big pull requests. Instead, file an issue and start a discussion so we can agree on a direction before you invest a large amount of time.
* DON'T commit code that you didn't write. If you find code that you think is a good fit, file an issue and start a discussion before proceeding.
* DON'T submit PRs that alter licensing related files or headers. If you believe there's a problem with them, file an issue and we'll be happy to discuss it.


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

The CI contains credentials and will push artifacts to bintray/sonatype/gradlePortal when a tag is pushed. Every tag will trigger a new release.

After a successful release, do not forget to add a changelog to the [releases page](https://github.com/apollographql/apollo-android/releases).
