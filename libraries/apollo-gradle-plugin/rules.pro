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
-keepattributes *Annotation*
# For debug
-keepattributes SourceFile,LineNumberTable

# kotlinpoet uses EnumSetOf that makes a reflexive access to "values"
# https://github.com/square/kotlinpoet/blob/9952ddcd5095a1fd09c86b9fb07faa347a4c04f0/kotlinpoet/src/main/java/com/squareup/kotlinpoet/PropertySpec.kt#L102
-keepclassmembers class com.squareup.kotlinpoet.KModifier {
    public static **[] values();
}

# Keep apollo-annotations for ApolloExperimental
-keep class com.apollographql.apollo.annotations.** { *; }
# We don't need to keep apollo-api but it's been here for a while and some folks might depend on it so keep it
-keep class com.apollographql.apollo.api.** { *; }
# Keep compiler API as it's used in workers and for things like OperationOutputGenerator
-keep class com.apollographql.apollo.compiler.** { *; }
-keep interface com.apollographql.apollo.compiler.** { *; }
-keep enum com.apollographql.apollo.compiler.** { *; }
# Schema is used in a worker: https://github.com/apollographql/apollo-kotlin/blob/198480d8b0b24c01f4d11da0b1e9fa9c97062c5c/libraries/apollo-gradle-plugin-external/src/main/kotlin/com/apollographql/apollo/gradle/internal/ApolloGenerateSourcesTask.kt#L131
-keep class com.apollographql.apollo.ast.Schema { *; }
# Keep the plugin API as it's used from build scripts
-keep class com.apollographql.apollo.gradle.api.** { *; }
-keep interface com.apollographql.apollo.gradle.api.** { *; }
-keep enum com.apollographql.apollo.gradle.api.** { *; }
# Keep the ApolloPlugin entry point and everything in internal too (not sure why this is needed, ApolloGenerateSourcesTask is shrunk else)
-keep class com.apollographql.apollo.gradle.internal.** { *; }

-keep class org.gradle.api.** { *; }
-keep interface org.gradle.api.** { *; }
-keep enum org.gradle.api.** { *; }

# OkHttp has a rule that keeps PublicSuffixDatabase. It's not clear if we need it or not. For now, keep the rule. This has the
# effect of actually keeping okhttp3.internal.publicsuffix.PublicSuffixDatabase. If this ever clashes with another version of
# okhttp, a solution would be to uncomment the line below and use mapping.txt to relocate okhttp3/internal/publicsuffix/publicsuffixes.gz
# inside the zip
# See https://github.com/square/okhttp/pull/3421 and https://github.com/square/okhttp/pull/3647
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Makes it easier to debug on MacOS case-insensitive filesystem when unzipping the jars
-dontusemixedcaseclassnames
# Keep class names to make debugging easier
-dontobfuscate
-repackageclasses com.apollographql.apollo.relocated

# Allow to repackage com.moshi.JsonAdapter.lenient
-allowaccessmodification

# The Gradle API jar and other compileOnly dependencies aren't added to the classpath, ignore the missing symbols
-dontwarn **
