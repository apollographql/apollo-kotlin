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
    v3_0_1,
    v3_1_1,
    v3_2_1,
    v3_2_2,
    v3_2_3,
    v3_3_1,
    v3_3_2,
    v3_3_3,
    v3_4_1,
    v3_5_1
  }
}
