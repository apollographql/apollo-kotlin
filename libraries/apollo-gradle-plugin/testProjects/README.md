# testProjects

This directory contains projects used by the plugin tests.

You can find other files in [../testFiles](../src/testFiles). These projects are programmatically constructed, but it becomes harder and harder to maintain so new test projects should be added here. It will duplicate some files and schemas but git should do the correct thing and only store objects once.

They expect a plugin and other dependencies to be published in apollo-kotlin/build/localMaven.

The tests will be run with the `:libraries:apollo-gradle-plugin:allTests` tasks. You can also debug individual projects by running gradle tasks manually:

    ../../gradlew -p $projectName $taskName.


