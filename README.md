# Apollo GraphQL Client for Android

[![GitHub license](https://img.shields.io/badge/license-MIT-lightgrey.svg?maxAge=2592000)](https://raw.githubusercontent.com/apollographql/apollo-android/master/LICENSE) [![Get on Slack](https://img.shields.io/badge/slack-join-orange.svg)](http://www.apollostack.com/#slack)
[![Build status](https://travis-ci.org/apollographql/apollo-android.svg?branch=master)](https://travis-ci.org/apollographql/apollo-android)

Apollo-Android is a GraphQL compliant client that generates Java models from standard GraphQL queries.  These models give you a typesafe API to work with GraphQL servers.  Apollo will help you keep your GraphQL query statements together, organized, and easy to access from Java. Change a query and recompile your project - Apollo code gen will rebuild your data model.  Code generation also allows Apollo to read and unmarshal responses from the network without the need of any reflection (see example generated code below).  Future versions of Apollo-Android will also work with AutoValue and other value object generators.

## Adding Apollo to your Project
Add the following to your project's top level `build.gradle` file 

```groovy

buildscript {

    ext.apollo_version = '0.2.2'
    ext.kotlin_version = '1.1.0'

    repositories {
        jcenter()
        mavenCentral()
        maven { url "https://jitpack.io" }
        maven { url "https://oss.sonatype.org/content/repositories/snapshots" }
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:2.3.0'
        classpath "com.apollographql.android:gradle-plugin:$apollo_version"
    }
}

allprojects {
    repositories {
        jcenter()
        mavenCentral()
        maven { url "https://jitpack.io" }
        maven { url "https://oss.sonatype.org/content/repositories/snapshots" }
    }
}

```

within your app module's `build.gradle` add the following:

```
apply plugin: 'com.android.application'
apply plugin: 'com.apollographql.android'

dependencies {
    
    compile "com.apollographql.android:runtime:$apollo_version"

}
```

## Generate Code using Apollo

Follow these steps:

1) Put your GraphQL queries in a `.graphql` file. In the sample project you can find in this repo you can find the graphql file at `apollo-sample/src/main/graphql/com/example/GithuntFeedQuery.graphql`. 

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

Note: There is nothing Android specific about this query, it can be shared with other GraphQL clients as well

2) You will also need to add a schema to the project. In the sample project you can find the schema `apollo-sample/src/main/graphql/com/example/schema.json`. 

You can find instructions to download your schema using apollo-codegen [HERE](http://dev.apollodata.com/ios/downloading-schema.html)

3) Sync Gradle to have Apollo generate the approriate Java classes with nested classes for reading from the network response. In the sample project, a `FeedQuery` Java class is created here `apollo-sample/build/generated/source/apollo/com/example`.

Note: This is a file that Apollo generates and therefore should not be mutated.


## Consuming Code

You can use the generated classes to make requests to your GraphQL API.  Apollo includes a `ApolloClient` that allows you to edit networking options like pick the base url for your GraphQL Endpoint.

In our sample project, we have the base url pointing to `https://githunt-api.herokuapp.com/graphql`

There is also a #newCall instance method on ApolloClient that can take as input any Query or Mutation that you have generated using Apollo.

```java
apolloClient.newCall(FeedQuery.builder()
                .limit(10)
                .type(FeedType.HOT)
                .build()).enqueue(new ApolloCall.Callback<FeedQuery.Data>() {

            @Override public void onResponse(@Nonnull Response<FeedQuery.Data> dataResponse) {

                final StringBuffer buffer = new StringBuffer();
                for (FeedQuery.Data.Feed feed : dataResponse.data().feed()) {
                    buffer.append("name:" + feed.repository().fragments().repositoryFragment().name());
                    buffer.append(" owner: " + feed.repository().fragments().repositoryFragment().owner().login());
                    buffer.append(" postedBy: " + feed.postedBy().login());
                    buffer.append("\n~~~~~~~~~~~");
                    buffer.append("\n\n");
                }

                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override public void run() {
                        TextView txtResponse = (TextView) findViewById(R.id.txtResponse);
                        txtResponse.setText(buffer.toString());
                    }
                });

            }

            @Override public void onFailure(@Nonnull Throwable t) {
                Log.e(TAG, t.getMessage(), t);
            }
        });
 ```

Note: Results are returing on a background Thread. If you want to make UI updates make sure they are done on the Main Thread.

## Custom Scalar Types

Apollo supports Custom Scalar Types like `DateTime` for an example.

You first need to define the mapping in your build.gradle file. This will tell the compiler what type to use when generating the classes.

```
apollo {
    generateClasses = true
    customTypeMapping {
        DateTime = "java.util.Date"
    }
}
```

Then register your custom adapter:

```
CustomTypeAdapter<Date> dateCustomTypeAdapter = new CustomTypeAdapter<Date>() {
    @Override
    public Date decode(String value) {
        try {
            return ISO8601_DATE_FORMAT.parse(value);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public String encode(Date value) {
        return ISO8601_DATE_FORMAT.format(value);
    }
};

ApolloConverterFactory apolloConverterFactory = new ApolloConverterFactory.Builder()
        .withCustomTypeAdapter(CustomType.DATETIME, dateCustomTypeAdapter)
        .withResponseFieldMappers(ResponseFieldMappers.MAPPERS)
        .build();
```

## Generate interfaces instead of classes

You can have the compiler generate interfaces instead of classes if it's easier to integrate with your existing framework:. More on that soon.

```
{
  generateClasses=false
}
```

## License

```
The MIT License (MIT)

Copyright (c) 2016 Meteor Development Group, Inc.
```
