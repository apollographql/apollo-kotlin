package com.apollographql.apollo.internal.interceptor;

import com.apollographql.apollo.Utils;
import com.apollographql.apollo.api.CustomTypeAdapter;
import com.apollographql.apollo.api.FileUpload;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.ScalarType;
import com.apollographql.apollo.api.ScalarTypeAdapters;
import com.apollographql.apollo.api.cache.http.HttpCache;
import com.apollographql.apollo.api.internal.ApolloLogger;
import com.apollographql.apollo.cache.CacheHeaders;
import com.apollographql.apollo.integration.upload.MultipleUploadMutation;
import com.apollographql.apollo.integration.upload.NestedUploadMutation;
import com.apollographql.apollo.integration.upload.SingleUploadMutation;
import com.apollographql.apollo.integration.upload.SingleUploadTwiceMutation;
import com.apollographql.apollo.integration.upload.type.NestedObject;
import com.apollographql.apollo.request.RequestHeaders;
import com.google.common.base.Predicate;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.Response;
import okio.Buffer;
import okio.Timeout;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static com.google.common.truth.Truth.assertThat;
import static junit.framework.Assert.fail;

public class ApolloServerInterceptorFileUploadTest {
  private final HttpUrl serverUrl = HttpUrl.parse("http://google.com");
  private final String file0 = createFile("file0.txt", "content_file0");
  private final String file1 = createFile("file1.jpg", "content_file1");
  private final String file2 = createFile("file2.png", "content_file2");
  private final FileUpload upload0 = new FileUpload("plain/txt", file0);
  private final FileUpload upload1 = new FileUpload("image/jpg", file1);
  private final FileUpload upload2 = new FileUpload("image/png", file2);
  private NestedObject nestedObject0 = NestedObject.builder()
      .file(upload0)
      .fileList(new ArrayList<>(Arrays.asList(upload1, upload2)))
      .build();
  private NestedObject nestedObject1 = NestedObject.builder()
      .file(upload1)
      .fileList(new ArrayList<>(Arrays.asList(upload0, upload2)))
      .build();
  private NestedObject nestedObject2 = NestedObject.builder()
      .file(upload2)
      .fileList(new ArrayList<>(Arrays.asList(upload0, upload1)))
      .recursiveNested(new ArrayList<>(Arrays.asList(nestedObject0, nestedObject1)))
      .build();

  private final SingleUploadMutation mutationSingle = SingleUploadMutation.builder()
      .file(upload1)
      .build();

  private final SingleUploadTwiceMutation mutationTwice = SingleUploadTwiceMutation.builder()
      .file1(upload1)
      .file2(upload2)
      .build();

  private MultipleUploadMutation mutationMultiple = null;

  private final NestedUploadMutation mutationNested = NestedUploadMutation.builder()
      .nested(nestedObject2)
      .topFile(upload2)
      .topFileList(new ArrayList<>(Arrays.asList(upload1, upload0)))
      .build();

  private String createFile(String fileName, String content) {
    String tempDir = System.getProperty("java.io.tmpdir");
    String filePath = tempDir + "/" + fileName;
    File f = new File(filePath);
    try {
      BufferedWriter bw = Files.newBufferedWriter(f.toPath(), StandardCharsets.UTF_8);
      bw.write(content);
      bw.close();
    } catch (Exception e) {
    }
    return f.getPath();
  }

  @Before public void prepare() {
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
        new ScalarTypeAdapters(Collections.<ScalarType, CustomTypeAdapter<?>>emptyMap()),
        new ApolloLogger(null));

    interceptor.httpPostCall(mutationSingle, CacheHeaders.NONE, RequestHeaders.NONE, true, false);
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
        new ScalarTypeAdapters(Collections.<ScalarType, CustomTypeAdapter<?>>emptyMap()),
        new ApolloLogger(null));

    interceptor.httpPostCall(mutationTwice, CacheHeaders.NONE, RequestHeaders.NONE, true, false);
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
        new ScalarTypeAdapters(Collections.<ScalarType, CustomTypeAdapter<?>>emptyMap()),
        new ApolloLogger(null));

    interceptor.httpPostCall(mutationMultiple, CacheHeaders.NONE, RequestHeaders.NONE, true, false);
  }

  @Test public void testDefaultHttpCallWithUploadNested() throws Exception {
    Predicate<Request> requestAssertPredicate = new Predicate<Request>() {

      @Override
      public boolean apply(@Nullable Request request) {
        assertThat(request).isNotNull();
        assertDefaultRequestHeaders(request, mutationNested);
        assertRequestBodyNested(request);
        return true;
      }
    };

    ApolloServerInterceptor interceptor = new ApolloServerInterceptor(serverUrl,
        new AssertHttpCallFactory(requestAssertPredicate), null, false,
        new ScalarTypeAdapters(Collections.<ScalarType, CustomTypeAdapter<?>>emptyMap()),
        new ApolloLogger(null));

    interceptor.httpPostCall(mutationNested, CacheHeaders.NONE, RequestHeaders.NONE,true, false);
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
        new ScalarTypeAdapters(Collections.<ScalarType, CustomTypeAdapter<?>>emptyMap()),
        new ApolloLogger(null));

    interceptor.httpPostCall(mutationSingle, CacheHeaders.NONE, requestHeaders, true, false);
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
    assertOperationsPart(part0, "expectedOperationsPartBodySingle.json");

    MultipartBody.Part part1 = body.parts().get(1);
    assertMapPart(part1, "expectedMapPartBodySingle.json");

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
    assertOperationsPart(part0, "expectedOperationsPartBodyTwice.json");

    MultipartBody.Part part1 = body.parts().get(1);
    assertMapPart(part1, "expectedMapPartBodyTwice.json");

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
    assertOperationsPart(part0, "expectedOperationsPartBodyMultiple.json");

    MultipartBody.Part part1 = body.parts().get(1);
    assertMapPart(part1, "expectedMapPartBodyMultiple.json");

    MultipartBody.Part part2 = body.parts().get(2);
    assertFileContentPart(part2, "0", "file1.jpg", "image/jpg");

    MultipartBody.Part part3 = body.parts().get(3);
    assertFileContentPart(part3, "1", "file2.png", "image/png");
  }

  private void assertRequestBodyNested(Request request) {
    assertThat(request.body()).isInstanceOf(MultipartBody.class);
    MultipartBody body = (MultipartBody) request.body();
    assertThat(body.contentType().type()).isEqualTo("multipart");
    assertThat(body.contentType().subtype()).isEqualTo("form-data");
    assertThat(body.parts().size()).isEqualTo(14);

    // Check
    MultipartBody.Part part0 = body.parts().get(0);
    assertOperationsPart(part0, "expectedOperationsPartBodyNested.json");

    MultipartBody.Part part1 = body.parts().get(1);
    assertMapPart(part1, "expectedMapPartBodyNested.json");
  }

  private void assertOperationsPart(MultipartBody.Part part, String expectedPath) {
    assertThat(part.headers().get("Content-Disposition")).isEqualTo("form-data; name=\"operations\"");
    assertThat(part.body().contentType()).isEqualTo(ApolloServerInterceptor.MEDIA_TYPE);
    Buffer bodyBuffer = new Buffer();
    try {
      part.body().writeTo(bodyBuffer);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    Utils.INSTANCE.checkTestFixture(bodyBuffer.readUtf8(), "ApolloServerInterceptorFileUploadTest/" + expectedPath);
  }

  private void assertMapPart(MultipartBody.Part part, String expectedPath) {
    assertThat(part.headers().get("Content-Disposition")).isEqualTo("form-data; name=\"map\"");
    assertThat(part.body().contentType()).isEqualTo(ApolloServerInterceptor.MEDIA_TYPE);
    Buffer bodyBuffer = new Buffer();
    try {
      part.body().writeTo(bodyBuffer);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    Utils.INSTANCE.checkTestFixture(bodyBuffer.readUtf8(), "ApolloServerInterceptorFileUploadTest/" + expectedPath);
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
