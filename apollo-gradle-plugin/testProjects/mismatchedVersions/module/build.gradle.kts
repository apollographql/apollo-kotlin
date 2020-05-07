apply(plugin = "kotlin")
apply(plugin = "com.apollographql.apollo")

configure<com.apollographql.apollo.gradle.api.ApolloExtension> {
    generateKotlinModels.set(true)
}

dependencies {
    add("implementation","com.apollographql.apollo:apollo-runtime:1.4.5")
}