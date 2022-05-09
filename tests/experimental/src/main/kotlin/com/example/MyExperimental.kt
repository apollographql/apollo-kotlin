package com.example

@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "Don't touch this"
)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class MyExperimental