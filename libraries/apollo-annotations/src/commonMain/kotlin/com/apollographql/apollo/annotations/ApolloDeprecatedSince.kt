package com.apollographql.apollo.annotations

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
@ApolloInternal
@MustBeDocumented
annotation class ApolloDeprecatedSince(val version: Version) {
  @ApolloInternal
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
    v3_5_1,
    v3_6_3,
    v3_7_2,
    v3_7_5,
    v4_0_0,
    v4_0_1,
    v4_0_2,
  }
}
