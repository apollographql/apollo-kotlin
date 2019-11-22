# Migrating to 1.3

The 1.3 plugin introduces several breaking changes to better deal with multiple GraphQL endpoints (services), lazy configuration and package name configuration. These changes only affect the plugin. The generated code is unchanged and your consumer code doesn't require any change but your `build.gradle` (or `build.gradle.kts`) files might.

### Plugin id is now `com.apollographql.apollo`

We renamed the plugin id from `com.apollographql.android` to `com.apollographql.apollo` to make it clear that the plugin works also for non Android projects. `com.apollographql.android` will be removed in a future revision.

```groovy
// Don't do this
apply plugin: 'com.apollographql.android'

// Do this
apply plugin: 'com.apollographql.apollo'
```
### Specifying schema and graphql files location

The root `schemaFilePath`, `outputPackageName` and `sourceSets.graphql` are removed and will throw an error if you try to use them. Instead you can use [CompilationUnit] to control what files the compiler will use as inputs.

```groovy
// Don't do this
// sourceSets {
//  main.graphql.srcDirs += "/path/to/your/graphql/queries/dir"
//}

// Do this
apollo {
  onCompilationUnits {
     graphqlSourceDirectorySet.srcDirs += "/path/to/your/graphql/queries/dir"
  }
}

// Don't do this
// apollo {
//  sourceSet {
//    schemaFilePath = "/path/to/your/schema.json"
//    exclude = "**/*.gql"
//  }
//  outputPackageName = "com.example"
//}

// Do this
apollo {
  onCompilationUnits {
     schemaFile = "/path/to/your/schema.json"
     graphqlSourceDirectorySet.exclude("**/*.gql")
     rootPackageName = "com.example"
  }
}
```

### Kotlin DSL

The plugin uses gradle [Properties](https://docs.gradle.org/current/javadoc/org/gradle/api/provider/Property.html) to support [lazy configuration](https://docs.gradle.org/current/userguide/lazy_configuration.html) and wiring tasks together.

If you're using Groovy `build.gradle` build scripts it should work transparently but Kotlin `build.gradle.kts` build scripts will require you to use the [Property.set](https://docs.gradle.org/current/javadoc/org/gradle/api/provider/Property.html#set-T-) API:

```kotlin
apollo {
  // Don't do this
  // setGenerateKotlinModels(true)

  // Do this
  generateKotlinModels.set(true)
}
```

Also, the classes of the plugin have been split between a [api](https://github.com/apollographql/apollo-android/tree/4692659508242d64882b8bff11efa7dcd555dbcc/apollo-gradle-plugin-incubating/src/main/kotlin/com/apollographql/apollo/gradle/api) part and an [internal](https://github.com/apollographql/apollo-android/tree/4692659508242d64882b8bff11efa7dcd555dbcc/apollo-gradle-plugin-incubating/src/main/kotlin/com/apollographql/apollo/gradle/internal) one. If you were relying on fully qualified class names from your `build.gradle.kts` files, you will have to tweak them:

```kotlin
// Don't do this
// import com.apollographql.apollo.gradle.ApolloExtension

// Never rely on internal classes, they might change without warning
// import com.apollographql.apollo.gradle.internal.DefaultApolloExtension

// Do this
import com.apollographql.apollo.gradle.api.ApolloExtension
```

### Using multiple services

The plugin now requires that you specify multiple services explicitely. If you previously had the following layout:

```bash
src/main/graphql/com/github/schema.json
src/main/graphql/com/github/GetRepositories.graphql
src/main/graphql/com/starwars/schema.json
src/main/graphql/com/starwars/GetHeroes.graphql
```

You will need to define 2 services:

```kotlin
apollo {
  service("github") {
    sourceFolder.set("com.github")
    rootPackageName.set("com.github")
  }
  service("starwars") {
    sourceFolder.set("com.starwars")
    rootPackageName.set("com.starwars")
  }
}
```

