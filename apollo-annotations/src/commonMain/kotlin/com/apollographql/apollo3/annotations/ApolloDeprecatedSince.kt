package com.apollographql.apollo3.annotations

@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.TYPEALIAS)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class ApolloDeprecatedSince(val version: Version) {
  @Suppress("EnumEntryName")
  enum class Version {
    `3.0.0`,

    // TODO rename this to the actual version name before releasing
    NEXT,
  }
}
