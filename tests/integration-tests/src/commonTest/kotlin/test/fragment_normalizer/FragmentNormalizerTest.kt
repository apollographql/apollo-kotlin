package test.fragment_normalizer

import IdCacheKeyGenerator
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.cache.normalized.api.CacheKey
import com.apollographql.apollo.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo.cache.normalized.api.normalize
import com.apollographql.apollo.cache.normalized.apolloStore
import com.apollographql.apollo.cache.normalized.normalizedCache
import com.apollographql.apollo.testing.internal.runTest
import fragment_normalizer.fragment.ConversationFragment
import fragment_normalizer.fragment.ConversationFragmentImpl
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class FragmentNormalizerTest{
  @Test
  fun test() = runTest {
    val cacheFactory = MemoryCacheFactory()

    val apolloClient = ApolloClient.Builder()
        .serverUrl("https:/example.com")
        .normalizedCache(cacheFactory)
        .build()

    var fragment1 = ConversationFragment(
        "1",
        ConversationFragment.Author(
            "John Doe",
        ),
        false
    )

    /**
     * This is not using .copy() because this test also runs in Java and Java doesn't have copy()
     */
    var fragment1Read = ConversationFragment(
        "1",
        ConversationFragment.Author(
            "John Doe",
        ),
        true
    )
    val fragment2 = ConversationFragment(
        "2",
        ConversationFragment.Author(
            "Yayyy Pancakes!",
        ),
        false
    )
    /**
     * This is not using .copy() because this test also runs in Java and Java doesn't have copy()
     */
    val fragment2Read = ConversationFragment(
        "2",
        ConversationFragment.Author(
            "Yayyy Pancakes!",
        ),
        true
    )
    apolloClient.apolloStore.writeFragmentSync(
        ConversationFragmentImpl(),
        CacheKey(fragment1.id),
        fragment1Read,
        CustomScalarAdapters.Empty
    )

    apolloClient.apolloStore.writeFragmentSync(
        ConversationFragmentImpl(),
        CacheKey(fragment2.id),
        fragment2Read,
        CustomScalarAdapters.Empty
    )

    fragment1 = apolloClient.apolloStore.readFragment(
        ConversationFragmentImpl(),
        CacheKey(fragment1.id),
    )

    assertEquals("John Doe", fragment1.author.fullName)
  }

  @Test
  fun rootKeyIsNotSkipped() = runTest {
    val fragment = ConversationFragment(
        "1",
        ConversationFragment.Author(
            "John Doe",
        ),
        false
    )

    val records = ConversationFragmentImpl().normalize(
        fragment,
        CustomScalarAdapters.Empty,
        IdCacheKeyGenerator,
        "1",
    )

    assertContains( records.keys, "1.author")
  }
}
