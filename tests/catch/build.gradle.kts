plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo")
}

apolloTest()

dependencies {
  implementation(libs.apollo.api)
  implementation(libs.apollo.testingsupport)
  testImplementation(libs.kotlin.test)
  testImplementation(libs.junit)
  testImplementation(libs.okhttp)
}

apollo {
  service("service") {
    // The package name for the generated models
    packageName.set("com.example")

    // Adds the given directory as a GraphQL source root
    srcDir("src/main/graphql")
    // Operation files to include.
    includes.add("**/*.graphql")
    // Operation files to exclude.
    excludes.add("**/*.graphqls")

    // Explicitly set the schema
    schemaFiles.from("src/main/graphql/schema.graphqls")
    // Extend your schema locally with type extensions
    schemaFiles.from("shared/graphql/schema.graphqls", "shared/graphql/extra.graphqls")

    // What codegen to use. One of "operationBased", "responseBased"
    codegenModels.set("operationBased")

    // Warn if using a deprecated field
    warnOnDeprecatedUsages.set(true)
    // Fail on warnings
    failOnWarnings.set(true)

    // Map the "Date" custom scalar to the com.example.Date Kotlin type
    mapScalar("Date", "com.example.Date")
    // Shorthands to map scalar to builtin types and configure their adapter at build time
    mapScalarToUpload("Upload")
    mapScalarToKotlinString("MyString")
    mapScalarToKotlinInt("MyInt")
    mapScalarToKotlinDouble("MyDouble")
    mapScalarToKotlinFloat("MyFloat")
    mapScalarToKotlinLong("MyLong")
    mapScalarToKotlinBoolean("MyBoolean")
    mapScalarToKotlinAny("MyAny")
    mapScalarToJavaString("MyString")
    mapScalarToJavaInteger("MyInteger")
    mapScalarToJavaDouble("MyDouble")
    mapScalarToJavaFloat("MyFloat")
    mapScalarToJavaLong("MyLong")
    mapScalarToJavaBoolean("MyBoolean")
    mapScalarToJavaObject("MyObject")


    // The format to output for the operation manifest. One of "operationOutput", "persistedQueryManifest"
    operationManifestFormat.set("persistedQueryManifest")
    // The file where to write the operation manifest
    operationManifest.set(file("build/generated/persistedQueryManifest.json"))

    // Whether to generate Kotlin or Java models
    generateKotlinModels.set(true)
    // Target language version for the generated code.
    languageVersion.set("1.5")
    // Whether to suffix operation name with 'Query', 'Mutation' or 'Subscription'
    useSemanticNaming.set(true)
    // Whether to generate kotlin constructors with `@JvmOverloads` for more graceful Java interop experience when default values are present.
    addJvmOverloads.set(true)
    // Whether to generate Kotlin models with `internal` visibility modifier.
    generateAsInternal.set(true)
    // Whether to generate default implementation classes for GraphQL fragments.
    generateFragmentImplementations.set(true)
    // Whether to write the query document in models
    generateQueryDocument.set(true)
    // Whether to generate the Schema class.
    generateSchema.set(true)
    // Name for the generated schema
    generatedSchemaName.set("Schema")
    // Whether to generate operation variables as [com.apollographql.apollo.api.Optional]
    generateOptionalOperationVariables.set(true)
    // Whether to generate the type safe Data builders.
    generateDataBuilders.set(true)
    // Whether to generate response model builders for Java.
    generateModelBuilders.set(true)
    // Which methods to auto generate (can include: `equalsHashCode`, `copy`, `toString`, or `dataClass`)
    generateMethods.set(listOf("dataClass"))
    // Whether to generate fields as primitive types (`int`, `double`, `boolean`) instead of their boxed types (`Integer`, `Double`, etc..)
    generatePrimitiveTypes.set(true)
    // Opt-in Builders for Operations, Fragments and Input types. Builders are more ergonomic than default arguments when there are a lot of
    // optional arguments.
    generateInputBuilders.set(true)
    // The style to use for fields that are nullable in the Java generated code
    nullableFieldStyle.set("apolloOptional")
    // Whether to decapitalize field names in the generated models (for instance `FooBar` -> `fooBar`)
    decapitalizeFields.set(false)

    // Whether to add the [JsExport] annotation to generated models.
    jsExport.set(true)
    // When to add __typename.
    addTypename.set("always")
    // Whether to flatten the models. File paths are limited on MacOSX to 256 chars and flattening can help keeping the path length manageable
    flattenModels.set(true)
    // A list of [Regex] patterns for GraphQL enums that should be generated as Kotlin sealed classes instead of the default Kotlin enums.
    sealedClassesForEnumsMatching.set(listOf(".*"))
    // A list of [Regex] patterns for GraphQL enums that should be generated as Java classes.
    classesForEnumsMatching.set(listOf(".*"))
    // Whether fields with different shape are disallowed to be merged in disjoint types.
    fieldsOnDisjointTypesMustMerge.set(false)


    // The directory where the generated models are written.
    outputDir.set(file("build/generated/apollo"))

    // Whether to generate Apollo metadata. Apollo metadata is used for multi-module support.
    generateApolloMetadata.set(true)
    // list of [Regex] patterns matching for types and fields that should be generated whether they are used by queries/fragments in this module or not.
    alwaysGenerateTypesMatching.set(listOf(".*"))
    // Reuse the schema from an upstream module
    dependsOn(project(":schema"))
    // Compute used types from a downstream module
    isADependencyOf(project(":feature"))

    // configure introspection schema download
    introspection {
      endpointUrl.set("https://your.domain/graphql/endpoint")
      schemaFile.set(file("src/main/graphql/com/example/schema.graphqls"))
    }
    // configure registry schema download
    registry {
      key.set(System.getenv("APOLLO_KEY"))
      graph.set(System.getenv("APOLLO_GRAPH"))
      schemaFile.set(file("src/main/graphql/com/example/schema.graphqls"))
    }
    // configure a compiler plugin
    plugin(project(":apollo-compiler-plugin")) {
      argument("myarg", "someValue")
    }
    // wire the generated models to the "test" source set
    outputDirConnection {
      connectToKotlinSourceSet("test")
    }
  }
  service("throw") {
    srcDir("src/main/graphql/shared")
    srcDir("src/main/graphql/throw")
    packageName.set("throw")
  }
  service("null") {
    srcDir("src/main/graphql/shared")
    srcDir("src/main/graphql/null")
    packageName.set("null")
  }
  service("result") {
    srcDir("src/main/graphql/shared")
    srcDir("src/main/graphql/result")
    packageName.set("result")
  }
}
