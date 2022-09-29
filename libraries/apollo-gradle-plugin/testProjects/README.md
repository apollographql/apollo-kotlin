# testProjects

This directory contains projects used by the plugin tests.

You can find other files in [../src/test/files](../src/test/files). These projects are programmatically constructed, but it becomes harder and harder to maintain so new test projects should be added here. It will duplicate some files and schemas but git should do the correct thing and only store objects once.

They read the versions from ../../../dependencies.gradle and expect a plugin to be published in ../../../build/localMaven.

The tests will be run with the `:apollo-gradle-plugin:test` tasks. You can also debug individual projects by running gradle tasks manually:

    ../../gradlew -p $projectName $taskName.


