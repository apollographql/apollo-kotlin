# Implementation

To add the `apollo-coroutines-support` module to your project, first add the following maven repository to your gradle file:

```groovy
maven { 
    url "https://dl.bintray.com/apollographql/android" 
}
```

Then add the dependency with the latest Apollo version:

```groovy
implementation "com.apollographql.apollo:apollo-coroutines-support:$apollo_version"
```