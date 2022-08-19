package test;

import com.apollographql.apollo3.api.ApolloResponse;
import com.apollographql.apollo3.cache.normalized.api.CacheKey;
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory;
import com.apollographql.apollo3.java.ApolloClient;
import com.apollographql.apollo3.java.cache.normalized.ApolloStore;
import com.apollographql.apollo3.mockserver.MockResponse;
import com.apollographql.apollo3.mockserver.MockServer;
import com.google.common.truth.Truth;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import test.fragment.CatFragment;
import test.fragment.CatFragmentImpl;

public class ApolloStoreTest {
  MockServer mockServer;
  ApolloStore store;
  ApolloClient apolloClient;

  @Before
  public void before() {
    mockServer = new MockServer();
    String serverUrl = (String) mockServer.url(new Continuation<String>() {
      @NotNull @Override public CoroutineContext getContext() {
        return EmptyCoroutineContext.INSTANCE;
      }

      @Override public void resumeWith(@NotNull Object o) {
      }
    });

    store = ApolloStore.createApolloStore(new MemoryCacheFactory());
    apolloClient = new ApolloClient.Builder()
        .serverUrl(serverUrl)
        .store(store)
        .build();

  }

  @Test
  public void readFragment() {
    mockServer.enqueue(new MockResponse.Builder().body("{\"data\": {\"animal\": {\"__typename\": \"Cat\", \"species\": \"regular\", \"habitat\":  {\"temperature\":  10.0}}}}").build());
    apolloClient.query(new AnimalQuery()).executeBlocking();

    CatFragment catFragment = store.readFragment(new CatFragmentImpl(), new CacheKey("animal"));
    Truth.assertThat(catFragment.species).isEqualTo("regular");
    Truth.assertThat(catFragment.habitat.temperature).isEqualTo(10.0);
  }

  @Test
  public void writeFragment() {
    mockServer.enqueue(new MockResponse.Builder().body("{\"data\": {\"animal\": {\"__typename\": \"Cat\", \"species\": \"regular\", \"habitat\":  {\"temperature\":  10.0}}}}").build());
    apolloClient.query(new AnimalQuery()).executeBlocking();

    store.writeFragment(new CatFragmentImpl(), new CacheKey("animal"), new CatFragment("siamese", new CatFragment.Habitat(20.0)));
    ApolloResponse<AnimalQuery.Data> response = apolloClient.query(new AnimalQuery()).executeBlocking();
    Truth.assertThat(response.dataAssertNoErrors().animal.catFragment.species).isEqualTo("siamese");
    Truth.assertThat(response.dataAssertNoErrors().animal.catFragment.habitat.temperature).isEqualTo(20.0);

    store.writeFragment(new CatFragmentImpl(), new CacheKey("animal"), new CatFragment("persian", new CatFragment.Habitat(30.0)));
    response = apolloClient.query(new AnimalQuery()).executeBlocking();
    Truth.assertThat(response.dataAssertNoErrors().animal.catFragment.species).isEqualTo("persian");
    Truth.assertThat(response.dataAssertNoErrors().animal.catFragment.habitat.temperature).isEqualTo(30.0);
  }
}
