# Module apollo-gradle-plugin

`apollo-gradle-plugin` contains the Apollo Gradle plugin.  

This module shadows and relocates its runtime dependencies to avoid classpath issues. This can make debugging harder in some cases. 

See `apollo-gradle-plugin-external` for a version of `apollo-gradle-plugin` that does not shadow its dependencies.

See ["Gradle Plugin Configuration"](https://www.apollographql.com/docs/kotlin/advanced/plugin-configuration/) for how to use the Gradle plugin.
