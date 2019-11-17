# Android Support

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

[ ![apollo-espresso-support](https://img.shields.io/bintray/v/apollographql/android/apollo-espresso-support.svg?label=apollo-espresso-support) ](https://bintray.com/apollographql/android/apollo-espresso-support/_latestVersion)
```gradle
implementation("com.apollographql.apollo:apollo-espresso-support:x.y.z")
```


The Apollo GraphQL client comes with a [IdlingResource](https://developer.android.com/training/testing/espresso/idling-resource) to use during your Android UI tests.

```kotlin
// Register the idlingResource before running your tests.
// This should be done once per client. Register several IdlingResources with the same name will crash
val idlingResource = ApolloIdlingResource.create("apolloClientIdlingResource", apolloClient)
IdlingRegistry.getInstance().register(idlingResource)
```