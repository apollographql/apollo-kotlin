/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.apollographql.apollo.cache.http;

import com.apollographql.apollo.cache.http.internal.StatusLine;
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
import okhttp3.TlsVersion;
import okhttp3.internal.http.HttpMethod;
import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.ByteString;
import okio.Okio;
import okio.Sink;
import okio.Source;

import static com.apollographql.apollo.cache.http.Utils.varyHeaders;

/**
 * Class was copied and modified from {@link okhttp3.Cache.Entry}
 */
final class ResponseHeaderRecord {
  /** Synthetic response header: the local time when the request was sent. */
  private static final String SENT_MILLIS = "OkHttp-Sent-Millis";

  /** Synthetic response header: the local time when the response was received. */
  private static final String RECEIVED_MILLIS = "OkHttp-Received-Millis";

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
  ResponseHeaderRecord(Source in) throws IOException {
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

  ResponseHeaderRecord(Response response) {
    this.url = response.request().url().toString();
    this.varyHeaders = varyHeaders(response);
    this.requestMethod = response.request().method();
    this.protocol = response.protocol();
    this.code = response.code();
    this.message = response.message();
    this.responseHeaders = response.headers();
    this.handshake = response.handshake();
    this.sentRequestMillis = response.sentRequestAtMillis();
    this.receivedResponseMillis = response.receivedResponseAtMillis();
  }

  void writeTo(Sink sink) throws IOException {
    BufferedSink bufferedSink = Okio.buffer(sink);

    bufferedSink.writeUtf8(url)
        .writeByte('\n');
    bufferedSink.writeUtf8(requestMethod)
        .writeByte('\n');
    bufferedSink.writeDecimalLong(varyHeaders.size())
        .writeByte('\n');
    for (int i = 0, size = varyHeaders.size(); i < size; i++) {
      bufferedSink.writeUtf8(varyHeaders.name(i))
          .writeUtf8(": ")
          .writeUtf8(varyHeaders.value(i))
          .writeByte('\n');
    }

    bufferedSink.writeUtf8(new StatusLine(protocol, code, message).toString())
        .writeByte('\n');
    bufferedSink.writeDecimalLong(responseHeaders.size() + 2)
        .writeByte('\n');
    for (int i = 0, size = responseHeaders.size(); i < size; i++) {
      bufferedSink.writeUtf8(responseHeaders.name(i))
          .writeUtf8(": ")
          .writeUtf8(responseHeaders.value(i))
          .writeByte('\n');
    }
    bufferedSink.writeUtf8(SENT_MILLIS)
        .writeUtf8(": ")
        .writeDecimalLong(sentRequestMillis)
        .writeByte('\n');
    bufferedSink.writeUtf8(RECEIVED_MILLIS)
        .writeUtf8(": ")
        .writeDecimalLong(receivedResponseMillis)
        .writeByte('\n');

    if (isHttps()) {
      bufferedSink.writeByte('\n');
      bufferedSink.writeUtf8(handshake.cipherSuite().javaName())
          .writeByte('\n');
      writeCertList(bufferedSink, handshake.peerCertificates());
      writeCertList(bufferedSink, handshake.localCertificates());
      // The handshake's TLS version is null on HttpsURLConnection and on older cached responses.
      if (handshake.tlsVersion() != null) {
        bufferedSink.writeUtf8(handshake.tlsVersion().javaName())
            .writeByte('\n');
      }
    }
    bufferedSink.close();
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
    RequestBody body = HttpMethod.permitsRequestBody(requestMethod) ? RequestBody.create(MediaType.parse("application/json; charset=utf-8"), "") : null;
    Request cacheRequest = new Request.Builder()
        .url(url)
        .method(requestMethod, body)
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
