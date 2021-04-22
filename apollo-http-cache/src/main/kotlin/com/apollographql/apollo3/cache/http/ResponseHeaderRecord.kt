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
package com.apollographql.apollo3.cache.http

import com.apollographql.apollo3.cache.http.internal.StatusLine
import okhttp3.CipherSuite
import okhttp3.Handshake
import okhttp3.Headers
import okhttp3.MediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.TlsVersion
import okhttp3.internal.http.HttpMethod
import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource
import okio.ByteString.Companion.decodeBase64
import okio.ByteString.Companion.toByteString
import okio.Sink
import okio.Source
import okio.buffer
import java.io.IOException
import java.security.cert.Certificate
import java.security.cert.CertificateEncodingException
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.util.ArrayList

/**
 * Class was copied and modified from [okhttp3.Cache.Entry]
 */
internal class ResponseHeaderRecord {
  private val url: String
  private val varyHeaders: Headers?
  private val requestMethod: String
  private val protocol: Protocol?
  private val code: Int
  private val message: String?
  private val responseHeaders: Headers
  private val handshake: Handshake?
  private val sentRequestMillis: Long
  private val receivedResponseMillis: Long

  /**
   * Reads an entry from an input stream. A typical entry looks like this:
   * <pre>`http://google.com/foo
   * GET
   * 2
   * Accept-Language: fr-CA
   * Accept-Charset: UTF-8
   * HTTP/1.1 200 OK
   * 3
   * Content-Type: image/png
   * Content-Length: 100
   * Cache-Control: max-age=600
  `</pre> *
   *
   *
   * A typical HTTPS file looks like this:
   * <pre>`https://google.com/foo
   * GET
   * 2
   * Accept-Language: fr-CA
   * Accept-Charset: UTF-8
   * HTTP/1.1 200 OK
   * 3
   * Content-Type: image/png
   * Content-Length: 100
   * Cache-Control: max-age=600
   *
   * AES_256_WITH_MD5
   * 2
   * base64-encoded peerCertificate[0]
   * base64-encoded peerCertificate[1]
   * -1
   * TLSv1.2
  `</pre> *
   * The file is newline separated. The first two lines are the URL and the request method. Next
   * is the number of HTTP Vary request header lines, followed by those lines.
   *
   *
   * Next is the response status line, followed by the number of HTTP response header lines,
   * followed by those lines.
   *
   *
   * HTTPS responses also contain SSL session information. This begins with a blank line, and
   * then a line containing the cipher suite. Next is the length of the peer certificate chain.
   * These certificates are base64-encoded and appear each on their own line. The next line
   * contains the length of the local certificate chain. These certificates are also
   * base64-encoded and appear each on their own line. A length of -1 is used to encode a null
   * array. The last line is optional. If present, it contains the TLS version.
   */
  constructor(source: Source) {
    source.use {
      val buffer = it.buffer()
      url = buffer.readUtf8LineStrict()
      requestMethod = buffer.readUtf8LineStrict()
      val varyHeadersBuilder = Headers.Builder()
      val varyRequestHeaderLineCount = readInt(buffer)
      for (i in 0 until varyRequestHeaderLineCount) {
        addHeaderLenient(varyHeadersBuilder, buffer.readUtf8LineStrict())
      }
      varyHeaders = varyHeadersBuilder.build()
      val statusLine: StatusLine = StatusLine.parse(buffer.readUtf8LineStrict())
      protocol = statusLine.protocol
      code = statusLine.code
      message = statusLine.message
      val responseHeadersBuilder = Headers.Builder()
      val responseHeaderLineCount = readInt(buffer)
      for (i in 0 until responseHeaderLineCount) {
        addHeaderLenient(responseHeadersBuilder, buffer.readUtf8LineStrict())
      }
      val sendRequestMillisString = responseHeadersBuilder[SENT_MILLIS]
      val receivedResponseMillisString = responseHeadersBuilder[RECEIVED_MILLIS]
      responseHeadersBuilder.removeAll(SENT_MILLIS)
      responseHeadersBuilder.removeAll(RECEIVED_MILLIS)
      sentRequestMillis = sendRequestMillisString.toLong()
      receivedResponseMillis = receivedResponseMillisString.toLong()
      responseHeaders = responseHeadersBuilder.build()
      handshake = if (isHttps) {
        val blank = buffer.readUtf8LineStrict()
        if (blank.isNotEmpty()) {
          throw IOException("expected \"\" but was \"$blank\"")
        }
        val cipherSuiteString = buffer.readUtf8LineStrict()
        val cipherSuite = CipherSuite.forJavaName(cipherSuiteString)
        val peerCertificates = readCertificateList(buffer)
        val localCertificates = readCertificateList(buffer)
        val tlsVersion = if (!buffer.exhausted()) TlsVersion.forJavaName(buffer.readUtf8LineStrict()) else null
        Handshake.get(tlsVersion, cipherSuite, peerCertificates, localCertificates)
      } else {
        null
      }
    }
  }

  constructor(response: Response) {
    url = response.request().url().toString()
    varyHeaders = Utils.varyHeaders(response)
    requestMethod = response.request().method()
    protocol = response.protocol()
    this.code = response.code()
    message = response.message()
    responseHeaders = response.headers()
    handshake = response.handshake()
    sentRequestMillis = response.sentRequestAtMillis()
    receivedResponseMillis = response.receivedResponseAtMillis()
  }

  @Throws(IOException::class)
  fun writeTo(sink: Sink) {
    val bufferedSink = sink.buffer()
    bufferedSink.writeUtf8(url)
        .writeByte('\n'.toInt())
    bufferedSink.writeUtf8(requestMethod)
        .writeByte('\n'.toInt())
    bufferedSink.writeDecimalLong(varyHeaders!!.size().toLong())
        .writeByte('\n'.toInt())
    run {
      var i = 0
      val size = varyHeaders!!.size()
      while (i < size) {
        bufferedSink.writeUtf8(varyHeaders.name(i))
            .writeUtf8(": ")
            .writeUtf8(varyHeaders.value(i))
            .writeByte('\n'.toInt())
        i++
      }
    }
    bufferedSink.writeUtf8(StatusLine(protocol, code, message).toString())
        .writeByte('\n'.toInt())
    bufferedSink.writeDecimalLong((responseHeaders.size() + 2).toLong())
        .writeByte('\n'.toInt())
    var i = 0
    val size = responseHeaders.size()
    while (i < size) {
      bufferedSink.writeUtf8(responseHeaders.name(i))
          .writeUtf8(": ")
          .writeUtf8(responseHeaders.value(i))
          .writeByte('\n'.toInt())
      i++
    }
    bufferedSink.writeUtf8(SENT_MILLIS)
        .writeUtf8(": ")
        .writeDecimalLong(sentRequestMillis)
        .writeByte('\n'.toInt())
    bufferedSink.writeUtf8(RECEIVED_MILLIS)
        .writeUtf8(": ")
        .writeDecimalLong(receivedResponseMillis)
        .writeByte('\n'.toInt())
    if (isHttps) {
      bufferedSink.writeByte('\n'.toInt())
      bufferedSink.writeUtf8(handshake!!.cipherSuite().javaName())
          .writeByte('\n'.toInt())
      writeCertList(bufferedSink, handshake.peerCertificates())
      writeCertList(bufferedSink, handshake.localCertificates())
      // The handshake's TLS version is null on HttpsURLConnection and on older cached responses.
      if (handshake.tlsVersion() != null) {
        bufferedSink.writeUtf8(handshake.tlsVersion().javaName())
            .writeByte('\n'.toInt())
      }
    }
    bufferedSink.close()
  }

  private val isHttps: Boolean
    get() = url.startsWith("https://")

  @Throws(IOException::class)
  private fun readCertificateList(source: BufferedSource): List<Certificate> {
    val length = readInt(source)
    return if (length == -1) emptyList() else try {
      val certificateFactory = CertificateFactory.getInstance("X.509")
      val result: MutableList<Certificate> = ArrayList(length)
      for (i in 0 until length) {
        val line = source.readUtf8LineStrict()
        val bytes = Buffer()
        line.decodeBase64()?.let { bytes.write(it) }
        result.add(certificateFactory.generateCertificate(bytes.inputStream()))
      }
      result
    } catch (e: CertificateException) {
      throw IOException(e.message)
    } // OkHttp v1.2 used -1 to indicate null.
  }

  @Throws(IOException::class)
  private fun writeCertList(sink: BufferedSink, certificates: List<Certificate>) {
    try {
      sink.writeDecimalLong(certificates.size.toLong())
          .writeByte('\n'.toInt())
      var i = 0
      val size = certificates.size
      while (i < size) {
        val bytes = certificates[i].encoded
        val line: String = bytes.toByteString().base64()
        sink.writeUtf8(line)
            .writeByte('\n'.toInt())
        i++
      }
    } catch (e: CertificateEncodingException) {
      throw IOException(e.message)
    }
  }

  fun response(): Response {
    var body: RequestBody? = null
    if (HttpMethod.permitsRequestBody(requestMethod)) {
      body = RequestBody.create(MediaType.parse("application/json"), "")
    }
    val cacheRequest = Request.Builder()
        .url(url)
        .method(requestMethod, body)
        .headers(varyHeaders)
        .build()
    return Response.Builder()
        .request(cacheRequest)
        .protocol(protocol)
        .code(code)
        .message(message)
        .headers(responseHeaders)
        .handshake(handshake)
        .sentRequestAtMillis(sentRequestMillis)
        .receivedResponseAtMillis(receivedResponseMillis)
        .build()
  }

  private fun addHeaderLenient(headersBuilder: Headers.Builder, line: String) {
    val index = line.indexOf(":", 1)
    if (index != -1) {
      headersBuilder.add(line.substring(0, index), line.substring(index + 1))
    } else if (line.startsWith(":")) {
      // Work around empty header names and header names that start with a
      // colon (created by old broken SPDY versions of the response cache).
      headersBuilder.add("", line.substring(1)) // Empty header name.
    } else {
      headersBuilder.add("", line) // No header name.
    }
  }

  companion object {
    /** Synthetic response header: the local time when the request was sent.  */
    private const val SENT_MILLIS = "OkHttp-Sent-Millis"

    /** Synthetic response header: the local time when the response was received.  */
    private const val RECEIVED_MILLIS = "OkHttp-Received-Millis"
    @Throws(IOException::class)
    private fun readInt(source: BufferedSource): Int {
      return try {
        val result = source.readDecimalLong()
        val line = source.readUtf8LineStrict()
        if (result < 0 || result > Int.MAX_VALUE || line.isNotEmpty()) {
          throw IOException("expected an int but was \"$result$line\"")
        }
        result.toInt()
      } catch (e: NumberFormatException) {
        throw IOException(e.message)
      }
    }
  }
}