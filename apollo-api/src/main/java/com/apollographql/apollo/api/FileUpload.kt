package com.apollographql.apollo.api

import java.io.File

data class FileUpload(@JvmField val mimetype: String, @JvmField val file: File)
