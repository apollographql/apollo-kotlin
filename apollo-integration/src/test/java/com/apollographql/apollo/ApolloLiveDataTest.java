package com.apollographql.apollo;

import android.arch.core.executor.testing.InstantTaskExecutorRule;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;

import com.apollographql.apollo.api.Input;
import com.apollographql.apollo.cache.normalized.lru.EvictionPolicy;
import com.apollographql.apollo.cache.normalized.lru.LruNormalizedCacheFactory;
import com.apollographql.apollo.integration.normalizer.EpisodeHeroNameQuery;
import com.apollographql.apollo.integration.normalizer.type.Episode;
import com.apollographql.apollo.livedata.LiveDataApollo;
import com.apollographql.apollo.livedata.LiveDataResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockWebServer;

import static com.apollographql.apollo.Utils.immediateExecutor;
import static com.apollographql.apollo.Utils.immediateExecutorService;
import static com.apollographql.apollo.Utils.mockResponse;
import static com.apollographql.apollo.fetcher.ApolloResponseFetchers.NETWORK_ONLY;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@SuppressWarnings({"unchecked", "unused"})
public class ApolloLiveDataTest {
    private ApolloClient apolloClient;
    private MockWebServer mockWebServer = new MockWebServer();

    private static final String FILE_EPISODE_HERO_NAME_WITH_ID = "EpisodeHeroNameResponseWithId.json";
    private static final String FILE_EPISODE_HERO_NAME_CHANGE = "EpisodeHeroNameResponseNameChange.json";
    private static final String FILE_HERO_AND_FRIEND_NAME_CHANGE = "HeroAndFriendsNameWithIdsNameChange.json";

    @Rule public final InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Before
    public void setup() throws IOException {
        mockWebServer.start();

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .dispatcher(new Dispatcher(immediateExecutorService()))
                .build();

        apolloClient = ApolloClient.builder()
                .serverUrl(mockWebServer.url("/"))
                .dispatcher(immediateExecutor())
                .okHttpClient(okHttpClient)
                .normalizedCache(new LruNormalizedCacheFactory(EvictionPolicy.NO_EVICTION), new IdFieldCacheKeyResolver())
                .build();
    }

    @After
    public void stopServer() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    public void callProducesValue() throws Exception {
        mockWebServer.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));

        Observer<LiveDataResponse<EpisodeHeroNameQuery.Data>> observer = mock(Observer.class);
        LiveData liveData = LiveDataApollo.INSTANCE.from(apolloClient.query(new EpisodeHeroNameQuery(Input.fromNullable(Episode.EMPIRE))));
        liveData.observeForever(observer);

        assertNotNull(liveData.getValue());
        verify(observer).onChanged((LiveDataResponse<EpisodeHeroNameQuery.Data>) (liveData.getValue()));
        assertThat(liveData.getValue(), instanceOf(LiveDataResponse.Success.class));

        LiveDataResponse.Success success = (LiveDataResponse.Success) liveData.getValue();
        EpisodeHeroNameQuery.Data data = (EpisodeHeroNameQuery.Data) success.getData();
        assertThat(data.hero().__typename() , is("Droid"));
        assertThat(data.hero().name() , is("R2-D2"));
    }

    @Test
    public void prefetchCompletes() throws Exception {
        mockWebServer.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));

        Observer<LiveDataResponse<EpisodeHeroNameQuery.Data>> observer = mock(Observer.class);
        LiveData liveData = LiveDataApollo.INSTANCE.from(apolloClient.prefetch(new EpisodeHeroNameQuery(Input.fromNullable(Episode.EMPIRE))));
        liveData.observeForever(observer);

        assertNotNull(liveData.getValue());
        verify(observer).onChanged((LiveDataResponse<EpisodeHeroNameQuery.Data>) (liveData.getValue()));
        assertThat(liveData.getValue(), instanceOf(LiveDataResponse.Complete.class));
    }

    @Test
    public void queryWatcherUpdatedSameQueryDifferentResults() throws Exception {
        mockWebServer.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));

        Observer<LiveDataResponse<EpisodeHeroNameQuery.Data>> observer = mock(Observer.class);
        LiveData liveData = LiveDataApollo.INSTANCE.from(apolloClient.query(new EpisodeHeroNameQuery(Input.fromNullable(Episode.EMPIRE))).watcher());
        liveData.observeForever(observer);

        assertNotNull(liveData.getValue());
        verify(observer).onChanged((LiveDataResponse<EpisodeHeroNameQuery.Data>) (liveData.getValue()));
        assertThat(liveData.getValue(), instanceOf(LiveDataResponse.Success.class));

        LiveDataResponse.Success success = (LiveDataResponse.Success) liveData.getValue();
        EpisodeHeroNameQuery.Data data = (EpisodeHeroNameQuery.Data) success.getData();
        assertThat(data.hero().__typename() , is("Droid"));
        assertThat(data.hero().name() , is("R2-D2"));

        mockWebServer.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_CHANGE));
        apolloClient.query(new EpisodeHeroNameQuery(Input.fromNullable(Episode.EMPIRE)))
                .responseFetcher(NETWORK_ONLY)
                .enqueue(null);

        LiveDataResponse.Success success2 = (LiveDataResponse.Success) liveData.getValue();
        EpisodeHeroNameQuery.Data data2 = (EpisodeHeroNameQuery.Data) success2.getData();
        assertThat(data2.hero().__typename() , is("Droid"));
        assertThat(data2.hero().name() , is("Artoo"));
    }

    @Test
    public void queryWatcherNotUpdatedSameQuerySameResults() throws Exception {
        mockWebServer.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));

        Observer<LiveDataResponse<EpisodeHeroNameQuery.Data>> observer = mock(Observer.class);
        LiveData liveData = LiveDataApollo.INSTANCE.from(apolloClient.query(new EpisodeHeroNameQuery(Input.fromNullable(Episode.EMPIRE))).watcher());
        liveData.observeForever(observer);

        assertNotNull(liveData.getValue());
        verify(observer).onChanged((LiveDataResponse<EpisodeHeroNameQuery.Data>) (liveData.getValue()));
        assertThat(liveData.getValue(), instanceOf(LiveDataResponse.Success.class));

        LiveDataResponse.Success success = (LiveDataResponse.Success) liveData.getValue();
        EpisodeHeroNameQuery.Data data = (EpisodeHeroNameQuery.Data) success.getData();
        assertThat(data.hero().__typename() , is("Droid"));
        assertThat(data.hero().name() , is("R2-D2"));

        mockWebServer.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_CHANGE));
        apolloClient.query(new EpisodeHeroNameQuery(Input.fromNullable(Episode.EMPIRE)))
                .responseFetcher(NETWORK_ONLY)
                .enqueue(null);

        verifyNoMoreInteractions(observer);

        LiveDataResponse.Success success2 = (LiveDataResponse.Success) liveData.getValue();
        EpisodeHeroNameQuery.Data data2 = (EpisodeHeroNameQuery.Data) success2.getData();
        assertThat(data2.hero().__typename() , is("Droid"));
        assertThat(data2.hero().name() , is("R2-D2"));
    }

    @Test
    public void queryWatcherUpdatedDifferentQueryDifferentResults(LiveDataResponse.Success value) throws Exception {
        mockWebServer.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));

        Observer<LiveDataResponse<EpisodeHeroNameQuery.Data>> observer = mock(Observer.class);
        LiveData liveData = LiveDataApollo.INSTANCE.from(apolloClient.query(new EpisodeHeroNameQuery(Input.fromNullable(Episode.EMPIRE))).watcher());
        liveData.observeForever(observer);

        assertNotNull(liveData.getValue());
        verify(observer).onChanged((LiveDataResponse<EpisodeHeroNameQuery.Data>) (liveData.getValue()));
        assertThat(liveData.getValue(), instanceOf(LiveDataResponse.Success.class));

        LiveDataResponse.Success success = (LiveDataResponse.Success) liveData.getValue();
        EpisodeHeroNameQuery.Data data = (EpisodeHeroNameQuery.Data) success.getData();
        assertThat(data.hero().__typename() , is("Droid"));
        assertThat(data.hero().name() , is("R2-D2"));

        mockWebServer.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_CHANGE));
        apolloClient.query(new EpisodeHeroNameQuery(Input.fromNullable(Episode.EMPIRE)))
                .responseFetcher(NETWORK_ONLY)
                .enqueue(null);

        LiveDataResponse.Success success2 = (LiveDataResponse.Success) liveData.getValue();
        EpisodeHeroNameQuery.Data data2 = (EpisodeHeroNameQuery.Data) success2.getData();
        assertThat(data2.hero().__typename() , is("Droid"));
        assertThat(data2.hero().name() , is("Artoo"));
    }

    @Test
    public void queryWatcherNotCalledWhenCanceled(LiveDataResponse.Success value) throws Exception {
        mockWebServer.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));

        Observer<LiveDataResponse<EpisodeHeroNameQuery.Data>> observer = mock(Observer.class);
        LiveData liveData = LiveDataApollo.INSTANCE.from(apolloClient.query(new EpisodeHeroNameQuery(Input.fromNullable(Episode.EMPIRE))).watcher());
        liveData.observeForever(observer);

        assertNotNull(liveData.getValue());
        verify(observer).onChanged((LiveDataResponse<EpisodeHeroNameQuery.Data>) (liveData.getValue()));
        assertThat(liveData.getValue(), instanceOf(LiveDataResponse.Success.class));

        LiveDataResponse.Success success = (LiveDataResponse.Success) liveData.getValue();
        EpisodeHeroNameQuery.Data data = (EpisodeHeroNameQuery.Data) success.getData();
        assertThat(data.hero().__typename() , is("Droid"));
        assertThat(data.hero().name() , is("R2-D2"));

        liveData.removeObserver(observer);

        mockWebServer.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_CHANGE));
        apolloClient.query(new EpisodeHeroNameQuery(Input.fromNullable(Episode.EMPIRE)))
                .responseFetcher(NETWORK_ONLY)
                .enqueue(null);

        verifyNoMoreInteractions(observer);

        LiveDataResponse.Success success2 = (LiveDataResponse.Success) liveData.getValue();
        EpisodeHeroNameQuery.Data data2 = (EpisodeHeroNameQuery.Data) success2.getData();
        assertThat(data2.hero().__typename() , is("Droid"));
        assertThat(data2.hero().name() , is("R2-D2"));
    }
}
