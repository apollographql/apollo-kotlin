@file:JvmName("ApolloClientUtils")

package com.apollographql.apollo3.java

fun com.apollographql.apollo3.ApolloClient.Builder.webSocketReopenWhen(webSocketReopenWhenListener: ApolloClient.WebSocketReopenWhenListener): com.apollographql.apollo3.ApolloClient.Builder {
  return apply { webSocketReopenWhen { throwable, attempt -> webSocketReopenWhenListener.shouldReopen(throwable, attempt) } }
}
