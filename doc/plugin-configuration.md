
#  Gradle Configuration of Apollo Android

Apollo-Android comes with logical defaults that will work for the majority of use cases, below you will find additional configuration.

## Service

Apollo-Android can use several services for multiple schemas and endpoints. The schema and graphql files related to different services must be in different folders.

You can configure services by calling `ApolloExtension.service(String)`:

```groovy
apollo {
  service("github") {
    // src/{foo}/graphql/github
    sourceFolder.set("github")
  }
  service("starwars") {
    // src/{foo}/graphql/starwars
    sourceFolder.set("starwars")
  }
}```

A service will compile all your graphql files for all variants in your project. For a simple JVM project it's usually just one variant but for android projects, you can define different queries for different variants.

## CompilationUnit

A CompilationUnit is a single invocation of the Apollo compiler. It's the combination of a service and a variant.

## CompilerParams

You can configure the Apollo compiler using [CompilerParams](src/main/kotlin/com/apollographql/apollo/gradle/api/CompilerParams.kt). `ApolloExtension`, `Service` and `CompilationUnit` all implement `CompilerParams` so you can overrride values as needed.

* Default compiler parameters are taken from `ApolloExtension`
* Compiler parameters from `Service` override the ones from `ApolloExtension`
* Compiler parameters from `CompilerUnit` override the ones from `Service`


```groovy
apollo {
  // rootPackageName is empty by default
  service("github") {
    rootPackageName.set("com.github")
  }

  onCompilationUnit {
    if (variantName == "debug") {
        // the debug variant will use the com.github.debug package
        rootPackageName.set("com.github.debug")
    } else {
        // other variants will use "com.github"
    }
  }
}
```

The complete list of parameters can be found in [CompilerParams](src/main/kotlin/com/apollographql/apollo/gradle/api/CompilerParams.kt):

```kotlin
  /**
   * Whether to generate java (default) or kotlin models
   */
  val generateKotlinModels: Property<Boolean>

  /**
   * Whether to generate the transformed queries. Transformed queries are the queries as sent to the
   * server. This can be useful if you need to upload a query's exact content to a server that doesn't
   * support automatic persisted queries.
   *
   * The transformedQueries are written in [CompilationUnit.transformedQueriesDir]
   */
  val generateTransformedQueries: Property<Boolean>

  /**
   * For custom scalar types like Date, map from the GraphQL type to the jvm/kotlin type.
   *
   * empty by default.
   */
  val customTypeMapping: MapProperty<String, String>

  /**
   * The custom types code generate some warnings that might make the build fail.
   * suppressRawTypesWarning will add the appropriate SuppressWarning annotation
   *
   * false by default
   */
  val suppressRawTypesWarning: Property<Boolean>

  /**
   * Whether to suffix your queries, etc.. with `Query`, etc..
   *
   * true by default
   */
  val useSemanticNaming: Property<Boolean>

  /**
   * The nullable value type to use. One of: annotated, apolloOptional, guavaOptional, javaOptional, inputType
   *
   * annotated by default
   * only valid for java models as kotlin has nullable support
   */
  val nullableValueType: Property<String>

  /**
   * Whether to generate builders for java models
   *
   * false by default
   * only valid for java models as kotlin has data classes
   */
  val generateModelBuilder: Property<Boolean>

  /**
   * Whether to use java beans getters in the models.
   *
   * false by default
   * only valif for java as kotlin has properties
   */
  val useJavaBeansSemanticNaming: Property<Boolean>

  /**
   *
   */
  val generateVisitorForPolymorphicDatatypes: Property<Boolean>

  /**
   * The package name of the models is computed from their folder hierarchy like for java sources.
   *
   * If you want, you can prepend a custom package name here to namespace your models.
   *
   * The empty string by default.
   */
  val rootPackageName: Property<String>

  /**
   * The graphql files containing the queries.
   *
   * This SourceDirectorySet includes .graphql and .gql files by default.
   *
   * By default, it will use [Service.sourceFolder] to populate the SourceDirectorySet.
   * You can override it from [ApolloExtension.onCompilationUnits] for more advanced use cases
   */
  val graphqlSourceDirectorySet: SourceDirectorySet

  /**
   * The schema file
   *
   * By default, it will use [Service.schemaFile] to set schemaFile.
   * You can override it from [ApolloExtension.onCompilationUnits] for more advanced use cases
   */
  val schemaFile: RegularFileProperty
```