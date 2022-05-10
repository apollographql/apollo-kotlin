package com.apollographql.apollo3.annotations

@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "This field, input field or enum value is declared experimental and can be changed in a backwards-incompatible manner in future schema updates."
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
@MustBeDocumented
annotation class Experimental
