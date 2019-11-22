
# Apollo GraphQL Client for Android and the JVM

[![GitHub license](https://img.shields.io/badge/license-MIT-lightgrey.svg?maxAge=2592000)](https://raw.githubusercontent.com/apollographql/apollo-android/master/LICENSE) [![Join Spectrum](https://img.shields.io/badge/spectrum-join-orange)](https://spectrum.chat/apollo/apollo-android)
[![Build status](https://travis-ci.org/apollographql/apollo-android.svg?branch=master)](https://travis-ci.org/apollographql/apollo-android)
[![GitHub release](https://img.shields.io/github/release/apollographql/apollo-android.svg)](https://github.com/apollographql/apollo-android/releases/latest)

Apollo-Android is a GraphQL compliant client that generates Java and Kotlin models from standard GraphQL queries.  These models give you a typesafe API to work with GraphQL servers.  Apollo will help you keep your GraphQL query statements together, organized, and easy to access. Change a query and recompile your project - Apollo code gen will rebuild your data model.  Code generation also allows Apollo to read and unmarshal responses from the network without the need of any reflection.

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

Then add the dependencies to your app's build.gradle and apply file and apply the `com.apollographql.android` plugin:

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

**NOTE: Apollo Gradle plugin requires Gradle 5.6 or higher.**

## Downloading a schema.json file

You can get a schema.json file by running an introspection query on your endpoint. Else, there the Apollo plugin has a `downloadApolloSchema` that you can use:

```bash
./gradlew downloadApolloSchema -Pcom.apollographql.apollo.schema=/path/to/your/schema.json -Pcom.apollographql.apollo.endpoint=https://your.graphq.enpoint
```

If your endpoint requires authentification, you can use the following parameters:

* `-Pcom.apollographql.apollo.query_params=params`: Will add the given params to the query string. Params must be a query string (e.g. key1=value1&key2=value2, where key and values are urlencoded)
* `-Pcom.apollographql.apollo.headers=params`: This is the same as query_params except it will use HTTP headers. Params must also be a query string.

## Generating models from your queries

1) Create a directory for your GraphQL files like you would do for Java/Kotlin: `src/main/graphql/com/com/example/`. Apollo-Android will generate models in the `com.apollographql.apollo.sample` package.
2) Add your `schema.json` to the directory at `src/main/graphql/com/example/schema.json`.
3) Put your GraphQL queries in a `.graphql` files. For an exemple: `src/main/graphql/com/example/feed.graphql`: 

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
apollo {
  generateKotlinModels.set(true) // or false
}
```

5) Execute `./gradlew :generateApolloClasses` to generate the models from your queries. This will create a generated `FeedQuery` Java or Kotlin source file for your query.

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

## Migrating to Apollo-Android plugin 1.3

If you're updating to version 1.3, you might need to update your build scripts. Read [doc/migrating.md](doc/migrating.md) for more details.


## Intellij Plugin

The [JS Graphql Intellij Plugin](https://jimkyndemeyer.github.io/js-graphql-intellij-plugin/) provides auto-completion, error highlighting, and go-to-definition functionality for your graphql files. You can create a [.graphqlconfig](https://jimkyndemeyer.github.io/js-graphql-intellij-plugin/docs/developer-guide#working-with-graphql-endpoints-and-scratch-files) file in order to use GraphQL scratch files to work with your schema outside product code, e.g. by writing temporary queries to test resolvers.

## Releases

Our [change log](CHANGELOG.md) has the release history. 

Releases are hosted on [jcenter](https://jcenter.bintray.com/com/apollographql/apollo/).

Latest development changes are available in Sonatype's snapshots repository:

```
  repositories {
    maven { url 'https://oss.sonatype.org/content/repositories/snapshots/' }
  }
```

## Advanced topics

Advanced topics are available in the [doc folder](doc):

* [doc/caching.md](doc/caching.md)
* [doc/plugin-configuration.md](doc/plugin-configuration.md) 
* [doc/android.md](doc/android.md) 
* [doc/file-upload.md](doc/file-upload.md)
* [doc/rxjava2.md](doc/rxjava2.md)
* [doc/coroutines.md](doc/coroutines.md) 
* [doc/persisted-queries.md](doc/no-runtime.md)
* [doc/no-runtime.md](doc/no-runtime.md) 
* [doc/subscriptions.md](doc/subscriptions.md) 

## License

```
The MIT License (MIT)

Copyright (c) 2017 Meteor Development Group, Inc.
```
