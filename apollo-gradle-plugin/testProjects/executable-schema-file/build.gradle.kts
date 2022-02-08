import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.apollographql.apollo3.gradle.api.ApolloExtension

buildscript {
    apply(from = "../../testProjects/buildscript.gradle.kts")
}

apply(plugin = "org.jetbrains.kotlin.jvm")
apply(plugin = "com.apollographql.apollo3")

configure<ApolloExtension> {
    packageNamesFromFilePaths()
}
