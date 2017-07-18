package com.apollographql.apollo;


import android.support.annotation.NonNull;

import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.cache.normalized.Record;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.exception.ApolloParseException;
import com.apollographql.apollo.integration.normalizer.EpisodeHeroNameQuery;
import com.apollographql.apollo.integration.normalizer.type.Episode;
import com.apollographql.apollo.interceptor.ApolloInterceptor;
import com.apollographql.apollo.interceptor.ApolloInterceptorChain;
import com.apollographql.apollo.interceptor.FetchOptions;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

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

public class ApolloInterceptorTest {

  private ApolloClient client;
  private MockWebServer mockWebServer;
  private OkHttpClient okHttpClient;

  private static final String FILE_EPISODE_HERO_NAME_WITH_ID = "EpisodeHeroNameResponseWithId.json";
  private static final String FILE_EPISODE_HERO_NAME_CHANGE = "EpisodeHeroNameResponseNameChange.json";
  private static final int TIMEOUT_SECONDS = 3;

  @Before
  public void setup() {
    mockWebServer = new MockWebServer();
    okHttpClient = new OkHttpClient.Builder().build();
  }

  @After
  public void tearDown() {
    try {
      mockWebServer.shutdown();
    } catch (IOException e) {

    }
  }

  @Test
  public void syncApplicationInterceptorCanShortCircuitResponses() throws IOException, ApolloException {
    mockWebServer.shutdown();

    EpisodeHeroNameQuery query = createHeroNameQuery();

    final InterceptorResponse expectedResponse = prepareInterceptorResponse(query);

    ApolloInterceptor interceptor = new ApolloInterceptor() {
      @Nonnull @Override
      public InterceptorResponse intercept(@Nonnull Operation operation, @Nonnull ApolloInterceptorChain chain,
          @Nonnull FetchOptions fetchOptions) throws ApolloException {
        return expectedResponse;
      }

      @Override
      public void interceptAsync(@Nonnull Operation operation, @Nonnull ApolloInterceptorChain chain,
          @Nonnull ExecutorService dispatcher, @Nonnull FetchOptions fetchOptions, @Nonnull CallBack callBack) {
        //No op
      }

      @Override public void dispose() {
        //No op
      }
    };

    client = createApolloClient(interceptor);

    Response<EpisodeHeroNameQuery.Data> actualResponse = client.query(query).execute();

    assertThat(expectedResponse.parsedResponse.get()).isEqualTo(actualResponse);
  }

  @Test
  public void asyncApplicationInterceptorCanShortCircuitResponses() throws IOException, TimeoutException,
      InterruptedException {
    mockWebServer.shutdown();

    final NamedCountDownLatch responseLatch = new NamedCountDownLatch("responseLatch", 1);

    mockWebServer.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));

    EpisodeHeroNameQuery query = createHeroNameQuery();

    final InterceptorResponse expectedResponse = prepareInterceptorResponse(query);

    ApolloInterceptor interceptor = new ApolloInterceptor() {
      @Nonnull @Override
      public InterceptorResponse intercept(@Nonnull Operation operation, @Nonnull ApolloInterceptorChain chain,
          @Nonnull FetchOptions fetchOptions) throws ApolloException {
        return null;
      }

      @Override
      public void interceptAsync(@Nonnull Operation operation, @Nonnull ApolloInterceptorChain chain,
          @Nonnull ExecutorService dispatcher, @Nonnull FetchOptions fetchOptions, @Nonnull CallBack callBack) {
        callBack.onResponse(expectedResponse);
      }

      @Override public void dispose() {

      }
    };

    client = createApolloClient(interceptor);

    client.query(query).enqueue(new ApolloCall.Callback<EpisodeHeroNameQuery.Data>() {
      @Override public void onResponse(@Nonnull Response<EpisodeHeroNameQuery.Data> response) {
        assertThat(expectedResponse.parsedResponse.get()).isEqualTo(response);
        responseLatch.countDown();
      }

      @Override public void onFailure(@Nonnull ApolloException e) {

      }
    });

    responseLatch.awaitOrThrowWithTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS);
  }

  @Test
  public void syncApplicationInterceptorRewritesResponsesFromServer() throws IOException, ApolloException {
    mockWebServer.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));

    EpisodeHeroNameQuery query = createHeroNameQuery();

    final InterceptorResponse rewrittenResponse = prepareInterceptorResponse(query);

    ApolloInterceptor interceptor = new ApolloInterceptor() {
      @Nonnull @Override
      public InterceptorResponse intercept(@Nonnull Operation operation, @Nonnull ApolloInterceptorChain chain,
          @Nonnull FetchOptions fetchOptions) throws ApolloException {
        chain.proceed(fetchOptions);
        return rewrittenResponse;
      }

      @Override
      public void interceptAsync(@Nonnull Operation operation, @Nonnull ApolloInterceptorChain chain,
          @Nonnull ExecutorService dispatcher, @Nonnull FetchOptions fetchOptions, @Nonnull CallBack callBack) {

      }

      @Override public void dispose() {

      }
    };

    client = createApolloClient(interceptor);

    Response<EpisodeHeroNameQuery.Data> actualResponse = client.query(query).execute();

    assertThat(rewrittenResponse.parsedResponse.get()).isEqualTo(actualResponse);
  }

  @Test
  public void asyncApplicationInterceptorRewritesResponsesFromServer() throws IOException, TimeoutException, InterruptedException {
    final NamedCountDownLatch responseLatch = new NamedCountDownLatch("responseLatch", 1);

    mockWebServer.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));

    EpisodeHeroNameQuery query = createHeroNameQuery();

    final InterceptorResponse rewrittenResponse = prepareInterceptorResponse(query);

    ApolloInterceptor interceptor = new ApolloInterceptor() {
      @Nonnull @Override
      public InterceptorResponse intercept(@Nonnull Operation operation, @Nonnull ApolloInterceptorChain chain,
          @Nonnull FetchOptions fetchOptions) throws ApolloException {
        return null;
      }

      @Override
      public void interceptAsync(@Nonnull Operation operation, @Nonnull ApolloInterceptorChain chain,
          @Nonnull ExecutorService dispatcher, @Nonnull FetchOptions fetchOptions, @Nonnull final CallBack callBack) {
        chain.proceedAsync(dispatcher, fetchOptions, new CallBack() {
          @Override public void onResponse(@Nonnull InterceptorResponse response) {
            callBack.onResponse(rewrittenResponse);
          }

          @Override public void onFailure(@Nonnull ApolloException e) {
            throw new RuntimeException(e);
          }

          @Override public void onCompleted() {
            callBack.onCompleted();
          }
        });
      }

      @Override public void dispose() {

      }
    };

    client = createApolloClient(interceptor);

    client.query(query).enqueue(new ApolloCall.Callback<EpisodeHeroNameQuery.Data>() {
      @Override public void onResponse(@Nonnull Response<EpisodeHeroNameQuery.Data> response) {
        assertThat(rewrittenResponse.parsedResponse.get()).isEqualTo(response);
        responseLatch.countDown();
      }

      @Override public void onFailure(@Nonnull ApolloException e) {
        throw new RuntimeException(e);
      }
    });

    responseLatch.awaitOrThrowWithTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS);
  }

  @Test
  public void syncApplicationInterceptorThrowsApolloException() {

    final String apolloException = "ApolloException";
    EpisodeHeroNameQuery query = createHeroNameQuery();

    ApolloInterceptor interceptor = new ApolloInterceptor() {
      @Nonnull @Override
      public InterceptorResponse intercept(@Nonnull Operation operation, @Nonnull ApolloInterceptorChain chain,
          @Nonnull FetchOptions fetchOptions)
          throws ApolloException {
        throw new ApolloException(apolloException);
      }

      @Override
      public void interceptAsync(@Nonnull Operation operation, @Nonnull ApolloInterceptorChain chain,
          @Nonnull ExecutorService dispatcher, @Nonnull FetchOptions fetchOptions, @Nonnull CallBack callBack) {

      }

      @Override public void dispose() {

      }
    };

    client = createApolloClient(interceptor);

    try {
      client.query(query).execute();
    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo("ApolloException");
      assertThat(e).isInstanceOf(ApolloException.class);
    }
  }

  @Test
  public void asyncApplicationInterceptorThrowsApolloException() throws TimeoutException, InterruptedException {
    final NamedCountDownLatch responseLatch = new NamedCountDownLatch("responseLatch", 1);

    final String message = "ApolloException";
    EpisodeHeroNameQuery query = createHeroNameQuery();

    ApolloInterceptor interceptor = new ApolloInterceptor() {

      @Nonnull @Override
      public InterceptorResponse intercept(@Nonnull Operation operation, @Nonnull ApolloInterceptorChain chain,
          @Nonnull FetchOptions fetchOptions) throws ApolloException {
        return null;
      }

      @Override
      public void interceptAsync(@Nonnull Operation operation, @Nonnull ApolloInterceptorChain chain,
          @Nonnull ExecutorService dispatcher, @Nonnull FetchOptions fetchOptions, @Nonnull CallBack callBack) {
        ApolloException apolloException = new ApolloParseException(message);
        callBack.onFailure(apolloException);
      }

      @Override public void dispose() {

      }
    };

    client = createApolloClient(interceptor);

    client.query(query)
        .enqueue(new ApolloCall.Callback<EpisodeHeroNameQuery.Data>() {
          @Override public void onResponse(@Nonnull Response<EpisodeHeroNameQuery.Data> response) {

          }

          @Override public void onFailure(@Nonnull ApolloException e) {
            assertThat(e.getMessage()).isEqualTo(message);
            assertThat(e).isInstanceOf(ApolloParseException.class);
            responseLatch.countDown();
          }
        });
    responseLatch.awaitOrThrowWithTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS);
  }

  @Test
  public void syncApplicationInterceptorThrowsRuntimeException() {

    EpisodeHeroNameQuery query = createHeroNameQuery();

    ApolloInterceptor interceptor = new ApolloInterceptor() {
      @Nonnull @Override
      public InterceptorResponse intercept(@Nonnull Operation operation, @Nonnull ApolloInterceptorChain chain,
          @Nonnull FetchOptions fetchOptions) throws ApolloException {
        throw new RuntimeException("RuntimeException");
      }

      @Override
      public void interceptAsync(@Nonnull Operation operation, @Nonnull ApolloInterceptorChain chain,
          @Nonnull ExecutorService dispatcher, @Nonnull FetchOptions fetchOptions, @Nonnull CallBack callBack) {

      }

      @Override public void dispose() {

      }
    };

    client = createApolloClient(interceptor);

    try {
      client.query(query).execute();
    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo("RuntimeException");
      assertThat(e).isInstanceOf(RuntimeException.class);
    }
  }

  @Test
  public void asyncApplicationInterceptorThrowsRuntimeException() throws TimeoutException, InterruptedException {
    NamedCountDownLatch latch = new NamedCountDownLatch("latch", 1);

    final String message = "RuntimeException";
    EpisodeHeroNameQuery query = createHeroNameQuery();

    ApolloInterceptor interceptor = new ApolloInterceptor() {

      @Nonnull @Override
      public InterceptorResponse intercept(@Nonnull Operation operation, @Nonnull ApolloInterceptorChain chain,
          @Nonnull FetchOptions fetchOptions) throws ApolloException {
        return null;
      }

      @Override
      public void interceptAsync(@Nonnull Operation operation, @Nonnull ApolloInterceptorChain chain,
          @Nonnull ExecutorService dispatcher, @Nonnull FetchOptions fetchOptions, @Nonnull CallBack callBack) {
        dispatcher.execute(new Runnable() {
          @Override public void run() {
            throw new RuntimeException(message);
          }
        });
      }

      @Override public void dispose() {

      }
    };

    client = ApolloClient.builder()
        .serverUrl(mockWebServer.url("/"))
        .okHttpClient(okHttpClient)
        .addApplicationInterceptor(interceptor)
        .dispatcher(new ExceptionHandlingExecutor(message, RuntimeException.class, latch))
        .build();

    client
        .query(query)
        .enqueue(new ApolloCall.Callback<EpisodeHeroNameQuery.Data>() {
          @Override public void onResponse(@Nonnull Response<EpisodeHeroNameQuery.Data> response) {

          }

          @Override public void onFailure(@Nonnull ApolloException e) {

          }
        });

    latch.awaitOrThrowWithTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS);
  }

  @Test
  public void syncApplicationInterceptorReturnsNull() {
    EpisodeHeroNameQuery query = createHeroNameQuery();

    ApolloInterceptor interceptor = new ApolloInterceptor() {
      @Nonnull @Override
      public InterceptorResponse intercept(@Nonnull Operation operation, @Nonnull ApolloInterceptorChain chain,
          @Nonnull FetchOptions fetchOptions) throws ApolloException {
        return null;
      }

      @Override
      public void interceptAsync(@Nonnull Operation operation, @Nonnull ApolloInterceptorChain chain,
          @Nonnull ExecutorService dispatcher, @Nonnull FetchOptions fetchOptions, @Nonnull CallBack callBack) {

      }

      @Override public void dispose() {

      }
    };

    client = createApolloClient(interceptor);

    try {
      client.query(query).execute();
      Assert.fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(NullPointerException.class);
    }
  }

  @Test
  public void asyncApplicationInterceptorReturnsNull() throws TimeoutException, InterruptedException {
    NamedCountDownLatch latch = new NamedCountDownLatch("first", 1);

    EpisodeHeroNameQuery query = createHeroNameQuery();

    ApolloInterceptor interceptor = new ApolloInterceptor() {
      @Nonnull @Override
      public InterceptorResponse intercept(@Nonnull Operation operation, @Nonnull ApolloInterceptorChain chain,
          @Nonnull FetchOptions fetchOptions) throws ApolloException {
        return null;
      }

      @Override
      public void interceptAsync(@Nonnull Operation operation, @Nonnull ApolloInterceptorChain chain,
          @Nonnull ExecutorService dispatcher, @Nonnull FetchOptions fetchOptions, @Nonnull final CallBack callBack) {
        dispatcher.execute(new Runnable() {
          @Override public void run() {
            callBack.onResponse(null);
          }
        });
      }

      @Override public void dispose() {

      }
    };

    client = ApolloClient.builder()
        .serverUrl(mockWebServer.url("/"))
        .okHttpClient(okHttpClient)
        .addApplicationInterceptor(interceptor)
        .dispatcher(new ExceptionHandlingExecutor(null, NullPointerException.class, latch))
        .build();

    client.query(query).enqueue(new ApolloCall.Callback<EpisodeHeroNameQuery.Data>() {
      @Override public void onResponse(@Nonnull Response<EpisodeHeroNameQuery.Data> response) {

      }

      @Override public void onFailure(@Nonnull ApolloException e) {

      }
    });

    latch.awaitOrThrowWithTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS);
  }

  @Test
  public void applicationInterceptorCanMakeMultipleRequestsToServer() throws IOException, ApolloException {
    mockWebServer.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));
    mockWebServer.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_CHANGE));

    EpisodeHeroNameQuery query = createHeroNameQuery();

    ApolloInterceptor interceptor = new ApolloInterceptor() {
      @Nonnull @Override
      public InterceptorResponse intercept(@Nonnull Operation operation, @Nonnull ApolloInterceptorChain chain,
          @Nonnull FetchOptions fetchOptions) throws ApolloException {
        chain.proceed(fetchOptions);
        return chain.proceed(fetchOptions);
      }

      @Override
      public void interceptAsync(@Nonnull Operation operation, @Nonnull ApolloInterceptorChain chain,
          @Nonnull ExecutorService dispatcher, @Nonnull FetchOptions fetchOptions, @Nonnull CallBack callBack) {

      }

      @Override public void dispose() {

      }
    };

    client = createApolloClient(interceptor);

    Response<EpisodeHeroNameQuery.Data> actualResponse = client.query(query).execute();
    assertThat(actualResponse.data().hero().name()).isEqualTo("Artoo");
  }

  @Test
  public void onShortCircuitingResponseSubsequentInterceptorsAreNotCalled() throws IOException, ApolloException {
    mockWebServer.shutdown();

    EpisodeHeroNameQuery query = createHeroNameQuery();
    final InterceptorResponse expectedResponse = prepareInterceptorResponse(query);

    ApolloInterceptor firstInterceptor = new ApolloInterceptor() {
      @Nonnull @Override
      public InterceptorResponse intercept(@Nonnull Operation operation, @Nonnull ApolloInterceptorChain chain,
          @Nonnull FetchOptions fetchOptions) throws
          ApolloException {
        return expectedResponse;
      }

      @Override
      public void interceptAsync(@Nonnull Operation operation, @Nonnull ApolloInterceptorChain chain,
          @Nonnull ExecutorService dispatcher, @Nonnull FetchOptions fetchOptions, @Nonnull CallBack callBack) {
      }

      @Override public void dispose() {
      }
    };

    ApolloInterceptor secondInterceptor = new ApolloInterceptor() {
      @Nonnull @Override
      public InterceptorResponse intercept(@Nonnull Operation operation, @Nonnull ApolloInterceptorChain chain,
         @Nonnull FetchOptions fetchOptions) throws
          ApolloException {
        Assert.fail("Second interceptor called, although response has been short circuited");
        return chain.proceed(fetchOptions);
      }

      @Override
      public void interceptAsync(@Nonnull Operation operation, @Nonnull ApolloInterceptorChain chain,
          @Nonnull ExecutorService dispatcher, @Nonnull FetchOptions fetchOptions, @Nonnull CallBack callBack) {

      }

      @Override public void dispose() {

      }
    };

    client = ApolloClient.builder()
        .serverUrl(mockWebServer.url("/"))
        .okHttpClient(okHttpClient)
        .addApplicationInterceptor(firstInterceptor)
        .addApplicationInterceptor(secondInterceptor)
        .build();

    Response<EpisodeHeroNameQuery.Data> actualResponse = client.query(query).execute();
    assertThat(expectedResponse.parsedResponse.get()).isEqualTo(actualResponse);
  }

  @Test
  public void onApolloCallCanceledAsyncApolloInterceptorIsDisposed() throws ApolloException, TimeoutException, InterruptedException {
    final NamedCountDownLatch latch = new NamedCountDownLatch("latch", 1);

    EpisodeHeroNameQuery query = createHeroNameQuery();
    final InterceptorResponse fakeResponse = prepareInterceptorResponse(query);

    ApolloInterceptor interceptor = new ApolloInterceptor() {

      volatile boolean disposed;

      @Nonnull @Override
      public InterceptorResponse intercept(@Nonnull Operation operation, @Nonnull ApolloInterceptorChain chain,
          @Nonnull FetchOptions fetchOptions) throws ApolloException {
        return null;
      }

      @Override
      public void interceptAsync(@Nonnull Operation operation, @Nonnull ApolloInterceptorChain chain,
          @Nonnull ExecutorService dispatcher, @Nonnull FetchOptions fetchOptions, @Nonnull final CallBack callBack) {
        dispatcher.execute(new Runnable() {
          @Override public void run() {
            if (!disposed) {
              callBack.onResponse(fakeResponse);
            }
          }
        });
      }

      @Override public void dispose() {
        disposed = true;
        latch.countDown();
      }
    };

    client = createApolloClient(interceptor);

    ApolloCall<EpisodeHeroNameQuery.Data> apolloCall = client.query(query);

    final AtomicReference<String> errorState = new AtomicReference<>(null);
    apolloCall.enqueue(new ApolloCall.Callback<EpisodeHeroNameQuery.Data>() {
      @Override public void onResponse(@Nonnull Response<EpisodeHeroNameQuery.Data> response) {
        errorState.set("Received a response, even though the request has been canceled");
      }

      @Override public void onFailure(@Nonnull ApolloException e) {
        errorState.set("Received an apolloException, even though the request has been canceled");
      }
    });

    apolloCall.cancel();

    //Latch's count should go down to zero in interceptor's dispose,
    //else timeout is reached which means the test fails.
    latch.awaitOrThrowWithTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    assertThat(errorState.get()).isNull();
  }

  @NonNull private EpisodeHeroNameQuery createHeroNameQuery() {
    return EpisodeHeroNameQuery
        .builder()
        .episode(Episode.EMPIRE)
        .build();
  }

  private ApolloClient createApolloClient(ApolloInterceptor interceptor) {
    return ApolloClient.builder()
        .serverUrl(mockWebServer.url("/"))
        .okHttpClient(okHttpClient)
        .addApplicationInterceptor(interceptor)
        .build();
  }

  @NonNull private InterceptorResponse prepareInterceptorResponse(EpisodeHeroNameQuery query) {
    Request request = new Request.Builder()
        .url(mockWebServer.url("/"))
        .build();

    okhttp3.Response okHttpResponse = new okhttp3.Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_2)
        .code(200)
        .message("Intercepted")
        .body(ResponseBody.create(MediaType.parse("text/plain; charset=utf-8"), "fakeResponse"))
        .build();

    Response<EpisodeHeroNameQuery.Data> apolloResponse = Response.<EpisodeHeroNameQuery.Data>builder(query).build();

    return new InterceptorResponse(okHttpResponse,
        apolloResponse, Collections.<Record>emptyList());
  }

  private MockResponse mockResponse(String fileName) throws IOException {
    return new MockResponse().setChunkedBody(Utils.readFileToString(getClass(), "/" + fileName), 32);
  }

  private class ExceptionHandlingExecutor extends ThreadPoolExecutor {

    private String message;
    private Class<?> exceptionClass;
    private NamedCountDownLatch latch;

    private ExceptionHandlingExecutor(String message, Class<?> exceptionClass, NamedCountDownLatch latch) {
      super(1, 1, 0, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
      this.message = message;
      this.exceptionClass = exceptionClass;
      this.latch = latch;
    }

    @Override public void execute(final Runnable command) {
      super.execute(new Runnable() {
        @Override public void run() {
          try {
            command.run();
          } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo(message);
            assertThat(e).isInstanceOf(exceptionClass);
            latch.countDown();
          }
        }
      });
    }
  }
}
