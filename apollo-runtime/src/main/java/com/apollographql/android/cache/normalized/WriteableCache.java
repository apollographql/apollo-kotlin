package com.apollographql.android.cache.normalized;

import java.util.Collection;
import java.util.Set;

public interface WriteableCache extends ReadableCache {

  Set<String> merge(Collection<Record> recordCollection);

}
