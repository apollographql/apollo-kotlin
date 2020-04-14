---
title: Kotlin Multiplatform 
---

With version 2.0.0, Apollo supports Kotlin Multiplatform! 

**Note:** Kotlin Multiplatform is evolving fast. For latest info on how to build Kotlin Multiplatform projects with Gradle, please refer to
original docs:
- https://kotlinlang.org/docs/reference/multiplatform.html
- https://kotlinlang.org/docs/reference/building-mpp-with-gradle.html
- https://play.kotlinlang.org/hands-on/Introduction%20to%20Kotlin%20Multiplatform/01_Introduction 

## How-To Guide

### Kotlin Multiplatform Library setup

Kotlin Multiplatform project produces an iOS Framework as an artifact of the build. In order the build iOS apps with Kotlin Multiplatform,
the Framework needs to be linked into your XCode project. For more details on this, please refer to official docs. 

### Gradle Setup

First of all, here is how a classic Gradle setup looks like:

```kotlin:title=build.gradle
plugins {
  kotlin("multiplatform")
  id("com.apollographql.apollo")
}                                                           

apollo {
  // use any of the extension properties Apollo provides
}                                                       

kotlin {
  // targets
  jvm()
  iosArm64("ios")

  sourceSets {
    commonMain {
      dependencies {
        implementation(kotlin("stdlib-common"))
        implementation("com.apollographql.apollo:apollo-api")
      }
    }                                    
    // Other Kotlin configurations...
  }
}
```

### Adding GraphQL files

Add your `schema.json` and other `.graphql` files under `src/commonMain/graphql`. Build your module and generated code will be available
under `commonMain` sourceSet. That means you can use them both in `commonMain` or platform specific Kotlin code. Once Kotlin plugin builds
the iOS Framework, generated code can even be called from Swift code.

### Usage

JVM and Android users can use `apollo-runtime` to do network requests and interacting with Apollo cache. Please refer to all the other
documentation for more details.

iOS users can only use `apollo-api` (as for 2.0.0). That means you will need to do the Http request (with any iOS networking library) and
let Apollo parse the response. Please refer to [no-runtime](https://www.apollographql.com/docs/android/advanced/no-runtime) for more
details.
