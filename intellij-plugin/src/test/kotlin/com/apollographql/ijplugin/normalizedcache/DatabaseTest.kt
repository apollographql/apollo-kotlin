package com.apollographql.ijplugin.normalizedcache

import com.apollographql.ijplugin.ApolloTestCase
import com.apollographql.ijplugin.normalizedcache.provider.DatabaseNormalizedCacheProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.File

@RunWith(JUnit4::class)
class DatabaseTest : ApolloTestCase() {

  @Test
  fun loadDatabase() {
    DatabaseNormalizedCacheProvider().provide(
        parameters = File("src/test/testData/normalizedcache/normalizedcache.db")
    ).getOrThrow()
  }
}
