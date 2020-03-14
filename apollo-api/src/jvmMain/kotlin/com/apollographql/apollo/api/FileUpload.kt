package com.apollographql.apollo.api

actual class FileUpload actual constructor(actual val mimetype: String, val file: File) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is FileUpload) return false

    if (mimetype != other.mimetype) return false
    if (file != other.file) return false

    return true
  }

  override fun hashCode(): Int {
    var result = mimetype.hashCode()
    result = 31 * result + file.hashCode()
    return result
  }
}
