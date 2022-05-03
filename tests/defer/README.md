# Tests related to `@defer`

At the moment the tests use `StreamingNSURLSessionHttpEngine` on Apple targets, which requires
the [new Memory Manager](https://github.com/JetBrains/kotlin/blob/master/kotlin-native/NEW_MM.md). It is enabled on this
project in `gradle.propertis`.
