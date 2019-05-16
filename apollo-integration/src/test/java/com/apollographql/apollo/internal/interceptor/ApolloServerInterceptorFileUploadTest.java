package com.apollographql.apollo.internal.interceptor;

import com.google.common.base.Predicate;

import com.apollographql.apollo.Logger;
import com.apollographql.apollo.api.FileUpload;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.ScalarType;
import com.apollographql.apollo.api.cache.http.HttpCache;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.cache.CacheHeaders;
import com.apollographql.apollo.integration.upload.MultipleUploadMutation;
import com.apollographql.apollo.integration.upload.SingleUploadMutation;
import com.apollographql.apollo.integration.upload.SingleUploadTwiceMutation;
import com.apollographql.apollo.internal.ApolloLogger;
import com.apollographql.apollo.request.RequestHeaders;
import com.apollographql.apollo.response.CustomTypeAdapter;
import com.apollographql.apollo.response.ScalarTypeAdapters;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.Response;
import okio.Buffer;
import okio.Timeout;

import static com.google.common.truth.Truth.assertThat;
import static junit.framework.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ApolloServerInterceptorFileUploadTest {
  private final HttpUrl serverUrl = HttpUrl.parse("http://google.com");
  private final File file1 = mock(File.class);
  private final File file2 = mock(File.class);
  private final FileUpload upload1 = new FileUpload("image/jpg", file1);
  private final FileUpload upload2 = new FileUpload("image/png", file2);

  private final SingleUploadMutation mutationSingle = SingleUploadMutation.builder()
      .file(upload1)
      .build();

  private final String expectedOperationsPartBodySingle = "{\"operationName\":\"SingleUpload\"," +
      "\"variables\":{\"file\":null}," +
      "\"extensions\":{\"persistedQuery\":{\"version\":1," +
      "\"sha256Hash\":\"" + SingleUploadMutation.OPERATION_ID + "\"}}," +
      "\"query\":\"mutation SingleUpload($file: Upload!) {  " +
      "singleUpload(file: $file) {    __typename    id    path    filename    mimetype  }}\"}";
  private final String expectedMapPartBodySingle = "{\"0\":[\"variables.file\"]}";

  private final SingleUploadTwiceMutation mutationTwice = SingleUploadTwiceMutation.builder()
      .file1(upload1)
      .file2(upload2)
      .build();
  private final String expectedOperationsPartBodyTwice = "{\"operationName\":\"SingleUploadTwice\"," +
      "\"variables\":{\"file1\":null,\"file2\":null}," +
      "\"extensions\":{\"persistedQuery\":{\"version\":1," +
      "\"sha256Hash\":\"" + SingleUploadTwiceMutation.OPERATION_ID + "\"}}," +
      "\"query\":\"mutation SingleUploadTwice($file1: Upload!, $file2: Upload!) {  " +
      "file1: singleUpload(file: $file1) {    __typename    id    path    filename    mimetype  }  " +
      "file2: singleUpload(file: $file2) {    __typename    id    path    filename    mimetype  }}\"}";
  private final String expectedMapPartBodyTwice = "{\"0\":[\"variables.file1\"],\"1\":[\"variables.file2\"]}";

  private MultipleUploadMutation mutationMultiple = null;
  private final String expectedOperationsPartBodyMultiple = "{\"operationName\":\"MultipleUpload\"," +
      "\"variables\":{\"files\":[null,null]}," +
      "\"extensions\":{\"persistedQuery\":{\"version\":1," +
      "\"sha256Hash\":\"" + MultipleUploadMutation.OPERATION_ID + "\"}}," +
      "\"query\":\"mutation MultipleUpload($files: [Upload!]!) {  " +
      "multipleUpload(files: $files) {    __typename    id    path    filename    mimetype  }}\"}";
  private final String expectedMapPartBodyMultiple = "{\"0\":[\"variables.files.0\"],\"1\":[\"variables.files.1\"]}";

  @Before public void prepare() {
    when(file1.getName()).thenReturn("file1.jpg");
    when(file2.getName()).thenReturn("file2.png");

    ArrayList<FileUpload> uploads = new ArrayList<>();
    uploads.add(upload1);
    uploads.add(upload2);
    mutationMultiple = MultipleUploadMutation.builder()
        .files(uploads)
        .build();
  }

  @Test public void testDefaultHttpCallWithUploadSingle() throws Exception {
    Predicate<Request> requestAssertPredicate = new Predicate<Request>() {
      @Override public boolean apply(@Nullable Request request) {
        assertThat(request).isNotNull();
        assertDefaultRequestHeaders(request, mutationSingle);
        assertThat(request.header(HttpCache.CACHE_KEY_HEADER)).isNull();
        assertThat(request.header(HttpCache.CACHE_FETCH_STRATEGY_HEADER)).isNull();
        assertThat(request.header(HttpCache.CACHE_EXPIRE_TIMEOUT_HEADER)).isNull();
        assertThat(request.header(HttpCache.CACHE_EXPIRE_AFTER_READ_HEADER)).isNull();
        assertThat(request.header(HttpCache.CACHE_PREFETCH_HEADER)).isNull();
        assertRequestBodySingle(request);
        return true;
      }
    };

    ApolloServerInterceptor interceptor = new ApolloServerInterceptor(serverUrl,
        new AssertHttpCallFactory(requestAssertPredicate), null, false,
        new ScalarTypeAdapters(Collections.<ScalarType, CustomTypeAdapter>emptyMap()),
        new ApolloLogger(Optional.<Logger>absent()),
        false);

    interceptor.httpPostCall(mutationSingle, CacheHeaders.NONE, RequestHeaders.NONE, true);
  }

  @Test public void testDefaultHttpCallWithUploadTwice() throws Exception {
    Predicate<Request> requestAssertPredicate = new Predicate<Request>() {
      @Override public boolean apply(@Nullable Request request) {
        assertThat(request).isNotNull();
        assertDefaultRequestHeaders(request, mutationTwice);
        assertThat(request.header(HttpCache.CACHE_KEY_HEADER)).isNull();
        assertThat(request.header(HttpCache.CACHE_FETCH_STRATEGY_HEADER)).isNull();
        assertThat(request.header(HttpCache.CACHE_EXPIRE_TIMEOUT_HEADER)).isNull();
        assertThat(request.header(HttpCache.CACHE_EXPIRE_AFTER_READ_HEADER)).isNull();
        assertThat(request.header(HttpCache.CACHE_PREFETCH_HEADER)).isNull();
        assertRequestBodyTwice(request);
        return true;
      }
    };

    ApolloServerInterceptor interceptor = new ApolloServerInterceptor(serverUrl,
        new AssertHttpCallFactory(requestAssertPredicate), null, false,
        new ScalarTypeAdapters(Collections.<ScalarType, CustomTypeAdapter>emptyMap()),
        new ApolloLogger(Optional.<Logger>absent()),
        false);

    interceptor.httpPostCall(mutationTwice, CacheHeaders.NONE, RequestHeaders.NONE, true);
  }

  @Test public void testDefaultHttpCallWithUploadMultiple() throws Exception {
    Predicate<Request> requestAssertPredicate = new Predicate<Request>() {
      @Override public boolean apply(@Nullable Request request) {
        assertThat(request).isNotNull();
        assertDefaultRequestHeaders(request, mutationMultiple);
        assertThat(request.header(HttpCache.CACHE_KEY_HEADER)).isNull();
        assertThat(request.header(HttpCache.CACHE_FETCH_STRATEGY_HEADER)).isNull();
        assertThat(request.header(HttpCache.CACHE_EXPIRE_TIMEOUT_HEADER)).isNull();
        assertThat(request.header(HttpCache.CACHE_EXPIRE_AFTER_READ_HEADER)).isNull();
        assertThat(request.header(HttpCache.CACHE_PREFETCH_HEADER)).isNull();
        assertRequestBodyMultiple(request);
        return true;
      }
    };

    ApolloServerInterceptor interceptor = new ApolloServerInterceptor(serverUrl,
        new AssertHttpCallFactory(requestAssertPredicate), null, false,
        new ScalarTypeAdapters(Collections.<ScalarType, CustomTypeAdapter>emptyMap()),
        new ApolloLogger(Optional.<Logger>absent()),
        false);

    interceptor.httpPostCall(mutationMultiple, CacheHeaders.NONE, RequestHeaders.NONE, true);
  }

  @Test public void testAdditionalHeaders() throws Exception {
    final String testHeader1 = "TEST_HEADER_1";
    final String testHeaderValue1 = "crappy_value";
    final String testHeader2 = "TEST_HEADER_2";
    final String testHeaderValue2 = "fantastic_value";
    final String testHeader3 = "TEST_HEADER_3";
    final String testHeaderValue3 = "awesome_value";
    
    Predicate<Request> requestAssertPredicate = new Predicate<Request>() {
      @Override public boolean apply(@Nullable Request request) {
        assertThat(request).isNotNull();
        assertDefaultRequestHeaders(request, mutationSingle);
        assertThat(request.header(HttpCache.CACHE_KEY_HEADER)).isNull();
        assertThat(request.header(HttpCache.CACHE_FETCH_STRATEGY_HEADER)).isNull();
        assertThat(request.header(HttpCache.CACHE_EXPIRE_TIMEOUT_HEADER)).isNull();
        assertThat(request.header(HttpCache.CACHE_EXPIRE_AFTER_READ_HEADER)).isNull();
        assertThat(request.header(HttpCache.CACHE_PREFETCH_HEADER)).isNull();
        assertThat(request.header(testHeader1)).isEqualTo(testHeaderValue1);
        assertThat(request.header(testHeader2)).isEqualTo(testHeaderValue2);
        assertThat(request.header(testHeader3)).isEqualTo(testHeaderValue3);
        assertRequestBodySingle(request);
        return true;
      }
    };

    RequestHeaders requestHeaders = RequestHeaders.builder()
        .addHeader(testHeader1, testHeaderValue1)
        .addHeader(testHeader2, testHeaderValue2)
        .addHeader(testHeader3, testHeaderValue3)
        .build();

    ApolloServerInterceptor interceptor = new ApolloServerInterceptor(serverUrl,
        new AssertHttpCallFactory(requestAssertPredicate), null, false,
        new ScalarTypeAdapters(Collections.<ScalarType, CustomTypeAdapter>emptyMap()),
        new ApolloLogger(Optional.<Logger>absent()),
        false);

    interceptor.httpPostCall(mutationSingle, CacheHeaders.NONE, requestHeaders, true);
  }

  private void assertDefaultRequestHeaders(Request request, Operation mutation) {
    assertThat(request.url()).isEqualTo(serverUrl);
    assertThat(request.method()).isEqualTo("POST");
    assertThat(request.header(ApolloServerInterceptor.HEADER_ACCEPT_TYPE)).isEqualTo(ApolloServerInterceptor.ACCEPT_TYPE);
    assertThat(request.header(ApolloServerInterceptor.HEADER_CONTENT_TYPE)).isEqualTo(ApolloServerInterceptor.CONTENT_TYPE);
    assertThat(request.header(ApolloServerInterceptor.HEADER_APOLLO_OPERATION_ID)).isEqualTo(mutation.operationId());
    assertThat(request.header(ApolloServerInterceptor.HEADER_APOLLO_OPERATION_NAME)).isEqualTo(mutation.name().name());
    assertThat(request.tag()).isEqualTo(mutation.operationId());
  }

  private void assertRequestBodySingle(Request request) {
    assertThat(request.body()).isInstanceOf(MultipartBody.class);
    MultipartBody body = (MultipartBody)request.body();
    assertThat(body.contentType().type()).isEqualTo("multipart");
    assertThat(body.contentType().subtype()).isEqualTo("form-data");
    assertThat(body.parts().size()).isEqualTo(3);

    // Check
    MultipartBody.Part part0 = body.parts().get(0);
    assertOperationsPart(part0, expectedOperationsPartBodySingle);

    MultipartBody.Part part1 = body.parts().get(1);
    assertMapPart(part1, expectedMapPartBodySingle);

    MultipartBody.Part part2 = body.parts().get(2);
    assertFileContentPart(part2, "0", "file1.jpg", "image/jpg");
  }

  private void assertRequestBodyTwice(Request request) {
    assertThat(request.body()).isInstanceOf(MultipartBody.class);
    MultipartBody body = (MultipartBody)request.body();
    assertThat(body.contentType().type()).isEqualTo("multipart");
    assertThat(body.contentType().subtype()).isEqualTo("form-data");
    assertThat(body.parts().size()).isEqualTo(4);

    // Check
    MultipartBody.Part part0 = body.parts().get(0);
    assertOperationsPart(part0, expectedOperationsPartBodyTwice);

    MultipartBody.Part part1 = body.parts().get(1);
    assertMapPart(part1, expectedMapPartBodyTwice);

    MultipartBody.Part part2 = body.parts().get(2);
    assertFileContentPart(part2, "0", "file1.jpg", "image/jpg");

    MultipartBody.Part part3 = body.parts().get(3);
    assertFileContentPart(part3, "1", "file2.png", "image/png");
  }

  private void assertRequestBodyMultiple(Request request) {
    assertThat(request.body()).isInstanceOf(MultipartBody.class);
    MultipartBody body = (MultipartBody)request.body();
    assertThat(body.contentType().type()).isEqualTo("multipart");
    assertThat(body.contentType().subtype()).isEqualTo("form-data");
    assertThat(body.parts().size()).isEqualTo(4);

    // Check
    MultipartBody.Part part0 = body.parts().get(0);
    assertOperationsPart(part0, expectedOperationsPartBodyMultiple);

    MultipartBody.Part part1 = body.parts().get(1);
    assertMapPart(part1, expectedMapPartBodyMultiple);

    MultipartBody.Part part2 = body.parts().get(2);
    assertFileContentPart(part2, "0", "file1.jpg", "image/jpg");

    MultipartBody.Part part3 = body.parts().get(3);
    assertFileContentPart(part3, "1", "file2.png", "image/png");
  }

  private void assertOperationsPart(MultipartBody.Part part, String expected) {
    assertThat(part.headers().get("Content-Disposition")).isEqualTo("form-data; name=\"operations\"");
    assertThat(part.body().contentType()).isEqualTo(ApolloServerInterceptor.MEDIA_TYPE);
    Buffer bodyBuffer = new Buffer();
    try {
      part.body().writeTo(bodyBuffer);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    assertThat(bodyBuffer.readUtf8()).isEqualTo(expected);

  }

  private void assertMapPart(MultipartBody.Part part, String expected) {
    assertThat(part.headers().get("Content-Disposition")).isEqualTo("form-data; name=\"map\"");
    assertThat(part.body().contentType()).isEqualTo(ApolloServerInterceptor.MEDIA_TYPE);
    Buffer bodyBuffer = new Buffer();
    try {
      part.body().writeTo(bodyBuffer);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    assertThat(bodyBuffer.readUtf8()).isEqualTo(expected);
  }

  private void assertFileContentPart(MultipartBody.Part part, String expectedName, String expectedFileName,
      String expectedMimeType) {
    assertThat(part.headers().get("Content-Disposition")).isEqualTo("form-data; name=\"" + expectedName +
        "\"; filename=\"" + expectedFileName + "\"");
    assertThat(part.body().contentType()).isEqualTo(MediaType.parse(expectedMimeType));
  }

  private static class AssertHttpCallFactory implements Call.Factory {
    final Predicate<Request> predicate;

    AssertHttpCallFactory(Predicate<Request> predicate) {
      this.predicate = predicate;
    }

    @Override public Call newCall(@NotNull Request request) {
      if (!predicate.apply(request)) {
        fail("Assertion failed");
      }
      return new NoOpCall();
    }
  }

  private static class NoOpCall implements Call {
    @Override public Request request() {
      return null;
    }

    @Override public Response execute() {
      return null;
    }

    @Override public void enqueue(Callback responseCallback) {
    }

    @Override public void cancel() {
    }

    @Override public boolean isExecuted() {
      return false;
    }

    @Override public boolean isCanceled() {
      return false;
    }

    @Override public Call clone() {
      return null;
    }

    @Override public Timeout timeout() {
      return null;
    }
  }
}
