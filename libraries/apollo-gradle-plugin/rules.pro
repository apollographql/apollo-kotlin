#noinspection ShrinkerUnresolvedReference

# Keep kotlin metadata so that the Kotlin compiler knows about top level functions such as
#  Line 1: import com.apollographql.apollo.gradle.api.kotlinMultiplatformExtension
#                                                      ^ Unresolved reference: kotlinMultiplatformExtension
-keep class kotlin.Metadata { *; }
-keep class kotlin.Unit { *; }

# Keep the @RequiresOptIn annotation so we get proper warnings in gradle build files
-keep class kotlin.RequiresOptIn { *; }

# We need to keep type arguments (Signature) for Gradle to be able to instantiate abstract models like `Property`
# Else it fails with
# 'Declaration of property alwaysGenerateTypesMatching does not include any type arguments in its property type interface org.gradle.api.provider.SetProperty'
-keepattributes Signature,InnerClasses,EnclosingMethod
# Similarly, Gradle needs the @Inject annotations
-keepattributes RuntimeVisible*Annotation*
# For debug
-keepattributes SourceFile,LineNumberTable

# kotlinpoet uses EnumSetOf that makes a reflexive access to "values"
# https://github.com/square/kotlinpoet/blob/9952ddcd5095a1fd09c86b9fb07faa347a4c04f0/kotlinpoet/src/main/java/com/squareup/kotlinpoet/PropertySpec.kt#L102
-keepclassmembers class com.squareup.kotlinpoet.KModifier {
    public static **[] values();
}

# Keep apollo-annotations for ApolloExperimental
-keep class com.apollographql.apollo.annotations.** { *; }
# Keep the plugin API as it's used from build scripts
-keep class com.apollographql.apollo.gradle.api.** { *; }
-keep interface com.apollographql.apollo.gradle.api.** { *; }
# And also the compiler API as it's used transitively for things like OperationOutputGenerator
-keep class com.apollographql.apollo.compiler.** { *; }
-keep interface com.apollographql.apollo.compiler.** { *; }
-keep enum com.apollographql.apollo.compiler.** { *; }
# Schema is used in a worker: https://github.com/apollographql/apollo-kotlin/blob/198480d8b0b24c01f4d11da0b1e9fa9c97062c5c/libraries/apollo-gradle-plugin-external/src/main/kotlin/com/apollographql/apollo/gradle/internal/ApolloGenerateSourcesTask.kt#L131
-keep class com.apollographql.apollo.ast.Schema { *; }
# Keep the ApolloPlugin entry point and everything in internal too because we have ApolloPlugin there
-keep class com.apollographql.apollo.gradle.internal.** { *; }

# Keep class names to make debugging easier
-dontobfuscate
-repackageclasses com.apollographql.apollo.relocated

# Allow to repackage com.moshi.JsonAdapter.lenient
-allowaccessmodification

# The Gradle API jar and other compileOnly dependencies aren't added to the classpath, ignore the missing symbols
# I tried adding them but they duplicate a lot of the program classes and trigger errors in R8.
# A future version could try to remove the intersection between the compileOnly classpath and the runtime one
-dontwarn org.gradle.**
-dontwarn org.jetbrains.kotlin.gradle.**
-dontwarn com.android.build.gradle.**
-dontwarn com.android.builder.**
-dontwarn javax.inject.Inject