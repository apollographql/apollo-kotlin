package com.example

import com.apollographql.apollo.api.Upload
import java.io.File
import com.apollographql.apollo.api.toUpload

suspend fun main() {
  val fileUpload: Upload = File("/etc/passwd").toUpload("text/plain")
}
