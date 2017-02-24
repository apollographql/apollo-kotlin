package com.apollographql.android;

import java.io.File;
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.CipherSuite;
import okhttp3.Handshake;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.TlsVersion;
import okhttp3.internal.cache.DiskLruCache;
import okhttp3.internal.http.HttpCodec;
import okhttp3.internal.http.HttpHeaders;
import okhttp3.internal.http.RealResponseBody;
import okhttp3.internal.http.StatusLine;
import okhttp3.internal.io.FileSystem;
import okhttp3.internal.platform.Platform;
import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.ByteString;
import okio.ForwardingSource;
import okio.Okio;
import okio.Source;
import okio.Timeout;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static okhttp3.internal.Util.discard;

public final class HttpCache {
  private static final int VERSION = 99991;
  private static final int ENTRY_METADATA = 0;
  private static final int ENTRY_BODY = 1;
  private static final int ENTRY_COUNT = 2;
  private final DiskLruCache cache;

  public HttpCache(File directory, long maxSize, FileSystem fileSystem) {
    this.cache = DiskLruCache.create(fileSystem, directory, VERSION, ENTRY_COUNT, maxSize);
  }

  public void delete() throws IOException {
    cache.delete();
  }

  public Response read(Request request) throws IOException {
    String cacheKey = cacheKey(request);
    DiskLruCache.Snapshot snapshot = cache.get(cacheKey);
    if (snapshot == null) {
      return null;
    }

    Response response = new HeadersEntry(snapshot.getSource(ENTRY_METADATA)).response();
    String contentType = response.header("Content-Type");
    String contentLength = response.header("Content-Length");
    return response.newBuilder()
        .body(new CacheResponseBody(snapshot, contentType, contentLength))
        .build();
  }

  Response cacheProxy(Response response) throws IOException {
    String cacheKey = cacheKey(response.request());
    final DiskLruCache.Editor editor = cache.edit(cacheKey);
    if (editor == null) {
      throw new IOException("failed to init disk cache editor");
    }

    HeadersEntry entry = new HeadersEntry(response);
    entry.writeTo(editor);

    return response.newBuilder()
        .body(new RealResponseBody(response.headers(), Okio.buffer(cacheSourceProxy(editor, response.body().source()))))
        .build();
  }

  private Source cacheSourceProxy(final DiskLruCache.Editor editor, final Source source) {
    final BufferedSink cacheBodySink = Okio.buffer(editor.newSink(ENTRY_BODY));
    return new Source() {
      boolean closed;

      @Override public long read(Buffer sink, long byteCount) throws IOException {
        long bytesRead;
        try {
          bytesRead = source.read(sink, byteCount);
        } catch (IOException e) {
          if (!closed) {
            // Failed to write a complete cache response.
            closed = true;
            cacheBodySink.close();
            editor.abort();
          }
          throw e;
        }

        if (bytesRead == -1) {
          if (!closed) {
            // The cache response is complete!
            closed = true;
            cacheBodySink.close();
            editor.commit();
          }
          return -1;
        }

        sink.copyTo(cacheBodySink.buffer(), sink.size() - bytesRead, bytesRead);
        cacheBodySink.emitCompleteSegments();
        return bytesRead;
      }

      @Override public Timeout timeout() {
        return source.timeout();
      }

      @Override public void close() throws IOException {
        if (closed) {
          return;
        }
        closed = true;

        if (discard(this, HttpCodec.DISCARD_STREAM_TIMEOUT_MILLIS, MILLISECONDS)) {
          source.close();
          cacheBodySink.close();
          editor.commit();
        } else {
          source.close();
          cacheBodySink.close();
          editor.abort();
        }
      }
    };
  }

  private static String cacheKey(Request request) throws IOException {
    Buffer hashBuffer = new Buffer();
    request.body().writeTo(hashBuffer);
    return hashBuffer.readByteString().md5().hex();
  }

  static class CacheResponseBody extends ResponseBody {
    final DiskLruCache.Snapshot snapshot;
    private final BufferedSource bodySource;
    private final String contentType;
    private final String contentLength;

    CacheResponseBody(final DiskLruCache.Snapshot snapshot, String contentType, String contentLength) {
      this.snapshot = snapshot;
      this.contentType = contentType;
      this.contentLength = contentLength;

      Source source = snapshot.getSource(ENTRY_BODY);
      bodySource = Okio.buffer(new ForwardingSource(source) {
        @Override public void close() throws IOException {
          snapshot.close();
          super.close();
        }
      });
    }

    @Override public MediaType contentType() {
      return contentType != null ? MediaType.parse(contentType) : null;
    }

    @Override public long contentLength() {
      try {
        return contentLength != null ? Long.parseLong(contentLength) : -1;
      } catch (NumberFormatException e) {
        return -1;
      }
    }

    @Override public BufferedSource source() {
      return bodySource;
    }
  }

  /**
   * Class was copied from okhttp3.Cache.Entry
   */
  private static final class HeadersEntry {
    /** Synthetic response header: the local time when the request was sent. */
    private static final String SENT_MILLIS = Platform.get().getPrefix() + "-Sent-Millis";

    /** Synthetic response header: the local time when the response was received. */
    private static final String RECEIVED_MILLIS = Platform.get().getPrefix() + "-Received-Millis";

    private final String url;
    private final Headers varyHeaders;
    private final String requestMethod;
    private final Protocol protocol;
    private final int code;
    private final String message;
    private final Headers responseHeaders;
    private final Handshake handshake;
    private final long sentRequestMillis;
    private final long receivedResponseMillis;

    /**
     * Reads an entry from an input stream. A typical entry looks like this:
     * <pre>{@code
     *   http://google.com/foo
     *   GET
     *   2
     *   Accept-Language: fr-CA
     *   Accept-Charset: UTF-8
     *   HTTP/1.1 200 OK
     *   3
     *   Content-Type: image/png
     *   Content-Length: 100
     *   Cache-Control: max-age=600
     * }</pre>
     *
     * <p>A typical HTTPS file looks like this:
     * <pre>{@code
     *   https://google.com/foo
     *   GET
     *   2
     *   Accept-Language: fr-CA
     *   Accept-Charset: UTF-8
     *   HTTP/1.1 200 OK
     *   3
     *   Content-Type: image/png
     *   Content-Length: 100
     *   Cache-Control: max-age=600
     *
     *   AES_256_WITH_MD5
     *   2
     *   base64-encoded peerCertificate[0]
     *   base64-encoded peerCertificate[1]
     *   -1
     *   TLSv1.2
     * }</pre>
     * The file is newline separated. The first two lines are the URL and the request method. Next
     * is the number of HTTP Vary request header lines, followed by those lines.
     *
     * <p>Next is the response status line, followed by the number of HTTP response header lines,
     * followed by those lines.
     *
     * <p>HTTPS responses also contain SSL session information. This begins with a blank line, and
     * then a line containing the cipher suite. Next is the length of the peer certificate chain.
     * These certificates are base64-encoded and appear each on their own line. The next line
     * contains the length of the local certificate chain. These certificates are also
     * base64-encoded and appear each on their own line. A length of -1 is used to encode a null
     * array. The last line is optional. If present, it contains the TLS version.
     */
    HeadersEntry(Source in) throws IOException {
      try {
        BufferedSource source = Okio.buffer(in);
        url = source.readUtf8LineStrict();
        requestMethod = source.readUtf8LineStrict();
        Headers.Builder varyHeadersBuilder = new Headers.Builder();
        int varyRequestHeaderLineCount = readInt(source);
        for (int i = 0; i < varyRequestHeaderLineCount; i++) {
          addHeaderLenient(varyHeadersBuilder, source.readUtf8LineStrict());
        }
        varyHeaders = varyHeadersBuilder.build();

        StatusLine statusLine = StatusLine.parse(source.readUtf8LineStrict());
        protocol = statusLine.protocol;
        code = statusLine.code;
        message = statusLine.message;
        Headers.Builder responseHeadersBuilder = new Headers.Builder();
        int responseHeaderLineCount = readInt(source);
        for (int i = 0; i < responseHeaderLineCount; i++) {
          addHeaderLenient(responseHeadersBuilder, source.readUtf8LineStrict());
        }
        String sendRequestMillisString = responseHeadersBuilder.get(SENT_MILLIS);
        String receivedResponseMillisString = responseHeadersBuilder.get(RECEIVED_MILLIS);
        responseHeadersBuilder.removeAll(SENT_MILLIS);
        responseHeadersBuilder.removeAll(RECEIVED_MILLIS);
        sentRequestMillis = sendRequestMillisString != null
            ? Long.parseLong(sendRequestMillisString)
            : 0L;
        receivedResponseMillis = receivedResponseMillisString != null
            ? Long.parseLong(receivedResponseMillisString)
            : 0L;
        responseHeaders = responseHeadersBuilder.build();

        if (isHttps()) {
          String blank = source.readUtf8LineStrict();
          if (blank.length() > 0) {
            throw new IOException("expected \"\" but was \"" + blank + "\"");
          }
          String cipherSuiteString = source.readUtf8LineStrict();
          CipherSuite cipherSuite = CipherSuite.forJavaName(cipherSuiteString);
          List<Certificate> peerCertificates = readCertificateList(source);
          List<Certificate> localCertificates = readCertificateList(source);
          TlsVersion tlsVersion = !source.exhausted()
              ? TlsVersion.forJavaName(source.readUtf8LineStrict())
              : null;
          handshake = Handshake.get(tlsVersion, cipherSuite, peerCertificates, localCertificates);
        } else {
          handshake = null;
        }
      } finally {
        in.close();
      }
    }

    HeadersEntry(Response response) {
      this.url = response.request().url().toString();
      this.varyHeaders = HttpHeaders.varyHeaders(response);
      this.requestMethod = response.request().method();
      this.protocol = response.protocol();
      this.code = response.code();
      this.message = response.message();
      this.responseHeaders = response.headers();
      this.handshake = response.handshake();
      this.sentRequestMillis = response.sentRequestAtMillis();
      this.receivedResponseMillis = response.receivedResponseAtMillis();
    }

    void writeTo(DiskLruCache.Editor editor) throws IOException {
      BufferedSink sink = Okio.buffer(editor.newSink(ENTRY_METADATA));

      sink.writeUtf8(url)
          .writeByte('\n');
      sink.writeUtf8(requestMethod)
          .writeByte('\n');
      sink.writeDecimalLong(varyHeaders.size())
          .writeByte('\n');
      for (int i = 0, size = varyHeaders.size(); i < size; i++) {
        sink.writeUtf8(varyHeaders.name(i))
            .writeUtf8(": ")
            .writeUtf8(varyHeaders.value(i))
            .writeByte('\n');
      }

      sink.writeUtf8(new StatusLine(protocol, code, message).toString())
          .writeByte('\n');
      sink.writeDecimalLong(responseHeaders.size() + 2)
          .writeByte('\n');
      for (int i = 0, size = responseHeaders.size(); i < size; i++) {
        sink.writeUtf8(responseHeaders.name(i))
            .writeUtf8(": ")
            .writeUtf8(responseHeaders.value(i))
            .writeByte('\n');
      }
      sink.writeUtf8(SENT_MILLIS)
          .writeUtf8(": ")
          .writeDecimalLong(sentRequestMillis)
          .writeByte('\n');
      sink.writeUtf8(RECEIVED_MILLIS)
          .writeUtf8(": ")
          .writeDecimalLong(receivedResponseMillis)
          .writeByte('\n');

      if (isHttps()) {
        sink.writeByte('\n');
        sink.writeUtf8(handshake.cipherSuite().javaName())
            .writeByte('\n');
        writeCertList(sink, handshake.peerCertificates());
        writeCertList(sink, handshake.localCertificates());
        // The handshake’s TLS version is null on HttpsURLConnection and on older cached responses.
        if (handshake.tlsVersion() != null) {
          sink.writeUtf8(handshake.tlsVersion().javaName())
              .writeByte('\n');
        }
      }
      sink.close();
    }

    private boolean isHttps() {
      return url.startsWith("https://");
    }

    private List<Certificate> readCertificateList(BufferedSource source) throws IOException {
      int length = readInt(source);
      if (length == -1) return Collections.emptyList(); // OkHttp v1.2 used -1 to indicate null.

      try {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        List<Certificate> result = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
          String line = source.readUtf8LineStrict();
          Buffer bytes = new Buffer();
          bytes.write(ByteString.decodeBase64(line));
          result.add(certificateFactory.generateCertificate(bytes.inputStream()));
        }
        return result;
      } catch (CertificateException e) {
        throw new IOException(e.getMessage());
      }
    }

    private void writeCertList(BufferedSink sink, List<Certificate> certificates)
        throws IOException {
      try {
        sink.writeDecimalLong(certificates.size())
            .writeByte('\n');
        for (int i = 0, size = certificates.size(); i < size; i++) {
          byte[] bytes = certificates.get(i).getEncoded();
          String line = ByteString.of(bytes).base64();
          sink.writeUtf8(line)
              .writeByte('\n');
        }
      } catch (CertificateEncodingException e) {
        throw new IOException(e.getMessage());
      }
    }

    Response response() {
      Request cacheRequest = new Request.Builder()
          .url(url)
          .method(requestMethod, RequestBody.create(MediaType.parse("application/json; charset=utf-8"), ""))
          .headers(varyHeaders)
          .build();
      return new Response.Builder()
          .request(cacheRequest)
          .protocol(protocol)
          .code(code)
          .message(message)
          .headers(responseHeaders)
          .handshake(handshake)
          .sentRequestAtMillis(sentRequestMillis)
          .receivedResponseAtMillis(receivedResponseMillis)
          .build();
    }

    private static int readInt(BufferedSource source) throws IOException {
      try {
        long result = source.readDecimalLong();
        String line = source.readUtf8LineStrict();
        if (result < 0 || result > Integer.MAX_VALUE || !line.isEmpty()) {
          throw new IOException("expected an int but was \"" + result + line + "\"");
        }
        return (int) result;
      } catch (NumberFormatException e) {
        throw new IOException(e.getMessage());
      }
    }

    private void addHeaderLenient(Headers.Builder headersBuilder, String line) {
      int index = line.indexOf(":", 1);
      if (index != -1) {
        headersBuilder.add(line.substring(0, index), line.substring(index + 1));
      } else if (line.startsWith(":")) {
        // Work around empty header names and header names that start with a
        // colon (created by old broken SPDY versions of the response cache).
        headersBuilder.add("", line.substring(1)); // Empty header name.
      } else {
        headersBuilder.add("", line); // No header name.
      }
    }
  }
}
