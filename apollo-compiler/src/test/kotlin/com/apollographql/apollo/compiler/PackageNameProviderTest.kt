package com.apollographql.apollo.compiler

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PackageNameProviderTest {

  @Test
  fun `when root package not set format package name from schema location`() {
    val packageNameProvider = PackageNameProvider(
        schemaFilePath = "src/main/graphql/com/sample/api/schema.json",
        rootPackageName = null
    )

    assertThat(packageNameProvider.packageName).isEqualTo("com.sample.api")
    assertThat(packageNameProvider.fragmentsPackageName).isEqualTo("com.sample.api.fragment")
    assertThat(packageNameProvider.typesPackageName).isEqualTo("com.sample.api.type")

     assertThat(packageNameProvider.operationPackageName("src/main/graphql/com/sample/api/query.graphql")).isEqualTo("com.sample.api")
    assertThat(packageNameProvider.operationPackageName("src/main/graphql/com/sample/api/features/query.graphql"))
        .isEqualTo("com.sample.api.features")
    assertThat(packageNameProvider.operationPackageName("src/main/graphql/com/sample/api/features/feature/query.graphql"))
        .isEqualTo("com.sample.api.features.feature")
  }

  @Test
  fun `when root package name set format package name from it`() {
    val packageNameProvider = PackageNameProvider(
        schemaFilePath = "src/main/graphql/com/sample/api/schema.json",
        rootPackageName = "com.mysample.graphql"
    )

    assertThat(packageNameProvider.packageName).isEqualTo("com.mysample.graphql")
    assertThat(packageNameProvider.fragmentsPackageName).isEqualTo("com.mysample.graphql.fragment")
    assertThat(packageNameProvider.typesPackageName).isEqualTo("com.mysample.graphql.type")

    assertThat(packageNameProvider.operationPackageName("src/main/graphql/com/sample/api/query.graphql")).isEqualTo("com.mysample.graphql")
    assertThat(packageNameProvider.operationPackageName("src/main/graphql/com/sample/api/features/query.graphql"))
        .isEqualTo("com.mysample.graphql.features")
    assertThat(packageNameProvider.operationPackageName("src/main/graphql/com/sample/api/features/feature/query.graphql"))
        .isEqualTo("com.mysample.graphql.features.feature")
  }

  @Test
  fun `when schema located at the root format package name`() {
    val packageNameProvider = PackageNameProvider(
        schemaFilePath = "src/main/graphql/schema.json",
        rootPackageName = "com.mysample.graphql"
    )

    assertThat(packageNameProvider.packageName).isEqualTo("com.mysample.graphql")
    assertThat(packageNameProvider.fragmentsPackageName).isEqualTo("com.mysample.graphql.fragment")
    assertThat(packageNameProvider.typesPackageName).isEqualTo("com.mysample.graphql.type")

    assertThat(packageNameProvider.operationPackageName("src/main/graphql/sample/api/query.graphql"))
        .isEqualTo("com.mysample.graphql.sample.api")
    assertThat(packageNameProvider.operationPackageName("src/main/graphql/com/mysample/graphql/query.graphql"))
        .isEqualTo("com.mysample.graphql")
    assertThat(packageNameProvider.operationPackageName("src/main/graphql/com/mysample/graphql/features/query.graphql"))
        .isEqualTo("com.mysample.graphql.features")
    assertThat(packageNameProvider.operationPackageName("src/main/graphql/com/mysample/graphql/features/feature/query.graphql"))
        .isEqualTo("com.mysample.graphql.features.feature")
  }
}
