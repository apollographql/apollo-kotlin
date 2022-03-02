package test.fragment_normalizer

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.cache.normalized.api.CacheKey
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.apolloStore
import com.apollographql.apollo3.cache.normalized.normalizedCache
import com.apollographql.apollo3.testing.runTest
import fragment_normalizer.fragment.ConversationFragment
import fragment_normalizer.fragment.ConversationFragmentImpl
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ApolloExperimental::class)
class FragmentNormalizerTest{
  @Test
  fun testFoo() = runTest {
    val cacheFactory = MemoryCacheFactory()

    val apolloClient = ApolloClient.builder()
        .serverUrl("https:/example.com")
        .normalizedCache(cacheFactory)
        .build()

    var fragment1 = ConversationFragment(
        id = "1",
        author = ConversationFragment.Author(
            fullName = "John Doe",
        ),
        read = false
    )
    val fragment2 = ConversationFragment(
        id = "2",
        author = ConversationFragment.Author(
            fullName = "Yayyy Pancakes!",
        ),
        read = false
    )
    apolloClient.apolloStore.writeFragment(
        ConversationFragmentImpl(),
        CacheKey(fragment1.id),
        fragment1.copy(
            read = true,
        ),
        CustomScalarAdapters.Empty
    )

    apolloClient.apolloStore.writeFragment(
        ConversationFragmentImpl(),
        CacheKey(fragment2.id),
        fragment2.copy(
            read = true,
        ),
        CustomScalarAdapters.Empty
    )

    fragment1 = apolloClient.apolloStore.readFragment(
        ConversationFragmentImpl(),
        CacheKey(fragment1.id),
    )

    assertEquals("John Doe", fragment1.author.fullName)
  }
}
