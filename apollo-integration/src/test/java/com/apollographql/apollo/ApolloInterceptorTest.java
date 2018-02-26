package com.apollographql.apollo;


import android.support.annotation.NonNull;

import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.cache.normalized.Record;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.exception.ApolloParseException;
import com.apollographql.apollo.integration.normalizer.EpisodeHeroNameQuery;
import com.apollographql.apollo.integration.normalizer.type.Episode;
import com.apollographql.apollo.interceptor.ApolloInterceptor;
import com.apollographql.apollo.interceptor.ApolloInterceptorChain;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.Nonnull;

import io.reactivex.functions.Predicate;
import okhttp3.Dispatcher;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static com.apollographql.apollo.Utils.assertResponse;
import static com.apollographql.apollo.Utils.enqueueAndAssertResponse;
import static com.apollographql.apollo.interceptor.ApolloInterceptor.InterceptorResponse;
import static com.google.common.truth.Truth.assertThat;
import static junit.framework.TestCase.fail;

public class ApolloInterceptorTest {
  private static final String FILE_EPISODE_HERO_NAME_WITH_ID = "EpisodeHeroNameResponseWithId.json";
  private static final String FILE_EPISODE_HERO_NAME_CHANGE = "EpisodeHeroNameResponseNameChange.json";

  private ApolloClient client;
  @Rule public final MockWebServer server = new MockWebServer();
  private OkHttpClient okHttpClient;

  @Before
  public void setup() {
    okHttpClient = new OkHttpClient.Builder()
        .dispatcher(new Dispatcher(Utils.immediateExecutorService()))
        .build();
  }

  @Test
  public void asyncApplicationInterceptorCanShortCircuitResponses() throws Exception {
    final NamedCountDownLatch responseLatch = new NamedCountDownLatch("responseLatch", 1);
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));
    EpisodeHeroNameQuery query = createHeroNameQuery();
    final InterceptorResponse expectedResponse = prepareInterceptorResponse(query);
    ApolloInterceptor interceptor = new ApolloInterceptor() {
      @Override
      public void interceptAsync(@Nonnull InterceptorRequest request, @Nonnull ApolloInterceptorChain chain,
          @Nonnull Executor dispatcher, @Nonnull CallBack callBack) {
        callBack.onResponse(expectedResponse);
      }

      @Override public void dispose() {

      }
    };

    client = createApolloClient(interceptor, Utils.immediateExecutor());
    client.query(query).enqueue(new ApolloCall.Callback<EpisodeHeroNameQuery.Data>() {
      @Override public void onResponse(@Nonnull Response<EpisodeHeroNameQuery.Data> response) {
        assertThat(expectedResponse.parsedResponse.get()).isEqualTo(response);
        responseLatch.countDown();
      }

      @Override public void onFailure(@Nonnull ApolloException e) {

      }
    });

    responseLatch.await();
  }

  @Test
  public void asyncApplicationInterceptorRewritesResponsesFromServer() throws Exception {
    final NamedCountDownLatch responseLatch = new NamedCountDownLatch("responseLatch", 1);

    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));
    EpisodeHeroNameQuery query = createHeroNameQuery();
    final InterceptorResponse rewrittenResponse = prepareInterceptorResponse(query);
    ApolloInterceptor interceptor = new ApolloInterceptor() {
      @Override
      public void interceptAsync(@Nonnull InterceptorRequest request, @Nonnull ApolloInterceptorChain chain,
          @Nonnull Executor dispatcher, @Nonnull final CallBack callBack) {
        chain.proceedAsync(request, dispatcher, new CallBack() {
          @Override public void onResponse(@Nonnull InterceptorResponse response) {
            callBack.onResponse(rewrittenResponse);
          }

          @Override public void onFailure(@Nonnull ApolloException e) {
            throw new RuntimeException(e);
          }

          @Override public void onCompleted() {
            callBack.onCompleted();
          }

          @Override public void onFetch(FetchSourceType sourceType) {
            callBack.onFetch(sourceType);
          }
        });
      }

      @Override public void dispose() {

      }
    };

    client = createApolloClient(interceptor, Utils.immediateExecutor());
    client.query(query).enqueue(new ApolloCall.Callback<EpisodeHeroNameQuery.Data>() {
      @Override public void onResponse(@Nonnull Response<EpisodeHeroNameQuery.Data> response) {
        assertThat(rewrittenResponse.parsedResponse.get()).isEqualTo(response);
        responseLatch.countDown();
      }

      @Override public void onFailure(@Nonnull ApolloException e) {
        throw new RuntimeException(e);
      }
    });

    responseLatch.await();
  }

  @Test
  public void asyncApplicationInterceptorThrowsApolloException() throws Exception {
    final NamedCountDownLatch responseLatch = new NamedCountDownLatch("responseLatch", 1);

    final String message = "ApolloException";
    EpisodeHeroNameQuery query = createHeroNameQuery();
    ApolloInterceptor interceptor = new ApolloInterceptor() {
      @Override
      public void interceptAsync(@Nonnull InterceptorRequest request, @Nonnull ApolloInterceptorChain chain,
          @Nonnull Executor dispatcher, @Nonnull CallBack callBack) {
        ApolloException apolloException = new ApolloParseException(message);
        callBack.onFailure(apolloException);
      }

      @Override public void dispose() {

      }
    };

    client = createApolloClient(interceptor, Utils.immediateExecutor());
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

    responseLatch.await();
  }

  @Test
  public void asyncApplicationInterceptorThrowsRuntimeException() throws TimeoutException, InterruptedException {
    NamedCountDownLatch latch = new NamedCountDownLatch("latch", 1);
    final String message = "RuntimeException";
    EpisodeHeroNameQuery query = createHeroNameQuery();
    ApolloInterceptor interceptor = new ApolloInterceptor() {
      @Override
      public void interceptAsync(@Nonnull InterceptorRequest request, @Nonnull ApolloInterceptorChain chain,
          @Nonnull Executor dispatcher, @Nonnull CallBack callBack) {
        dispatcher.execute(new Runnable() {
          @Override public void run() {
            throw new RuntimeException(message);
          }
        });
      }

      @Override public void dispose() {

      }
    };

    client = createApolloClient(interceptor, new ExceptionHandlingExecutor(message, RuntimeException.class, latch));

    client
        .query(query)
        .enqueue(new ApolloCall.Callback<EpisodeHeroNameQuery.Data>() {
          @Override public void onResponse(@Nonnull Response<EpisodeHeroNameQuery.Data> response) {

          }

          @Override public void onFailure(@Nonnull ApolloException e) {

          }
        });

    latch.await();
  }

  @Test
  public void asyncApplicationInterceptorReturnsNull() throws TimeoutException, InterruptedException {
    NamedCountDownLatch latch = new NamedCountDownLatch("first", 1);
    EpisodeHeroNameQuery query = createHeroNameQuery();
    ApolloInterceptor interceptor = new ApolloInterceptor() {
      @Override
      public void interceptAsync(@Nonnull InterceptorRequest request, @Nonnull ApolloInterceptorChain chain,
          @Nonnull Executor dispatcher, @Nonnull final CallBack callBack) {
        dispatcher.execute(new Runnable() {
          @Override public void run() {
            callBack.onResponse(null);
          }
        });
      }

      @Override public void dispose() {

      }
    };

    client = createApolloClient(interceptor, new ExceptionHandlingExecutor(null, NullPointerException.class, latch));

    client.query(query).enqueue(new ApolloCall.Callback<EpisodeHeroNameQuery.Data>() {
      @Override public void onResponse(@Nonnull Response<EpisodeHeroNameQuery.Data> response) {

      }

      @Override public void onFailure(@Nonnull ApolloException e) {

      }
    });

    latch.await();
  }

  @Test
  public void applicationInterceptorCanMakeMultipleRequestsToServer() throws Exception {
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_CHANGE));
    EpisodeHeroNameQuery query = createHeroNameQuery();
    ApolloInterceptor interceptor = new ApolloInterceptor() {
      @Override
      public void interceptAsync(@Nonnull InterceptorRequest request, @Nonnull ApolloInterceptorChain chain,
          @Nonnull Executor dispatcher, @Nonnull CallBack callBack) {
        chain.proceedAsync(request, dispatcher, callBack);
      }

      @Override public void dispose() {
      }
    };

    client = createApolloClient(interceptor, Utils.immediateExecutor());

    enqueueAndAssertResponse(
        server,
        FILE_EPISODE_HERO_NAME_WITH_ID,
        client.query(query),
        new Predicate<Response<EpisodeHeroNameQuery.Data>>() {
          @Override public boolean test(Response<EpisodeHeroNameQuery.Data> response) throws Exception {
            assertThat(response.data().hero().name()).isEqualTo("Artoo");
            return true;
          }
        }
    );
  }

  @Test
  public void onShortCircuitingResponseSubsequentInterceptorsAreNotCalled() throws IOException, ApolloException {
    EpisodeHeroNameQuery query = createHeroNameQuery();
    final InterceptorResponse expectedResponse = prepareInterceptorResponse(query);

    ApolloInterceptor firstInterceptor = new ApolloInterceptor() {
      @Override
      public void interceptAsync(@Nonnull InterceptorRequest request, @Nonnull ApolloInterceptorChain chain,
          @Nonnull Executor dispatcher, @Nonnull CallBack callBack) {
        callBack.onResponse(expectedResponse);
      }

      @Override public void dispose() {
      }
    };

    ApolloInterceptor secondInterceptor = new ApolloInterceptor() {
      @Override
      public void interceptAsync(@Nonnull InterceptorRequest request, @Nonnull ApolloInterceptorChain chain,
          @Nonnull Executor dispatcher, @Nonnull CallBack callBack) {
        chain.proceedAsync(request, dispatcher, callBack);
      }

      @Override public void dispose() {

      }
    };

    client = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .okHttpClient(okHttpClient)
        .addApplicationInterceptor(firstInterceptor)
        .addApplicationInterceptor(secondInterceptor)
        .build();

    assertResponse(
        client.query(query),
        new Predicate<Response<EpisodeHeroNameQuery.Data>>() {
          @Override public boolean test(Response<EpisodeHeroNameQuery.Data> response) throws Exception {
            assertThat(expectedResponse.parsedResponse.get()).isEqualTo(response);
            return true;
          }
        }
    );
  }

  @Test
  public void onApolloCallCanceledAsyncApolloInterceptorIsDisposed() throws ApolloException, TimeoutException,
      InterruptedException, IOException {
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));

    EpisodeHeroNameQuery query = createHeroNameQuery();
    SpyingApolloInterceptor interceptor = new SpyingApolloInterceptor();

    Utils.TestExecutor testExecutor = new Utils.TestExecutor();
    client = createApolloClient(interceptor, testExecutor);

    ApolloCall<EpisodeHeroNameQuery.Data> apolloCall = client.query(query);

    apolloCall.enqueue(new ApolloCall.Callback<EpisodeHeroNameQuery.Data>() {
      @Override public void onResponse(@Nonnull Response<EpisodeHeroNameQuery.Data> response) {
      }

      @Override public void onFailure(@Nonnull ApolloException e) {
      }
    });
    apolloCall.cancel();
    testExecutor.triggerActions();
    assertThat(interceptor.isDisposed).isTrue();
  }

  @NonNull private EpisodeHeroNameQuery createHeroNameQuery() {
    return EpisodeHeroNameQuery
        .builder()
        .episode(Episode.EMPIRE)
        .build();
  }

  private ApolloClient createApolloClient(ApolloInterceptor interceptor, Executor dispatcher) {
    return ApolloClient.builder()
        .serverUrl(server.url("/"))
        .okHttpClient(okHttpClient)
        .addApplicationInterceptor(interceptor)
        .dispatcher(dispatcher)
        .build();
  }

  @NonNull private InterceptorResponse prepareInterceptorResponse(EpisodeHeroNameQuery query) {
    Request request = new Request.Builder()
        .url(server.url("/"))
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

  private static class ExceptionHandlingExecutor extends ThreadPoolExecutor {

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
            fail();
          } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo(message);
            assertThat(e).isInstanceOf(exceptionClass);
            latch.countDown();
          }
        }
      });
    }
  }

  private static class SpyingApolloInterceptor implements ApolloInterceptor {

    volatile boolean isDisposed = false;

    @Override
    public void interceptAsync(@Nonnull InterceptorRequest request, @Nonnull ApolloInterceptorChain chain, @Nonnull Executor dispatcher, @Nonnull CallBack callBack) {
      chain.proceedAsync(request, dispatcher, callBack);
    }

    @Override public void dispose() {
      isDisposed = true;
    }
  }
}
