package com.apollographql.apollo.compiler

/**
 * This file duplicates some of the apollo-compiler symbols to display a meaningful error to users relying
 * on them in build scripts.
 *
 * It will be removed in a future version.
 */
import com.apollographql.apollo.annotations.ApolloDeprecatedSince


@Deprecated("The apollo-compiler symbols are not available in the Gradle plugin anymore. Use their value directly.", replaceWith = ReplaceWith("responseBased"), level = DeprecationLevel.ERROR)
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v5_0_0)
@JvmField
val MODELS_RESPONSE_BASED = "responseBased"
@Deprecated("The apollo-compiler symbols are not available in the Gradle plugin anymore. Use their value directly.", replaceWith = ReplaceWith("operationBased"), level = DeprecationLevel.ERROR)
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v5_0_0)
@JvmField
val MODELS_OPERATION_BASED = "operationBased"
@Deprecated("The apollo-compiler symbols are not available in the Gradle plugin anymore. Use their value directly.", replaceWith = ReplaceWith("experimental_operationBasedWithInterfaces"), level = DeprecationLevel.ERROR)
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v5_0_0)
@JvmField
val MODELS_OPERATION_BASED_WITH_INTERFACES = "experimental_operationBasedWithInterfaces"

@Deprecated("The apollo-compiler symbols are not available in the Gradle plugin anymore. Use their value directly.", replaceWith = ReplaceWith("ifFragments"), level = DeprecationLevel.ERROR)
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v5_0_0)
@JvmField
val ADD_TYPENAME_IF_FRAGMENTS = "ifFragments"
@Deprecated("The apollo-compiler symbols are not available in the Gradle plugin anymore. Use their value directly.", replaceWith = ReplaceWith("ifPolymorphic"), level = DeprecationLevel.ERROR)
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v5_0_0)
@JvmField
val ADD_TYPENAME_IF_POLYMORPHIC = "ifPolymorphic"
@Deprecated("The apollo-compiler symbols are not available in the Gradle plugin anymore. Use their value directly.", replaceWith = ReplaceWith("ifAbstract"), level = DeprecationLevel.ERROR)
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v5_0_0)
@JvmField
val ADD_TYPENAME_IF_ABSTRACT = "ifAbstract"
@Deprecated("The apollo-compiler symbols are not available in the Gradle plugin anymore. Use their value directly.", replaceWith = ReplaceWith("always"), level = DeprecationLevel.ERROR)
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v5_0_0)
@JvmField
val ADD_TYPENAME_ALWAYS = "always"

@Deprecated("Use \"persistedQueryManifest\" instead", replaceWith = ReplaceWith("persistedQueryManifest"), level = DeprecationLevel.ERROR)
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_1)
@JvmField
val MANIFEST_OPERATION_OUTPUT = "operationOutput"
@Deprecated("The apollo-compiler symbols are not available in the Gradle plugin anymore. Use their value directly.", replaceWith = ReplaceWith("persistedQueryManifest"), level = DeprecationLevel.ERROR)
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v5_0_0)
@JvmField
val MANIFEST_PERSISTED_QUERY = "persistedQueryManifest"
@Deprecated("The apollo-compiler symbols are not available in the Gradle plugin anymore. Use their value directly.", replaceWith = ReplaceWith("none"), level = DeprecationLevel.ERROR)
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v5_0_0)
@JvmField
val MANIFEST_NONE = "none"
