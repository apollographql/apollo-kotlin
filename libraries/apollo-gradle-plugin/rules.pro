#noinspection ShrinkerUnresolvedReference

# Keep kotlin metadata so that the Kotlin compiler knows about top level functions such as
#  Line 1: import com.apollographql.apollo.gradle.api.kotlinMultiplatformExtension
#                                                      ^ Unresolved reference: kotlinMultiplatformExtension
-keep class kotlin.Metadata { *; }
-keep class kotlin.Unit { *; }

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
# Keep the ApolloPlugin entry point and everything in internal too (not sure why this is needed, ApolloGenerateSourcesTask is shrunk else)
-keep class com.apollographql.apollo.gradle.internal.** { *; }

# Makes it easier to debug on MacOS case-insensitive filesystem when unzipping the jars
-dontusemixedcaseclassnames
# Keep class names to make debugging easier
-dontobfuscate
-repackageclasses com.apollographql.apollo.relocated

# Allow to repackage com.moshi.JsonAdapter.lenient
-allowaccessmodification

# The Gradle API jar and other compileOnly dependencies aren't added to the classpath, ignore the missing symbols
-dontwarn **
