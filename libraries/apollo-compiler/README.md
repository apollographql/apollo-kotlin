# Module apollo-compiler

`apollo-compiler` is the low level compiler API used by apollo-gradle-plugin. 

`apollo-compiler` uses [JavaPoet](https://github.com/square/javapoet) and [KotlinPoet](https://github.com/square/kotlinpoet) to generate Java and Kotlin models from GraphQL operations.

`apollo-compiler` is usually consumed through Gradle or Maven plugins. It also contains `ApolloCompilerPlugin`.

See ["Apollo Compiler plugins"](https://www.apollographql.com/docs/kotlin/advanced/compiler-plugins) for how to develop compiler plugins.

