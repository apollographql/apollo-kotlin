
# Apollo GraphQL Client for Android and the JVM

[![GitHub license](https://img.shields.io/badge/license-MIT-lightgrey.svg?maxAge=2592000)](https://raw.githubusercontent.com/apollographql/apollo-android/master/LICENSE) [![Join Spectrum](https://img.shields.io/badge/spectrum-join-orange)](https://spectrum.chat/apollo/apollo-android)
[![Build status](https://travis-ci.org/apollographql/apollo-android.svg?branch=master)](https://travis-ci.org/apollographql/apollo-android)
[![GitHub release](https://img.shields.io/github/release/apollographql/apollo-android.svg)](https://github.com/apollographql/apollo-android/releases/latest)

Apollo-Android is a GraphQL compliant client that generates Java and Kotlin models from standard GraphQL queries. These models give you a typesafe API to work with GraphQL servers.  Apollo will help you keep your GraphQL query statements together, organized, and easy to access. Change a query and recompile your project - Apollo code gen will rebuild your data model.  Code generation also allows Apollo to read and unmarshal responses from the network without the need of any reflection.

Apollo-Android is designed primarily with Android in mind but you can use it in any Java/Kotlin app. The android-only parts are in `apollo-android-support` and are only needed to use SQLite as a cache or the android main thread for callbacks.

Apollo-android features:

* Automatic generation of typesafe models.
* Support for Java and Kotlin code generation.
* Queries, Mutations and Subscriptions.
* Reflection-free parsing of responses.
* HTTP cache.
* Normalized cache.
* File Upload.
* Custom scalar types.
* Support for RxJava2 and Coroutines. 

## Adding Apollo-Android to your Project

The latest Gradle plugin version is [ ![Download](https://api.bintray.com/packages/apollographql/android/apollo-gradle-plugin/images/download.svg) ](https://bintray.com/apollographql/android/apollo-gradle-plugin/_latestVersion)

To use this plugin, add the dependency to your project's root build.gradle file:

```groovy
buildscript {
  repositories {
    jcenter()
  }
  dependencies {
    classpath("com.apollographql.apollo:apollo-gradle-plugin:x.y.z")
  }
}
```

Then add the dependencies to your app's build.gradle and apply file and apply the `com.apollographql.apollo` plugin:

```groovy
apply plugin: 'com.apollographql.apollo'

repositories {
    jcenter()
}

dependencies {
  implementation("com.apollographql.apollo:apollo-runtime:x.y.z")
  
  // If not already on your classpath, you might need the jetbrains annotations
  compileOnly("org.jetbrains:annotations:13.0")
  testCompileOnly("org.jetbrains:annotations:13.0")
}
```

## Generating models from your queries

1) Create a directory for your GraphQL files like you would do for Java/Kotlin: `src/main/graphql/com/example/`. Apollo-Android will generate models in the `com.apollographql.apollo.sample` package.
2) Add your `schema.json` to the directory at `src/main/graphql/com/example/schema.json`. If you don't have a `schema.json` file yet, read the section about [downloading a schema file](#downloading-a-schemajson-file). 
3) Put your GraphQL queries in a `.graphql` files. For an example: `src/main/graphql/com/example/feed.graphql`: 

```
query FeedQuery($type: FeedType!, $limit: Int!) {
  feed(type: $type, limit: $limit) {
    comments {
      ...FeedCommentFragment
    }
    repository {
      ...RepositoryFragment
    }
    postedBy {
      login
    }
  }
}

fragment RepositoryFragment on Repository {
  name
  full_name
  owner {
    login
  }
}

fragment FeedCommentFragment on Comment {
  id
  postedBy {
    login
  }
  content
}
```

4) Decide if you want to generate Kotlin or Java models:

```groovy
// build.gradle or build.gradle.kts
apollo {
  generateKotlinModels.set(true) // or false for Java models
}
```

5) Execute `./gradlew generateApolloSources` to generate the models from your queries. This will create a generated `FeedQuery` Java or Kotlin source file for your query.

## Consuming Code

Apollo includes an `ApolloClient` to interact with your server and cache.

To make a query using the generated models:
```java
apolloClient.query(
  FeedQuery.builder()
    .limit(10)
    .type(FeedType.HOT)
    .build()
).enqueue(new ApolloCall.Callback<FeedQuery.Data>() {

  @Override public void onResponse(@NotNull Response<FeedQuery.Data> dataResponse) {

    final StringBuffer buffer = new StringBuffer();
    for (FeedQuery.Data.Feed feed : dataResponse.data().feed()) {
      buffer.append("name:" + feed.repository().fragments().repositoryFragment().name());
().login());
      buffer.append(" postedBy: " + feed.postedBy().login());
    }

    // onResponse returns on a background thread. If you want to make UI updates make sure they are done on the Main Thread.
    MainActivity.this.runOnUiThread(new Runnable() {
      @Override public void run() {
        TextView txtResponse = (TextView) findViewById(R.id.txtResponse);
        txtResponse.setText(buffer.toString());
      }
    });
      
  }

  @Override public void onFailure(@NotNull Throwable t) {
    Log.e(TAG, t.getMessage(), t);
  }
});       
```

## Custom Scalar Types

Apollo supports Custom Scalar Types like `Date`.

You first need to define the mapping in your build.gradle file. This maps from the GraphQL type to the Java/Kotlin class to use in code.

```groovy
apollo {
  customTypeMapping = [
    "Date" : "java.util.Date"
  ]
}
```

Next register your custom adapter & add it to your Apollo Client Builder:

```java
 dateCustomTypeAdapter = new CustomTypeAdapter<Date>() {
      @Override public Date decode(CustomTypeValue value) {
        try {
          return DATE_FORMAT.parse(value.value.toString());
        } catch (ParseException e) {
          throw new RuntimeException(e);
        }
      }

      @Override public CustomTypeValue encode(Date value) {
        return new CustomTypeValue.GraphQLString(DATE_FORMAT.format(value));
      }
    };

ApolloClient.builder()
  .serverUrl(serverUrl)
  .okHttpClient(okHttpClient)
  .addCustomTypeAdapter(CustomType.DATE, dateCustomTypeAdapter)
  .build();
```

If you have compiler warnings as errors (`options.compilerArgs << "-Xlint" << "-Werror"`)
turned on, your custom type will not compile. You can add a switch `suppressRawTypesWarning` to the
apollo plugin configuration which will annotate your generated class with the proper suppression
(`@SuppressWarnings("rawtypes")`:

```groovy
apollo {
    customTypeMapping = [
      "URL" : "java.lang.String"
    ]
    suppressRawTypesWarning = "true"
}
```

## Downloading a schema.json file

You can get a schema.json file by running an introspection query on your endpoint. The Apollo Gradle plugin exposes a `downloadApolloSchema` task to help with this. You can download a schema by specifying your endpoint and the location where you want the schema to be downloaded:

```
./gradlew :shared:downloadApolloSchema -Pcom.apollographql.apollo.endpoint=https://your.graphql.endpoint -Pcom.apollographql.apollo.schema=src/main/graphql/com/example/schema.json
```

If your endpoint requires authentication, you can pass query parameters and/or custom HTTP headers:

```
./gradlew :shared:downloadApolloSchema -Pcom.apollographql.apollo.endpoint=https://your.graphql.endpoint -Pcom.apollographql.apollo.schema=src/main/graphql/com/example/schema.json  "-Pcom.apollographql.apollo.headers=Authorization=Bearer YOUR_TOKEN" "-Pcom.apollographql.apollo.query_params=key1=value1&key2=value2"
```

The `com.apollographql.apollo.headers` and `com.apollographql.apollo.query_params` properties both take a query string where key and values should be URL encoded.

## Intellij Plugin

The [JS Graphql Intellij Plugin](https://jimkyndemeyer.github.io/js-graphql-intellij-plugin/) provides auto-completion, error highlighting, and go-to-definition functionality for your graphql files. You can create a [.graphqlconfig](https://jimkyndemeyer.github.io/js-graphql-intellij-plugin/docs/developer-guide#working-with-graphql-endpoints-and-scratch-files) file in order to use GraphQL scratch files to work with your schema outside product code, e.g. by writing temporary queries to test resolvers.

## Releases

Our [release history](https://github.com/apollographql/apollo-android/releases) has the release history. 

Releases are hosted on [jcenter](https://jcenter.bintray.com/com/apollographql/apollo/).

Latest development changes are available in Sonatype's snapshots repository:

```
  repositories {
    maven { url 'https://oss.sonatype.org/content/repositories/snapshots/' }
  }
```

## Migrating to 1.3.x

Apollo-Android version 1.3.0 introduces some fixes and improvements that are incompatible with 1.2.x. Updating should be transparent for simple use cases and your project should compile fine. If you're using more advanced features such as custom schema/graphql files location, Kotlin Gradle scripts and/or transformed queries, or if you encounter a build error after updating, read on for details about the changes.

### Gradle plugin changes

The plugin has been rewritten in Kotlin to make it more maintainable and have better support for multiple GraphQL endpoints.  Below are the main changes. Read [plugin-configuration](#configuration-reference) for a reference of the different options.

#### New plugin ID

The plugin ID has been changed from `com.apollographql.android` to `com.apollographql.apollo` to make it clear that the plugin works also for non-Android projects. `com.apollographql.android` will be removed in a future revision.

```groovy
// Replace:
apply plugin: 'com.apollographql.android'

// With:
apply plugin: 'com.apollographql.apollo'
```

#### Using multiple services

The plugin now requires that you specify multiple services explicitly. If you previously had the following layout:

```
src/main/graphql/com/github/schema.json
src/main/graphql/com/github/GetRepositories.graphql
src/main/graphql/com/starwars/schema.json
src/main/graphql/com/starwars/GetHeroes.graphql
```

You will need to define 2 services:

```kotlin
apollo {
  service("github") {
    sourceFolder.set("com/github")
    rootPackageName.set("com.github")
  }
  service("starwars") {
    sourceFolder.set("com/starwars")
    rootPackageName.set("com.starwars")
  }
}
```

#### Specifying schema and GraphQL files location

The root `schemaFilePath`, `outputPackageName` and `sourceSets.graphql` are removed and will throw an error if you try to use them. Instead you can use [CompilationUnit] to control what files the compiler will use as inputs.

```groovy
// Replace:
sourceSets {
  main.graphql.srcDirs += "/path/to/your/graphql/queries/dir"
}

// With:
apollo {
  onCompilationUnit {
     graphqlSourceDirectorySet.srcDirs += "/path/to/your/graphql/queries/dir"
  }
}

// Replace
apollo {
  sourceSet {
    schemaFilePath = "/path/to/your/schema.json"
    exclude = "**/*.gql"
  }
  outputPackageName = "com.example"
}

// With:
apollo {
  onCompilationUnit {
     schemaFile.set(file("/path/to/your/schema.json"))
     graphqlSourceDirectorySet.exclude("**/*.gql")
     rootPackageName.set("com.example")
  }
}
```

#### Kotlin DSL

The plugin uses Gradle [Properties](https://docs.gradle.org/current/javadoc/org/gradle/api/provider/Property.html) to support [lazy configuration](https://docs.gradle.org/current/userguide/lazy_configuration.html) and wiring tasks together.

If you're using Groovy `build.gradle` build scripts it should work transparently but Kotlin `build.gradle.kts` build scripts will require you to use the [Property.set](https://docs.gradle.org/current/javadoc/org/gradle/api/provider/Property.html#set-T-) API:

```kotlin
apollo {
  // Replace:
  setGenerateKotlinModels(true)

  // With:
  generateKotlinModels.set(true)
}
```

Also, the classes of the plugin have been split into an [api](https://github.com/apollographql/apollo-android/tree/4692659508242d64882b8bff11efa7dcd555dbcc/apollo-gradle-plugin-incubating/src/main/kotlin/com/apollographql/apollo/gradle/api) part and an [internal](https://github.com/apollographql/apollo-android/tree/4692659508242d64882b8bff11efa7dcd555dbcc/apollo-gradle-plugin-incubating/src/main/kotlin/com/apollographql/apollo/gradle/internal) one. If you were relying on fully qualified class names from your `build.gradle.kts` files, you will have to tweak them:

```kotlin
// Replace:
import com.apollographql.apollo.gradle.ApolloExtension

// With:
import com.apollographql.apollo.gradle.api.ApolloExtension
```

### Breaking changes in generated Kotlin models with inline fragments:

Field `inlineFragment` is no longer generated with a new Apollo **1.3.0** release for Kotlin models. 

For example:

[previous version of model with inline fragments](https://github.com/apollographql/apollo-android/blob/hotfix/1.2.3/apollo-compiler/src/test/graphql/com/example/simple_inline_fragment/TestQuery.kt#L129)

```
data class Hero(
    val __typename: String,
    /**
     * The name of the character
     */
    val name: String,
    val inlineFragment: HeroCharacter?
  ) {
    val asHuman: AsHuman? = inlineFragment as? AsHuman

    val asDroid: AsDroid? = inlineFragment as? AsDroid
...
```

[new version of generated model with inline fragments](https://github.com/apollographql/apollo-android/blob/v1.3.0/apollo-compiler/src/test/graphql/com/example/simple_inline_fragment/TestQuery.kt#L125)

```
  data class Hero(
    val __typename: String,
    /**
     * The name of the character
     */
    val name: String,
    val asHuman: AsHuman?,
    val asDroid: AsDroid?
  )
```

***Motivation***: there is an issue with previous version of generated model, there are cases when specified multiple inline fragments should be resolved for the same GraphQL type. For example imagine that GraphQL schema defines this hierarchy of types `Character <- Hero <- Human`. Having this GraphQL query:

```
query {
  character {
    name
    ... on Hero { ... }
    ... on Human { ... }
   }
}
```

both inline fragments `on Hero` and `on Human` should be resolved for character type `Human` as `Hero` is super type of `Human`. 

Previous version of generated model for `Character` didn't resolve both inline fragments but rather first declared `... on Hero`. New version resolves both fragments `on Hero` and `on Human`.

***Migration***:

If you have this code to get access to the resolved inline fragment:

```
when (hero.inlineFragment) {
    is Hero.AsHuman -> ...
    is Hero.AsDroid -> ...
}
```

you should change it to check all declared inline fragments for nullability, as it's possible now to have multiple resolved fragments:

```
if (hero.asHuman != null) {
  ...
}

if (hero.asDroid != null) {
  ...
}
```

### Singularization

Singularization rules have been improved (see [1888](https://github.com/apollographql/apollo-android/pull/1888)). That means the name of some classes that were previously wrongly or badly singularized might have changed. Check for a generated class with a similar name if that happens.

### Nested class names

Nested classes are now allowed to have the same name as their parent (see [1893](https://github.com/apollographql/apollo-android/pull/1893)). If you were previously using such a class, the numbered suffix will be removed.

### Transformed queries removal

Version 1.3.0 can now optionally generate a `OperationOutput.json` file. This file will contain the generated queries source, operation name and operation ID. You can use them to whitelist the operation on your server or any other use case. See [1841](https://github.com/apollographql/apollo-android/pull/1841) for details.

Since OperationOutput.json is a superset of the transformed queries, transformed queries have been removed. If you were using transformed queries, you will now have to use OperationOutput.json.

### Espresso Idling Resources

Idling Resources integration is moved to AndroidX! This is a potential breaking change for users who has not migrated to AndroidX yet. If you haven't you can still use the 1.2.x version in your test code.

The artifact is also renamed to make its intention more obvious. Documentation for idling resource can be found [here](https://www.apollographql.com/docs/android/advanced/android/#apolloidlingresource)

```groovy
  // Replace:
  androidTestImplementation("com.apollographql.apollo:apollo-espresso-support:x.y.z")

  // With:
  androidTestImplementation("com.apollographql.apollo:apollo-idling-resource:x.y.z")
```

## Advanced topics

Advanced topics are available in [the official docs](https://www.apollographql.com/docs/android/):

* [caching.md](https://www.apollographql.com/docs/android/essentials/caching/)  
* [plugin-configuration.md](https://www.apollographql.com/docs/android/gradle/plugin-configuration/) 
* [incubating-plugin.md](https://www.apollographql.com/docs/android/gradle/incubating-plugin/)
* [android.md](https://www.apollographql.com/docs/android/advanced/android/) 
* [file-upload.md](https://www.apollographql.com/docs/android/advanced/file-upload/)
* [coroutines.md](https://www.apollographql.com/docs/android/advanced/coroutines/) 
* [rxjava2.md](https://www.apollographql.com/docs/android/advanced/rxjava2/)
* [persisted-queries.md](https://www.apollographql.com/docs/android/advanced/persisted-queries/)
* [no-runtime.md](https://www.apollographql.com/docs/android/advanced/no-runtime/) 
* [subscriptions.md](https://www.apollographql.com/docs/android/advanced/subscriptions/)

## License

```
The MIT License (MIT)

Copyright (c) 2019 Meteor Development Group, Inc.
```
