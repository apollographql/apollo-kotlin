package com.apollographql.apollo.api

expect class FileUpload constructor(mimetype: String, file: File) {
  val mimetype: String
}
