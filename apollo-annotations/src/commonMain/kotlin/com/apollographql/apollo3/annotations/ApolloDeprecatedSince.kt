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
    v3_0_0,

    /**
     * Represents the next version of the library.
     *
     * TODO: rename this to the actual version name before releasing, and then add a new NEXT enum entry.
     */
    NEXT,
  }
}
