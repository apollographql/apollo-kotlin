---
title: Android support
---

Apollo-Android has support artifacts to help with caching and testing.

## SqlNormalizedCacheFactory

Add the following `dependency`:

[ ![apollo-android-support](https://img.shields.io/bintray/v/apollographql/android/apollo-android-support.svg?label=apollo-android-support) ](https://bintray.com/apollographql/android/apollo-android-support/_latestVersion)
```gradle
implementation("com.apollographql.apollo:apollo-android-support:x.y.z")
```

SqlNormalizedCacheFactory uses the Android framework [SQLiteDatabase](https://developer.android.com/reference/android/database/sqlite/SQLiteDatabase) databse to provide and instance of a `NormalizedCacheFactory`.

```java
ApolloSqlHelper apolloSqlHelper = ApolloSqlHelper.create(context, "db_name");
NormalizedCacheFactory cacheFactory = new SqlNormalizedCacheFactory(apolloSqlHelper);
```

## ApolloIdlingResource

Add the following `dependency`:

[ ![apollo-idling-resource](https://img.shields.io/bintray/v/apollographql/android/apollo-idling-resource.svg?label=apollo-idling-resource) ](https://bintray.com/apollographql/android/apollo-idling-resource/_latestVersion)
```gradle
implementation("com.apollographql.apollo:apollo-idling-resource:x.y.z")
```

The Apollo GraphQL client comes with a [IdlingResource](https://developer.android.com/training/testing/espresso/idling-resource) to use
 during your Android Espresso UI tests. It needs to be created and registered per ApolloClient instance. Register several IdlingResources
 with the same name will crash.

- Example in Java:
```java
// Register the idlingResource before running your tests (once per client).
IdlingResource idlingResource = ApolloIdlingResource.create("ApolloIdlingResource", apolloClient);
IdlingRegistry.getInstance().register(idlingResource);
```

- Example in Kotlin:
```kotlin
// Register the idlingResource before running your tests (once per client).
val idlingResource = ApolloIdlingResource.create("ApolloIdlingResource", apolloClient)
IdlingRegistry.getInstance().register(idlingResource)
```

Most frequently this code is put into a custom TestRunner as below. Please note that you need the ApolloClient instance you use in the app.

```java
public final class TestRunner extends AndroidJUnitRunner {
  @Override
  public void onStart() {
    IdlingResource idlingResource = ApolloIdlingResource.create("ApolloIdlingResource", apolloClient);
    IdlingRegistry.getInstance().register(idlingResource);
    // etc...

    super.onStart();
  }
}
```
