import com.apollographql.apollo3.gradle.api.ApolloExtension

apply(plugin = "kotlin")
apply(plugin = "com.apollographql.apollo3")

dependencies {
    add("implementation","com.apollographql.apollo3:apollo-runtime:1.4.5")
}

configure<ApolloExtension> {
    packageNamesFromFilePaths()
}