
# Apollo GraphQL Client for Android and the JVM

[![GitHub license](https://img.shields.io/badge/license-MIT-lightgrey.svg?maxAge=2592000)](https://raw.githubusercontent.com/apollographql/apollo-android/master/LICENSE) [![Join Spectrum](https://img.shields.io/badge/spectrum-join-orange)](https://spectrum.chat/apollo/apollo-android)
![CI](https://github.com/apollographql/apollo-android/workflows/CI/badge.svg)
[![GitHub release](https://img.shields.io/github/release/apollographql/apollo-android.svg)](https://github.com/apollographql/apollo-android/releases/latest)

Apollo-Android is a GraphQL client that generates Java and Kotlin models from GraphQL queries. These models give you a typesafe API to work with GraphQL servers.  Apollo will help you keep your GraphQL query statements together, organized, and easy to access. Change a query and recompile your project - Apollo code gen will rebuild your data model.  Code generation also allows Apollo to read and unmarshal responses from the network without the need of any reflection.

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
* Support for RxJava2, RxJava3, and Coroutines.

## Adding Apollo-Android to your Project

The latest Gradle plugin version is [ ![Download](https://api.bintray.com/packages/apollographql/android/apollo/images/download.svg) ](https://bintray.com/apollographql/android/apollo-gradle-plugin/_latestVersion)

In your app Gradle file, apply the `com.apollographql.apollo` plugin and add the Apollo dependencies:

```groovy
// app/build.gradle or app/build.gradle.kts
plugins {
  id("com.apollographql.apollo").version("x.y.z")
}

repositories {
  jcenter()
}

dependencies {
  implementation("com.apollographql.apollo:apollo-runtime:x.y.z")
  
  // optional: if you want to use the normalized cache
  implementation("com.apollographql.apollo:apollo-normalized-cache-sqlite:x.y.z")
  // optional: for coroutines support
  implementation("com.apollographql.apollo:apollo-coroutines-support:x.y.z")
  // optional: for RxJava3 support  
  implementation("com.apollographql.apollo:apollo-rx3-support:x.y.z")
}
```

## Downloading a schema.json file

Apollo-Android requires an introspection schema. You can get a schema.json file by running an introspection query on your endpoint. The Apollo Gradle plugin exposes a `downloadApolloSchema` task to help with this. You can download a schema by specifying your endpoint and the location where you want the schema to be downloaded:

```bash
./gradlew downloadApolloSchema --endpoint=https://your.graphql.endpoint --schema=app/src/main/graphql/com/example/schema.json
```

If your endpoint requires authentication, you can pass custom HTTP headers:

```
./gradlew downloadApolloSchema --endpoint="https://your.graphql.endpoint" --schema="app/src/main/graphql/com/example" --header="Authorization: Bearer $TOKEN"
```

## Generating models from your queries

1) Create a directory for your GraphQL files like you would do for Java/Kotlin: `src/main/graphql/com/example/`. Apollo-Android will generate models in the `com.apollographql.apollo.sample` package.
2) Add your `schema.json` to the directory at `src/main/graphql/com/example/schema.json`. If you don't have a `schema.json` file yet, read the section about [downloading a schema file](#downloading-a-schemajson-file). 
3) Put your GraphQL queries in a `.graphql` files. For an example: `src/main/graphql/com/example/LaunchDetails.graphql`: 

```graphql
query LaunchDetails($id:ID!) {
  launch(id: $id) {
    id
    site
    mission {
      name
      missionPatch(size:LARGE)
    }
  }
}
```

4) Decide if you want to generate Kotlin or Java models:

```groovy
// app/build.gradle or app/build.gradle.kts
apollo {
  generateKotlinModels.set(true) // or false for Java models
}
```

5) Execute `./gradlew generateApolloSources` to generate the models from your queries. This will create a generated `LaunchDetailsQuery` Java or Kotlin source file for your query.

## Executing queries

Apollo includes an `ApolloClient` to interact with your server and cache.

To make a query using the generated models:

```kotlin
val apolloClient = ApolloClient.builder()
                .serverUrl("https://")
                .build()

            apolloClient.query(LaunchDetailsQuery(id = "83"))
                .enqueue(object : ApolloCall.Callback<LaunchDetailsQuery.Data>() {
                    override fun onFailure(e: ApolloException) {
                        Log.e("Apollo", "Error", e)
                    }

                    override fun onResponse(response: Response<LaunchDetailsQuery.Data>) {
                        Log.e("Apollo", "Launch site: ${response.data?.launch?.site}")
                    }
                })
```

## Custom Scalar Types

Apollo supports Custom Scalar Types like `Date`.

You first need to define the mapping in your build.gradle file. This maps from the GraphQL type to the Java/Kotlin class to use in code.

```
apollo {
  customTypeMapping = [
    "Date" : "java.util.Date"
  ]
}

or in Kotlin:
apollo {
    customTypeMapping.set(mapOf(
        "Date" to "java.util.Date"
    ))
}
```

Next register your custom adapter & add it to your Apollo Client Builder:

```kotlin
    val dateCustomTypeAdapter = object : CustomTypeAdapter<Date> {
         override fun decode(value: CustomTypeValue<*>): Date {
             return try {
                 DATE_FORMAT.parse(value.value.toString())
             } catch (e: ParseException) {
                 throw RuntimeException(e)
             }
         }
    
         override fun encode(value: Date): CustomTypeValue<*> {
             return GraphQLString(DATE_FORMAT.format(value))
         }
     }
    
     ApolloClient.builder()
         .serverUrl(serverUrl)
         .addCustomTypeAdapter(CustomType.DATE, dateCustomTypeAdapter)
         .build()
```

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

## Advanced topics

Advanced topics are available in [the official docs](https://www.apollographql.com/docs/android/):

* [caching.md](https://www.apollographql.com/docs/android/essentials/caching/)  
* [plugin-configuration.md](https://www.apollographql.com/docs/android/essentials/plugin-configuration/) 
* [android.md](https://www.apollographql.com/docs/android/advanced/android/) 
* [file-upload.md](https://www.apollographql.com/docs/android/advanced/file-upload/)
* [coroutines.md](https://www.apollographql.com/docs/android/advanced/coroutines/) 
* [rxjava2.md](https://www.apollographql.com/docs/android/advanced/rxjava2/)
* [rxjava3.md](https://www.apollographql.com/docs/android/advanced/rxjava3/)
* [persisted-queries.md](https://www.apollographql.com/docs/android/advanced/persisted-queries/)
* [no-runtime.md](https://www.apollographql.com/docs/android/advanced/no-runtime/) 
* [subscriptions.md](https://www.apollographql.com/docs/android/advanced/subscriptions/)
* [migrations.md](https://www.apollographql.com/docs/android/essentials/migration/)

## Contributing

If you'd like to contribute, please refer to the [Contributing.md](https://github.com/apollographql/apollo-android/blob/master/Contributing.md).

## License

```
The MIT License (MIT)

Copyright (c) 2020 Meteor Development Group, Inc.
```
