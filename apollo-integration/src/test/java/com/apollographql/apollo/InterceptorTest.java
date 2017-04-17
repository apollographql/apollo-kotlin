package com.apollographql.apollo;


import com.apollographql.android.impl.normalizer.EpisodeHeroName;
import com.apollographql.android.impl.normalizer.type.Episode;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.cache.normalized.Record;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.interceptor.ApolloInterceptor;
import com.apollographql.apollo.interceptor.ApolloInterceptorChain;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.Nonnull;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static com.apollographql.apollo.interceptor.ApolloInterceptor.InterceptorResponse;
import static com.google.common.truth.Truth.assertThat;

public class InterceptorTest {

  public static final int TIMEOUT_SECONDS = 3;
  private ApolloClient client;
  private MockWebServer mockWebServer;
  private OkHttpClient okHttpClient;

  private static final String FILE_EPISODE_HERO_NAME_WITH_ID = "EpisodeHeroNameResponseWithId.json";
  private static final String FILE_EPISODE_HERO_NAME_CHANGE = "EpisodeHeroNameResponseNameChange.json";


  @Before
  public void setup() {
    mockWebServer = new MockWebServer();
    okHttpClient = new OkHttpClient.Builder().build();
  }

  @Test
  public void syncApplicationInterceptorsCanShortCircuitResponses() throws IOException, ApolloException {
    mockWebServer.shutdown();

    EpisodeHeroName query = EpisodeHeroName.builder().episode(Episode.EMPIRE).build();

    okhttp3.Request request = getFakeHttpRequest();

    okhttp3.Response okHttpResponse = getFakeHttpResponse(request);

    Response<EpisodeHeroName.Data> apolloResponse = new Response<>(query);

    final InterceptorResponse expectedResponse = new InterceptorResponse(okHttpResponse,
        apolloResponse, Collections.<Record>emptyList());

    ApolloInterceptor interceptor = new ApolloInterceptor() {
      @Nonnull @Override
      public InterceptorResponse intercept(Operation operation, ApolloInterceptorChain chain) throws ApolloException {
        return expectedResponse;
      }

      @Override
      public void interceptAsync(@Nonnull Operation operation, @Nonnull ApolloInterceptorChain chain, @Nonnull ExecutorService dispatcher, @Nonnull CallBack callBack) {
        //No op
      }

      @Override public void dispose() {
        //No op
      }
    };

    client = getApolloClient(interceptor);

    Response<EpisodeHeroName.Data> actualResponse = client.newCall(query).execute();

    assertThat(expectedResponse.parsedResponse.get()).isEqualTo(actualResponse);
  }

  @Test
  public void syncApplicationInterceptorsRewriteResponsesFromServer() throws IOException, ApolloException {
    mockWebServer.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));

    EpisodeHeroName query = EpisodeHeroName.builder().episode(Episode.EMPIRE).build();

    okhttp3.Request request = getFakeHttpRequest();

    okhttp3.Response okHttpResponse = getFakeHttpResponse(request);

    Response<EpisodeHeroName.Data> apolloResponse = new Response<>(query);

    final InterceptorResponse rewrittenResponse = new InterceptorResponse(okHttpResponse,
        apolloResponse, Collections.<Record>emptyList());

    ApolloInterceptor interceptor = new ApolloInterceptor() {
      @Nonnull @Override
      public InterceptorResponse intercept(Operation operation, ApolloInterceptorChain chain) throws ApolloException {
        chain.proceed();
        return rewrittenResponse;
      }

      @Override
      public void interceptAsync(@Nonnull Operation operation, @Nonnull ApolloInterceptorChain chain, @Nonnull ExecutorService dispatcher, @Nonnull CallBack callBack) {

      }

      @Override public void dispose() {

      }
    };

    client = getApolloClient(interceptor);

    Response<EpisodeHeroName.Data> actualResponse = client.newCall(query).execute();

    assertThat(rewrittenResponse.parsedResponse.get()).isEqualTo(actualResponse);
  }

  @Test
  public void asyncApplicationInterceptorsCanShortCutResponses() throws IOException, TimeoutException, InterruptedException {
    mockWebServer.shutdown();

    final NamedCountDownLatch responseLatch = new NamedCountDownLatch("responseLatch", 1);

    mockWebServer.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));

    EpisodeHeroName query = EpisodeHeroName.builder().episode(Episode.EMPIRE).build();

    okhttp3.Request request = getFakeHttpRequest();

    okhttp3.Response okHttpResponse = getFakeHttpResponse(request);

    Response<EpisodeHeroName.Data> apolloResponse = new Response<>(query);

    final InterceptorResponse expectedResponse = new InterceptorResponse(okHttpResponse,
        apolloResponse, Collections.<Record>emptyList());

    ApolloInterceptor interceptor = new ApolloInterceptor() {
      @Nonnull @Override
      public InterceptorResponse intercept(Operation operation, ApolloInterceptorChain chain) throws ApolloException {
        return null;
      }

      @Override
      public void interceptAsync(@Nonnull Operation operation, @Nonnull ApolloInterceptorChain chain, @Nonnull ExecutorService dispatcher, @Nonnull CallBack callBack) {
        callBack.onResponse(expectedResponse);
      }

      @Override public void dispose() {

      }
    };

    client = getApolloClient(interceptor);

    client.newCall(query).enqueue(new ApolloCall.Callback<EpisodeHeroName.Data>() {
      @Override public void onResponse(@Nonnull Response<EpisodeHeroName.Data> response) {
        assertThat(expectedResponse.parsedResponse.get()).isEqualTo(response);
        responseLatch.countDown();
      }

      @Override public void onFailure(@Nonnull ApolloException e) {

      }
    });

    responseLatch.awaitOrThrowWithTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS);
  }

  @Test
  public void asyncApplicationInterceptorsRewriteResponsesFromServer() throws IOException, TimeoutException, InterruptedException {
    final NamedCountDownLatch responseLatch = new NamedCountDownLatch("responseLatch", 1);

    mockWebServer.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));

    EpisodeHeroName query = EpisodeHeroName.builder().episode(Episode.EMPIRE).build();

    okhttp3.Request request = getFakeHttpRequest();

    okhttp3.Response okHttpResponse = getFakeHttpResponse(request);

    Response<EpisodeHeroName.Data> apolloResponse = new Response<>(query);

    final InterceptorResponse rewrittenResponse = new InterceptorResponse(okHttpResponse,
        apolloResponse, Collections.<Record>emptyList());

    ApolloInterceptor interceptor = new ApolloInterceptor() {
      @Nonnull @Override
      public InterceptorResponse intercept(Operation operation, ApolloInterceptorChain chain) throws ApolloException {
        return null;
      }

      @Override
      public void interceptAsync(@Nonnull Operation operation, @Nonnull ApolloInterceptorChain chain, @Nonnull ExecutorService dispatcher, @Nonnull final CallBack callBack) {
        chain.proceedAsync(dispatcher, new CallBack() {
          @Override public void onResponse(@Nonnull InterceptorResponse response) {
            callBack.onResponse(rewrittenResponse);
          }

          @Override public void onFailure(@Nonnull ApolloException e) {

          }
        });
      }

      @Override public void dispose() {

      }
    };

    client = getApolloClient(interceptor);

    client.newCall(query).enqueue(new ApolloCall.Callback<EpisodeHeroName.Data>() {
      @Override public void onResponse(@Nonnull Response<EpisodeHeroName.Data> response) {
        assertThat(rewrittenResponse.parsedResponse.get()).isEqualTo(response);
        responseLatch.countDown();
      }

      @Override public void onFailure(@Nonnull ApolloException e) {

      }
    });

    responseLatch.awaitOrThrowWithTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS);
  }

  @Test
  public void applicationInterceptorsCanMakeMultipleRequestsToServer() throws IOException, ApolloException {
    mockWebServer.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));
    mockWebServer.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_CHANGE));

    EpisodeHeroName query = EpisodeHeroName.builder().episode(Episode.EMPIRE).build();

    ApolloInterceptor interceptor = new ApolloInterceptor() {
      @Nonnull @Override
      public InterceptorResponse intercept(Operation operation, ApolloInterceptorChain chain) throws ApolloException {
        chain.proceed();
        return chain.proceed();
      }

      @Override
      public void interceptAsync(@Nonnull Operation operation, @Nonnull ApolloInterceptorChain chain, @Nonnull ExecutorService dispatcher, @Nonnull CallBack callBack) {

      }

      @Override public void dispose() {

      }
    };

    client = getApolloClient(interceptor);

    Response<EpisodeHeroName.Data> actualResponse = client.newCall(query).execute();
    assertThat(actualResponse.data().hero().name()).isEqualTo("Artoo");
  }

  @Test
  public void syncApplicationInterceptorThrowsRuntimeException() {

    EpisodeHeroName query = EpisodeHeroName.builder().episode(Episode.EMPIRE).build();

    ApolloInterceptor interceptor = new ApolloInterceptor() {
      @Nonnull @Override
      public InterceptorResponse intercept(Operation operation, ApolloInterceptorChain chain) throws ApolloException {
        throw new RuntimeException("RuntimeException");
      }

      @Override
      public void interceptAsync(@Nonnull Operation operation, @Nonnull ApolloInterceptorChain chain, @Nonnull ExecutorService dispatcher, @Nonnull CallBack callBack) {

      }

      @Override public void dispose() {

      }
    };

    client = getApolloClient(interceptor);

    try {
      client.newCall(query).execute();
    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo("RuntimeException");
    }
  }

  private ApolloClient getApolloClient(ApolloInterceptor interceptor) {
    return ApolloClient.builder()
        .serverUrl(mockWebServer.url("/"))
        .okHttpClient(okHttpClient)
        .applicationInterceptor(interceptor)
        .build();
  }

  private Request getFakeHttpRequest() {
    return new Request.Builder()
        .url(mockWebServer.url("/"))
        .build();
  }

  private okhttp3.Response getFakeHttpResponse(Request request) {
    return new okhttp3.Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_2)
        .code(200)
        .message("Intercepted")
        .body(ResponseBody.create(MediaType.parse("text/plain; charset=utf-8"), "fakeResponse"))
        .build();
  }

  private MockResponse mockResponse(String fileName) throws IOException {
    return new MockResponse().setChunkedBody(Utils.readFileToString(getClass(), "/" + fileName), 32);
  }
}
