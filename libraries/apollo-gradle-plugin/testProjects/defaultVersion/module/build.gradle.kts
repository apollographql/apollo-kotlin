import com.apollographql.apollo3.gradle.api.ApolloExtension

apply(plugin = "kotlin")
apply(plugin = "com.apollographql.apollo3")

dependencies {
    // Version comes from the plugin
    add("implementation","com.apollographql.apollo3:apollo-runtime")
}

configure<ApolloExtension> {
    packageNamesFromFilePaths()
}