package com.example

import com.apollographql.apollo3.api.Upload
import java.io.File
import com.apollographql.apollo3.api.toUpload

suspend fun main() {
  val fileUpload: Upload = File("/etc/passwd").toUpload("text/plain")
}
