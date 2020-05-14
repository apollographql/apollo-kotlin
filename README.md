
# Apollo GraphQL Client for Android and the JVM

[![GitHub license](https://img.shields.io/badge/license-MIT-lightgrey.svg?maxAge=2592000)](https://raw.githubusercontent.com/apollographql/apollo-android/master/LICENSE) [![Join Spectrum](https://img.shields.io/badge/spectrum-join-orange)](https://spectrum.chat/apollo/apollo-android)
![CI](https://github.com/apollographql/apollo-android/workflows/CI/badge.svg)
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

The latest Gradle plugin version is [ ![Download](https://api.bintray.com/packages/apollographql/android/apollo/images/download.svg) ](https://bintray.com/apollographql/android/apollo-gradle-plugin/_latestVersion)

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
./gradlew :module:downloadApolloSchema -Pcom.apollographql.apollo.endpoint=https://your.graphql.endpoint -Pcom.apollographql.apollo.schema=src/main/graphql/com/example/schema.json
```

If your endpoint requires authentication, you can pass query parameters and/or custom HTTP headers:

```
./gradlew :module:downloadApolloSchema -Pcom.apollographql.apollo.endpoint=https://your.graphql.endpoint -Pcom.apollographql.apollo.schema=src/main/graphql/com/example/schema.json  "-Pcom.apollographql.apollo.headers=Authorization=Bearer YOUR_TOKEN" "-Pcom.apollographql.apollo.query_params=key1=value1&key2=value2"
```

The `com.apollographql.apollo.headers` and `com.apollographql.apollo.query_params` properties both take a query string where key and values should be URL encoded.

The default timeout for download operation is 1 minute. If you have a large `schema.json`, you may want to increase the timeout. Do that by adding the following into `gradle.properties`:
```
org.gradle.jvmargs=-DokHttp.connectTimeout=60 -DokHttp.readTimeout=60
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
* [persisted-queries.md](https://www.apollographql.com/docs/android/advanced/persisted-queries/)
* [no-runtime.md](https://www.apollographql.com/docs/android/advanced/no-runtime/) 
* [subscriptions.md](https://www.apollographql.com/docs/android/advanced/subscriptions/)
* [migrations.md](https://www.apollographql.com/docs/android/essentials/migration/)


## Changelog
[Read about the latest changes to the library](https://github.com/apollographql/apollo-android/releases)

## Contributing

If you'd like to contribute, please refer to the [Apollo Contributor Guide](https://github.com/apollographql/apollo-android/blob/master/Contributing.md).

*Note:* Running samples require importing `composite` folder instead of root.

## License

```
The MIT License (MIT)

Copyright (c) 2019 Meteor Development Group, Inc.
```
