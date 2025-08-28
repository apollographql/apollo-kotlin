# Module apollo-gradle-plugin-tasks

`apollo-gradle-plugin-tasks` contains the task implementations for the Apollo Gradle plugin.

This module is loaded in a separate classloader to allow compiler plugins and avoid polluting the main build script classpath. This module is an implementation detail of `apollo-gradle-plugin` and should not be depended on directly.
