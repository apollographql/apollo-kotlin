# Apollo GraphQL Client for Android

[![GitHub license](https://img.shields.io/badge/license-MIT-lightgrey.svg?maxAge=2592000)](https://raw.githubusercontent.com/apollographql/apollo-android/master/LICENSE) [![Get on Slack](https://img.shields.io/badge/slack-join-orange.svg)](http://www.apollostack.com/#slack)
[![Build status](https://travis-ci.org/apollographql/apollo-android.svg?branch=master)](https://travis-ci.org/apollographql/apollo-android)

This is a Gradle plugin and set of libraries that generate Java code based on a GraphQL schema and query documents.
The plugin uses `apollo-codegen` under the hood.



## Usage

```groovy
buildscript {
  repositories {
    mavenCentral()
  }
  dependencies {
    classpath 'com.apollographql.android:gradle-plugin:0.1.0'
  }
}

apply plugin: 'com.apollographql.android'
```

To use Apollo, put your GraphQL queries in a `.graphql` file, like `src/main/graphql/com/example/DroidDetails.grapqhl`.

```
query DroidDetails {
  species(id: "c3BlY2llczoy") {
    id
    name
    classification
  }
}
```

From this, Apollo will generate a `DroidDetails` Java class with nested classes for reading from the network response.

```java
@Generated("Apollo GraphQL")
public final class DroidDetails implements Query<Operation.Variables> {
  public static final String OPERATION_DEFINITION = "query DroidDetails {\n"
      + "  species(id: \"c3BlY2llczoy\") {\n"
      + "    id\n"
      + "    name\n"
      + "    classification\n"
      + "  }\n"
      + "}";

  public static final String QUERY_DOCUMENT = OPERATION_DEFINITION;

  private final Operation.Variables variables;

  public DroidDetails() {
    this.variables = Operation.EMPTY_VARIABLES;
  }

  @Override
  public String queryDocument() {
    return QUERY_DOCUMENT;
  }

  @Override
  public Operation.Variables variables() {
    return variables;
  }

  public static class Data implements Operation.Data {
    private static final ResponseFieldMapper<Data> MAPPER = new ResponseFieldMapper<Data>() {
      private final Field[] FIELDS = {
        Field.forObject("species", "species", null, true, new Field.ObjectReader<Specy>() {
          @Override public Specy read(final ResponseReader reader) throws IOException {
            return new Specy(reader);
          }
        })
      };

      @Override
      public void map(final ResponseReader reader, final Data instance) throws IOException {
        reader.read(new ResponseReader.ValueHandler() {
          @Override
          public void handle(final int fieldIndex, final Object value) throws IOException {
            switch (fieldIndex) {
              case 0: {
                instance.species = (Specy) value;
                break;
              }
            }
          }
        }, FIELDS);
      }
    };

    private @Nullable Specy species;

    public Data(ResponseReader reader) throws IOException {
      MAPPER.map(reader, this);
    }

    public @Nullable Specy species() {
      return this.species;
    }

    public static class Specy {
      private static final ResponseFieldMapper<Specy> MAPPER = new ResponseFieldMapper<Specy>() {
        private final Field[] FIELDS = {
          Field.forString("id", "id", null, false),
          Field.forString("name", "name", null, true),
          Field.forString("classification", "classification", null, true)
        };

        @Override
        public void map(final ResponseReader reader, final Specy instance) throws IOException {
          reader.read(new ResponseReader.ValueHandler() {
            @Override
            public void handle(final int fieldIndex, final Object value) throws IOException {
              switch (fieldIndex) {
                case 0: {
                  instance.id = (String) value;
                  break;
                }
                case 1: {
                  instance.name = (String) value;
                  break;
                }
                case 2: {
                  instance.classification = (String) value;
                  break;
                }
              }
            }
          }, FIELDS);
        }
      };

      private @Nonnull String id;

      private @Nullable String name;

      private @Nullable String classification;

      public Specy(ResponseReader reader) throws IOException {
        MAPPER.map(reader, this);
      }

      public @Nonnull String id() {
        return this.id;
      }

      public @Nullable String name() {
        return this.name;
      }

      public @Nullable String classification() {
        return this.classification;
      }
    }
  }
}
```

## Consuming Code

You can then use the genrated classes with Retrofit to make requests to your GraphQL API:

```java
interface ApiService {
  @POST("/") Call<Response<DroidDetails.Data>> droidDetails(@Body OperationRequest<Operation.Variables> query);
}
```

```java
OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
Retrofit retrofit = new Retrofit.Builder()
    .baseUrl(BASE_URL)
    .client(okHttpClient)
    .addConverterFactory(new ApolloConverterFactory.Builder().build())
    .addConverterFactory(MoshiConverterFactory.create())
    .build();
ApiService service = retrofit.create(ApiService.class);
service.droidDetails(new OperationRequest<>(new DroidDetails()))
    application.service()
            .droidDetails(new OperationRequest<>(new DroidDetails()))
            .enqueue(new Callback<Response<DroidDetails.Data>>() {
              @Override
              public void onResponse(Call<Response<DroidDetails.Data>> call,
                  retrofit2.Response<Response<DroidDetails.Data>> response) {
                // parse the response
              }

              @Override public void onFailure(Call<Response<DroidDetails.Data>> call, Throwable t) {
                // handle error
              }
            });
```

## License

```
The MIT License (MIT)

Copyright (c) 2016 Meteor Development Group, Inc.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
