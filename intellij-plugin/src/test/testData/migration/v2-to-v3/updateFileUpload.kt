package com.example

import com.apollographql.apollo.api.FileUpload

suspend fun main() {
  val fileUpload: FileUpload = FileUpload("text/plain", "/etc/passwd")
}
