package com.apollographql.apollo.api

class FileUpload(val mimetype: String, val filePath: String) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is FileUpload) return false

    if (mimetype != other.mimetype) return false
    if (filePath != other.filePath) return false

    return true
  }

  override fun hashCode(): Int {
    var result = mimetype.hashCode()
    result = 31 * result + filePath.hashCode()
    return result
  }
}
