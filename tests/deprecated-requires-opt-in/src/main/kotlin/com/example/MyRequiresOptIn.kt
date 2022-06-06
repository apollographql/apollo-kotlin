package com.example

@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "Apollo: This symbol requires opt-in"
)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class MyRequiresOptIn