package com.apollographql.apollo.internal.interceptor;

import com.apollographql.apollo.api.Error;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.CustomScalarAdapters;
import com.apollographql.apollo.api.internal.ApolloLogger;
import com.apollographql.apollo.api.internal.ResponseFieldMapper;
import com.apollographql.apollo.api.internal.ResponseFieldMarshaller;
import com.apollographql.apollo.cache.normalized.Record;
import com.apollographql.apollo.interceptor.ApolloAutoPersistedOperationInterceptor;
import com.apollographql.apollo.interceptor.ApolloInterceptor;
import com.apollographql.apollo.interceptor.ApolloInterceptorChain;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSource;
import okio.ByteString;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ApolloAutoPersistedOperationInterceptorTest {
  private ApolloAutoPersistedOperationInterceptor interceptor =
      new ApolloAutoPersistedOperationInterceptor(new ApolloLogger(null), false);

  private ApolloAutoPersistedOperationInterceptor interceptorWithGetMethod =
      new ApolloAutoPersistedOperationInterceptor(new ApolloLogger(null), true);

  private ApolloInterceptor.InterceptorRequest request = ApolloInterceptor.InterceptorRequest.builder(new MockOperation())
      .autoPersistQueries(true)
      .build();

  @Test
  public void initialRequestWithoutQueryDocument() {
    ApolloInterceptorChain chain = mock(ApolloInterceptorChain.class);

    interceptor.interceptAsync(request, chain, new TrampolineExecutor(), mock(ApolloInterceptor.CallBack.class));

    ArgumentCaptor<ApolloInterceptor.InterceptorRequest> requestArgumentCaptor =
        ArgumentCaptor.forClass(ApolloInterceptor.InterceptorRequest.class);
    verify(chain).proceedAsync(requestArgumentCaptor.capture(), any(Executor.class),
        any(ApolloInterceptor.CallBack.class));

    assertThat(requestArgumentCaptor.getValue().sendQueryDocument).isFalse();
    assertThat(requestArgumentCaptor.getValue().useHttpGetMethodForQueries).isFalse();
    assertThat(requestArgumentCaptor.getValue().autoPersistQueries).isTrue();
  }

  @Test
  public void initialRequestWithGetMethodForPersistedQueries() {
    ApolloInterceptorChain chain = mock(ApolloInterceptorChain.class);

    interceptorWithGetMethod.interceptAsync(request, chain, new TrampolineExecutor(), mock(ApolloInterceptor.CallBack.class));

    ArgumentCaptor<ApolloInterceptor.InterceptorRequest> requestArgumentCaptor =
        ArgumentCaptor.forClass(ApolloInterceptor.InterceptorRequest.class);
    verify(chain).proceedAsync(requestArgumentCaptor.capture(), any(Executor.class),
        any(ApolloInterceptor.CallBack.class));

    assertThat(requestArgumentCaptor.getValue().sendQueryDocument).isFalse();
    assertThat(requestArgumentCaptor.getValue().useHttpGetMethodForQueries).isTrue();
    assertThat(requestArgumentCaptor.getValue().autoPersistQueries).isTrue();
  }

  @Test
  public void onPersistedQueryNotFoundErrorRequestWithQueryDocument() {
    ApolloInterceptorChainAdapter chain = new ApolloInterceptorChainAdapter() {
      @Override
      public void proceedAsync(@NotNull ApolloInterceptor.InterceptorRequest request, @NotNull Executor dispatcher,
          @NotNull ApolloInterceptor.CallBack callBack) {
        super.proceedAsync(request, dispatcher, callBack);
        if (proceedAsyncInvocationCount == 1) {
          assertThat(request.sendQueryDocument).isFalse();
          assertThat(request.autoPersistQueries).isTrue();
          callBack.onResponse(
              new ApolloInterceptor.InterceptorResponse(
                  mockHttpResponse(),
                  com.apollographql.apollo.api.Response.<MockOperation.Data>builder(new MockOperation())
                      .errors(
                          Collections.singletonList(
                              new Error("PersistedQueryNotFound", Collections.emptyList(), Collections.emptyMap())
                          )
                      )
                      .build(),
                  Collections.<Record>emptyList()
              )
          );
        } else if (proceedAsyncInvocationCount == 2) {
          assertThat(request.sendQueryDocument).isTrue();
          assertThat(request.autoPersistQueries).isTrue();
          callBack.onResponse(
              new ApolloInterceptor.InterceptorResponse(
                  mockHttpResponse(),
                  com.apollographql.apollo.api.Response.<MockOperation.Data>builder(new MockOperation())
                      .data(new MockOperation.Data())
                      .build(),
                  Collections.<Record>emptyList()
              )
          );
        } else {
          fail("expected only 2 invocation first without query document, second with it");
        }
      }
    };
    ApolloInterceptor.CallBack interceptorCallBack = mock(ApolloInterceptor.CallBack.class);

    interceptor.interceptAsync(request, chain, new TrampolineExecutor(), interceptorCallBack);

    assertThat(chain.proceedAsyncInvocationCount).isEqualTo(2);

    ArgumentCaptor<ApolloInterceptor.InterceptorResponse> interceptorResponseArgumentCaptor =
        ArgumentCaptor.forClass(ApolloInterceptor.InterceptorResponse.class);
    verify(interceptorCallBack).onResponse(interceptorResponseArgumentCaptor.capture());
    assertThat(interceptorResponseArgumentCaptor.getValue().parsedResponse.get().hasErrors()).isFalse();
    assertThat(interceptorResponseArgumentCaptor.getValue().parsedResponse.get().getData()).isNotNull();
  }

  @Test
  public void onPersistedQueryNotSupportedErrorRequestWithQueryDocument() {
    ApolloInterceptorChainAdapter chain = new ApolloInterceptorChainAdapter() {
      @Override
      public void proceedAsync(@NotNull ApolloInterceptor.InterceptorRequest request, @NotNull Executor dispatcher,
          @NotNull ApolloInterceptor.CallBack callBack) {
        super.proceedAsync(request, dispatcher, callBack);
        if (proceedAsyncInvocationCount == 1) {
          assertThat(request.sendQueryDocument).isFalse();
          assertThat(request.autoPersistQueries).isTrue();
          callBack.onResponse(
              new ApolloInterceptor.InterceptorResponse(
                  mockHttpResponse(),
                  com.apollographql.apollo.api.Response.<MockOperation.Data>builder(new MockOperation())
                      .errors(
                          Collections.singletonList(
                              new Error("PersistedQueryNotSupported", Collections.emptyList(), Collections.emptyMap())
                          )
                      )
                      .build(),
                  Collections.<Record>emptyList()
              )
          );
        } else if (proceedAsyncInvocationCount == 2) {
          assertThat(request.sendQueryDocument).isTrue();
          assertThat(request.autoPersistQueries).isTrue();
          callBack.onResponse(
              new ApolloInterceptor.InterceptorResponse(
                  mockHttpResponse(),
                  com.apollographql.apollo.api.Response.<MockOperation.Data>builder(new MockOperation())
                      .data(new MockOperation.Data())
                      .build(),
                  Collections.<Record>emptyList()
              )
          );
        } else {
          fail("expected only 2 invocation first without query document, second with it");
        }
      }
    };
    ApolloInterceptor.CallBack interceptorCallBack = mock(ApolloInterceptor.CallBack.class);

    interceptor.interceptAsync(request, chain, new TrampolineExecutor(), interceptorCallBack);

    assertThat(chain.proceedAsyncInvocationCount).isEqualTo(2);

    ArgumentCaptor<ApolloInterceptor.InterceptorResponse> interceptorResponseArgumentCaptor =
        ArgumentCaptor.forClass(ApolloInterceptor.InterceptorResponse.class);
    verify(interceptorCallBack).onResponse(interceptorResponseArgumentCaptor.capture());
    assertThat(interceptorResponseArgumentCaptor.getValue().parsedResponse.get().hasErrors()).isFalse();
    assertThat(interceptorResponseArgumentCaptor.getValue().parsedResponse.get().getData()).isNotNull();
  }

  @Test
  public void onNonPersistedQueryErrorOriginalCallbackCalled() {
    ApolloInterceptorChain chain = mock(ApolloInterceptorChain.class);
    doAnswer(new Answer() {
      @Override public Object answer(InvocationOnMock invocation) throws Throwable {
        ((ApolloInterceptor.CallBack) invocation.getArguments()[2]).onResponse(
            new ApolloInterceptor.InterceptorResponse(
                mockHttpResponse(),
                com.apollographql.apollo.api.Response.<MockOperation.Data>builder(new MockOperation())
                    .errors(
                        Collections.singletonList(
                            new Error("SomeOtherError", Collections.<Error.Location>emptyList(), Collections.<String, Object>emptyMap())
                        )
                    )
                    .build(),
                Collections.<Record>emptyList()
            )
        );
        return null;
      }
    }).when(chain).proceedAsync(
        any(ApolloInterceptor.InterceptorRequest.class),
        any(Executor.class),
        any(ApolloInterceptor.CallBack.class)
    );

    ApolloInterceptor.CallBack interceptorCallBack = mock(ApolloInterceptor.CallBack.class);

    interceptor.interceptAsync(request, chain, new TrampolineExecutor(), interceptorCallBack);

    verify(chain).proceedAsync(any(ApolloInterceptor.InterceptorRequest.class), any(Executor.class),
        any(ApolloInterceptor.CallBack.class));

    ArgumentCaptor<ApolloInterceptor.InterceptorResponse> interceptorResponseArgumentCaptor =
        ArgumentCaptor.forClass(ApolloInterceptor.InterceptorResponse.class);
    verify(interceptorCallBack).onResponse(interceptorResponseArgumentCaptor.capture());
    assertThat(interceptorResponseArgumentCaptor.getValue().parsedResponse.get().hasErrors()).isTrue();
  }

  @Test
  public void onPersistedQueryFoundCallbackCalled() {
    ApolloInterceptorChain chain = mock(ApolloInterceptorChain.class);
    doAnswer(new Answer() {
      @Override public Object answer(InvocationOnMock invocation) throws Throwable {
        ((ApolloInterceptor.CallBack) invocation.getArguments()[2]).onResponse(
            new ApolloInterceptor.InterceptorResponse(
                mockHttpResponse(),
                com.apollographql.apollo.api.Response.<MockOperation.Data>builder(new MockOperation())
                    .data(new MockOperation.Data())
                    .build(),
                Collections.<Record>emptyList()
            )
        );
        return null;
      }
    }).when(chain).proceedAsync(
        any(ApolloInterceptor.InterceptorRequest.class),
        any(Executor.class),
        any(ApolloInterceptor.CallBack.class)
    );

    ApolloInterceptor.CallBack interceptorCallBack = mock(ApolloInterceptor.CallBack.class);

    interceptor.interceptAsync(request, chain, new TrampolineExecutor(), interceptorCallBack);

    verify(chain).proceedAsync(any(ApolloInterceptor.InterceptorRequest.class), any(Executor.class),
        any(ApolloInterceptor.CallBack.class));

    ArgumentCaptor<ApolloInterceptor.InterceptorResponse> interceptorResponseArgumentCaptor =
        ArgumentCaptor.forClass(ApolloInterceptor.InterceptorResponse.class);
    verify(interceptorCallBack).onResponse(interceptorResponseArgumentCaptor.capture());
    assertThat(interceptorResponseArgumentCaptor.getValue().parsedResponse.get().getData()).isNotNull();
    assertThat(interceptorResponseArgumentCaptor.getValue().parsedResponse.get().hasErrors()).isFalse();
  }

  private Response mockHttpResponse() {
    return new okhttp3.Response.Builder()
        .request(new Request.Builder()
            .url("https://localhost/")
            .build())
        .protocol(Protocol.HTTP_2)
        .code(200)
        .message("Intercepted")
        .body(ResponseBody.create(MediaType.parse("text/plain; charset=utf-8"), "fakeResponse"))
        .build();
  }

  static class MockOperation implements Operation<MockOperation.Data, Operation.Variables> {

    @Override public String queryDocument() {
      throw new UnsupportedOperationException();
    }

    @Override public Variables variables() {
      throw new UnsupportedOperationException();
    }

    @Override public ResponseFieldMapper<Data> responseFieldMapper() {
      throw new UnsupportedOperationException();
    }

    @NotNull @Override public OperationName name() {
      return new OperationName() {
        @Override public String name() {
          return "MockOperation";
        }
      };
    }

    @NotNull @Override public String operationId() {
      return UUID.randomUUID().toString();
    }

    static class Data implements Operation.Data {
      @Override public ResponseFieldMarshaller marshaller() {
        throw new UnsupportedOperationException();
      }
    }

    @NotNull @Override public com.apollographql.apollo.api.Response<Data> parse(@NotNull BufferedSource source) {
      throw new UnsupportedOperationException();
    }

    @NotNull @Override public com.apollographql.apollo.api.Response<Data> parse(
        @NotNull BufferedSource source,
        @NotNull CustomScalarAdapters customScalarAdapters
    ) {
      throw new UnsupportedOperationException();
    }

    @NotNull @Override public com.apollographql.apollo.api.Response parse(@NotNull ByteString byteString) {
      throw new UnsupportedOperationException();
    }

    @NotNull @Override public com.apollographql.apollo.api.Response parse(
        @NotNull ByteString byteString,
        @NotNull CustomScalarAdapters customScalarAdapters
    ) {
      throw new UnsupportedOperationException();
    }

    @NotNull @Override public ByteString composeRequestBody(
        boolean autoPersistQueries,
        boolean withQueryDocument,
        @NotNull CustomScalarAdapters customScalarAdapters) {
      throw new UnsupportedOperationException();
    }

    @NotNull @Override public ByteString composeRequestBody(@NotNull CustomScalarAdapters customScalarAdapters) {
      throw new UnsupportedOperationException();
    }

    @NotNull @Override public ByteString composeRequestBody() {
      throw new UnsupportedOperationException();
    }
  }

  static class TrampolineExecutor extends AbstractExecutorService {
    @Override public void shutdown() {
    }

    @Override public List<Runnable> shutdownNow() {
      return null;
    }

    @Override public boolean isShutdown() {
      return false;
    }

    @Override public boolean isTerminated() {
      return false;
    }

    @Override public boolean awaitTermination(long l, TimeUnit timeUnit) {
      return false;
    }

    @Override public void execute(Runnable runnable) {
      runnable.run();
    }
  }

  static class ApolloInterceptorChainAdapter implements ApolloInterceptorChain {
    int proceedAsyncInvocationCount;

    @Override
    public void proceedAsync(@NotNull ApolloInterceptor.InterceptorRequest request, @NotNull Executor dispatcher,
        @NotNull ApolloInterceptor.CallBack callBack) {
      proceedAsyncInvocationCount++;
    }

    @Override public void dispose() {
    }
  }
}
