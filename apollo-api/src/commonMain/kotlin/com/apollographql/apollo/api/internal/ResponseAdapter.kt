package com.apollographql.apollo.api.internal

interface ResponseAdapter<T> {

  fun fromResponse(reader: ResponseReader, __typename: String? = null): T
}
