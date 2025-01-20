package com.apollographql.apollo.execution

internal typealias Weigher<Key, Value> = (Key, Value?) -> Int

