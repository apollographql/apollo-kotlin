package com.apollographql.apollo.internal.interceptor;

import com.apollographql.apollo.api.FileUpload;
import com.apollographql.apollo.api.Input;
import com.apollographql.apollo.api.InputType;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.ScalarTypeAdapters;
import com.apollographql.apollo.api.cache.http.HttpCache;
import com.apollographql.apollo.api.cache.http.HttpCachePolicy;
import com.apollographql.apollo.api.internal.ApolloLogger;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.api.internal.json.InputFieldJsonWriter;
import com.apollographql.apollo.api.internal.json.JsonWriter;
import com.apollographql.apollo.cache.ApolloCacheHeaders;
import com.apollographql.apollo.cache.CacheHeaders;
import com.apollographql.apollo.exception.ApolloNetworkException;
import com.apollographql.apollo.interceptor.ApolloInterceptor;
import com.apollographql.apollo.interceptor.ApolloInterceptorChain;
import com.apollographql.apollo.request.RequestHeaders;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;
import okio.ByteString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

/**
 * ApolloServerInterceptor is a concrete {@link ApolloInterceptor} responsible for making the network calls to the
 * server. It is the last interceptor in the chain of interceptors and hence doesn't call
 * {@link ApolloInterceptorChain#proceedAsync(InterceptorRequest, Executor, CallBack)}
 * on the interceptor chain.
 */
@SuppressWarnings("WeakerAccess")
public final class ApolloServerInterceptor implements ApolloInterceptor {
  static final String HEADER_ACCEPT_TYPE = "Accept";
  static final String HEADER_CONTENT_TYPE = "Content-Type";
  static final String HEADER_APOLLO_OPERATION_ID = "X-APOLLO-OPERATION-ID";
  static final String HEADER_APOLLO_OPERATION_NAME = "X-APOLLO-OPERATION-NAME";
  static final String ACCEPT_TYPE = "application/json";
  static final String CONTENT_TYPE = "application/json";
  static final MediaType MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

  final HttpUrl serverUrl;
  final okhttp3.Call.Factory httpCallFactory;
  final Optional<HttpCachePolicy.Policy> cachePolicy;
  final boolean prefetch;
  final ApolloLogger logger;
  final ScalarTypeAdapters scalarTypeAdapters;
  AtomicReference<Call> httpCallRef = new AtomicReference<>();
  volatile boolean disposed;

  public ApolloServerInterceptor(@NotNull HttpUrl serverUrl, @NotNull Call.Factory httpCallFactory,
      @Nullable HttpCachePolicy.Policy cachePolicy, boolean prefetch,
      @NotNull ScalarTypeAdapters scalarTypeAdapters, @NotNull ApolloLogger logger) {
    this.serverUrl = checkNotNull(serverUrl, "serverUrl == null");
    this.httpCallFactory = checkNotNull(httpCallFactory, "httpCallFactory == null");
    this.cachePolicy = Optional.fromNullable(cachePolicy);
    this.prefetch = prefetch;
    this.scalarTypeAdapters = checkNotNull(scalarTypeAdapters, "scalarTypeAdapters == null");
    this.logger = checkNotNull(logger, "logger == null");
  }

  @Override
  public void interceptAsync(@NotNull final InterceptorRequest request, @NotNull final ApolloInterceptorChain chain,
      @NotNull Executor dispatcher, @NotNull final CallBack callBack) {
    dispatcher.execute(new Runnable() {
      @Override public void run() {
        executeHttpCall(request, callBack);
      }
    });
  }

  @Override
  public void dispose() {
    disposed = true;

    final Call httpCall = httpCallRef.getAndSet(null);
    if (httpCall != null) {
      httpCall.cancel();
    }
  }

  void executeHttpCall(@NotNull final InterceptorRequest request, @NotNull final CallBack callBack) {
    if (disposed) return;

    callBack.onFetch(FetchSourceType.NETWORK);

    final Call httpCall;
    try {
      if (request.useHttpGetMethodForQueries && request.operation instanceof Query) {
        httpCall = httpGetCall(request.operation, request.cacheHeaders, request.requestHeaders,
            request.sendQueryDocument, request.autoPersistQueries);
      } else {
        httpCall = httpPostCall(request.operation, request.cacheHeaders, request.requestHeaders,
            request.sendQueryDocument, request.autoPersistQueries);
      }
    } catch (IOException e) {
      logger.e(e, "Failed to prepare http call for operation %s", request.operation.name().name());
      callBack.onFailure(new ApolloNetworkException("Failed to prepare http call", e));
      return;
    }

    final Call previousCall = httpCallRef.getAndSet(httpCall);
    if (previousCall != null) {
      previousCall.cancel();
    }

    if (httpCall.isCanceled() || disposed) {
      httpCallRef.compareAndSet(httpCall, null);
      return;
    }

    httpCall.enqueue(new Callback() {
      @Override
      public void onFailure(@NotNull Call call, @NotNull IOException e) {
        if (disposed) return;

        if (httpCallRef.compareAndSet(httpCall, null)) {
          logger.e(e, "Failed to execute http call for operation %s", request.operation.name().name());
          callBack.onFailure(new ApolloNetworkException("Failed to execute http call", e));
        }
      }

      @Override
      public void onResponse(@NotNull Call call, @NotNull Response response) {
        if (disposed) return;

        if (httpCallRef.compareAndSet(httpCall, null)) {
          callBack.onResponse(new ApolloInterceptor.InterceptorResponse(response));
          callBack.onCompleted();
        }
      }
    });
  }

  Call httpGetCall(Operation operation, CacheHeaders cacheHeaders, RequestHeaders requestHeaders,
      boolean writeQueryDocument, boolean autoPersistQueries) throws IOException {
    Request.Builder requestBuilder = new Request.Builder()
        .url(httpGetUrl(serverUrl, operation, scalarTypeAdapters, writeQueryDocument, autoPersistQueries))
        .get();
    decorateRequest(requestBuilder, operation, cacheHeaders, requestHeaders);
    return httpCallFactory.newCall(requestBuilder.build());
  }

  Call httpPostCall(Operation operation, CacheHeaders cacheHeaders, RequestHeaders requestHeaders,
      boolean writeQueryDocument, boolean autoPersistQueries) throws IOException {
    RequestBody requestBody = RequestBody.create(MEDIA_TYPE, httpPostRequestBody(operation, scalarTypeAdapters,
        writeQueryDocument, autoPersistQueries));

    requestBody = transformToMultiPartIfUploadExists(requestBody, operation);

    Request.Builder requestBuilder = new Request.Builder()
        .url(serverUrl)
        .header(HEADER_CONTENT_TYPE, CONTENT_TYPE)
        .post(requestBody);
    decorateRequest(requestBuilder, operation, cacheHeaders, requestHeaders);
    return httpCallFactory.newCall(requestBuilder.build());
  }

  void decorateRequest(Request.Builder requestBuilder, Operation operation, CacheHeaders cacheHeaders,
      RequestHeaders requestHeaders) throws IOException {
    requestBuilder
        .header(HEADER_ACCEPT_TYPE, ACCEPT_TYPE)
        .header(HEADER_APOLLO_OPERATION_ID, operation.operationId())
        .header(HEADER_APOLLO_OPERATION_NAME, operation.name().name())
        .tag(operation.operationId());

    for (String header : requestHeaders.headers()) {
      String value = requestHeaders.headerValue(header);
      requestBuilder.header(header, value);
    }

    if (cachePolicy.isPresent()) {
      HttpCachePolicy.Policy cachePolicy = this.cachePolicy.get();
      boolean skipCacheHttpResponse = "true".equalsIgnoreCase(cacheHeaders.headerValue(
          ApolloCacheHeaders.DO_NOT_STORE));

      String cacheKey = cacheKey(operation, scalarTypeAdapters);
      requestBuilder
          .header(HttpCache.CACHE_KEY_HEADER, cacheKey)
          .header(HttpCache.CACHE_FETCH_STRATEGY_HEADER, cachePolicy.fetchStrategy.name())
          .header(HttpCache.CACHE_EXPIRE_TIMEOUT_HEADER, String.valueOf(cachePolicy.expireTimeoutMs()))
          .header(HttpCache.CACHE_EXPIRE_AFTER_READ_HEADER, Boolean.toString(cachePolicy.expireAfterRead))
          .header(HttpCache.CACHE_PREFETCH_HEADER, Boolean.toString(prefetch))
          .header(HttpCache.CACHE_DO_NOT_STORE, Boolean.toString(skipCacheHttpResponse));
    }
  }

  static String cacheKey(Operation operation, ScalarTypeAdapters scalarTypeAdapters) throws IOException {
    return httpPostRequestBody(operation, scalarTypeAdapters, true, true).md5().hex();
  }

  static ByteString httpPostRequestBody(Operation operation, ScalarTypeAdapters scalarTypeAdapters,
      boolean writeQueryDocument, boolean autoPersistQueries) throws IOException {
    return operation.composeRequestBody(autoPersistQueries, writeQueryDocument, scalarTypeAdapters);
  }

  static HttpUrl httpGetUrl(HttpUrl serverUrl, Operation operation,
      ScalarTypeAdapters scalarTypeAdapters, boolean writeQueryDocument,
      boolean autoPersistQueries) throws IOException {
    HttpUrl.Builder urlBuilder = serverUrl.newBuilder();
    if (!autoPersistQueries || writeQueryDocument) {
      urlBuilder.addQueryParameter("query", operation.queryDocument());
    }
    if (operation.variables() != Operation.EMPTY_VARIABLES) {
      addVariablesUrlQueryParameter(urlBuilder, operation, scalarTypeAdapters);
    }
    urlBuilder.addQueryParameter("operationName", operation.name().name());
    if (autoPersistQueries) {
      addExtensionsUrlQueryParameter(urlBuilder, operation);
    }
    return urlBuilder.build();
  }

  static void addVariablesUrlQueryParameter(HttpUrl.Builder urlBuilder, Operation operation,
      ScalarTypeAdapters scalarTypeAdapters) throws IOException {
    Buffer buffer = new Buffer();
    JsonWriter jsonWriter = JsonWriter.of(buffer);
    jsonWriter.setSerializeNulls(true);
    jsonWriter.beginObject();
    operation.variables().marshaller().marshal(new InputFieldJsonWriter(jsonWriter, scalarTypeAdapters));
    jsonWriter.endObject();
    jsonWriter.close();
    urlBuilder.addQueryParameter("variables", buffer.readUtf8());
  }

  static void addExtensionsUrlQueryParameter(HttpUrl.Builder urlBuilder, Operation operation) throws IOException {
    Buffer buffer = new Buffer();
    JsonWriter jsonWriter = JsonWriter.of(buffer);
    jsonWriter.setSerializeNulls(true);
    jsonWriter.beginObject();
    jsonWriter.name("persistedQuery")
        .beginObject()
        .name("version").value(1)
        .name("sha256Hash").value(operation.operationId())
        .endObject();
    jsonWriter.endObject();
    jsonWriter.close();
    urlBuilder.addQueryParameter("extensions", buffer.readUtf8());
  }

  private static void recursiveGetUploadData(Object value, String variableName, ArrayList<FileUploadMeta> allUploads) {
    if (value instanceof InputType) {
      try {
        Field[] fields = value.getClass().getDeclaredFields();
        for (Field field : fields) {
          field.setAccessible(true);
          Object subValue = field.get(value);
          String key = field.getName();
          recursiveGetUploadData(subValue, variableName + "." + key, allUploads);
        }
      } catch (IllegalAccessException e) {
        // never happen
      }
    } else if (value instanceof Input) {
      Object unwrappedValue = ((Input) value).value;
      recursiveGetUploadData(unwrappedValue, variableName, allUploads);
    } else if (value instanceof FileUpload) {
      FileUpload upload = (FileUpload) value;
      String key = variableName;
      allUploads.add(new FileUploadMeta(key, upload.getMimetype(), new File(upload.getFilePath())));
      System.out.println(key);
    } else if (value instanceof FileUpload[]) {
      int varFileIndex = 0;
      FileUpload[] uploads = (FileUpload[]) value;
      for (FileUpload upload : uploads) {
        String key = variableName + "." + varFileIndex;
        allUploads.add(new FileUploadMeta(key, upload.getMimetype(), new File(upload.getFilePath())));
        System.out.println(key);
        varFileIndex++;
      }
    } else if (value instanceof Collection<?>) {
      Object[] listData = ((Collection) value).toArray();
      for (int i = 0; i < listData.length; i++) {
        Object subValue = listData[i];
        recursiveGetUploadData(subValue, variableName + "." + i, allUploads);
      }
    }
  }

  static RequestBody transformToMultiPartIfUploadExists(RequestBody originalBody, Operation operation)
      throws IOException {
    ArrayList<FileUploadMeta> allUploads = new ArrayList<>();
    for (String variableName : operation.variables().valueMap().keySet()) {
      Object value = operation.variables().valueMap().get(variableName);
      recursiveGetUploadData(value, "variables." + variableName, allUploads);
    }
    if (allUploads.isEmpty()) {
      return originalBody;
    } else {
      return httpMultipartRequestBody(originalBody, allUploads);
    }
  }

  static RequestBody httpMultipartRequestBody(RequestBody operations, ArrayList<FileUploadMeta> fileUploads)
      throws IOException {
    Buffer buffer = new Buffer();
    JsonWriter jsonWriter = JsonWriter.of(buffer);
    jsonWriter.beginObject();
    for (int i = 0; i < fileUploads.size(); i++) {
      jsonWriter.name(String.valueOf(i)).beginArray();
      jsonWriter.value(fileUploads.get(i).key);
      jsonWriter.endArray();
    }
    jsonWriter.endObject();
    jsonWriter.close();
    MultipartBody.Builder multipartBodyBuilder = new MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart("operations", null, operations)
        .addFormDataPart("map", null, RequestBody.create(MEDIA_TYPE, buffer.readByteString()));
    for (int i = 0; i < fileUploads.size(); i++) {
      FileUploadMeta fileMeta = fileUploads.get(i);
      multipartBodyBuilder.addFormDataPart(String.valueOf(i), fileMeta.file.getName(),
          RequestBody.create(MediaType.parse(fileMeta.mimetype), fileMeta.file));
    }
    return multipartBodyBuilder.build();
  }

  private static final class FileUploadMeta {
    public final String key;
    public final String mimetype;
    public final File file;

    FileUploadMeta(String key, String mimetype, File file) {
      this.key = key;
      this.mimetype = mimetype;
      this.file = file;
    }
  }
}
