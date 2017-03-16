package com.apollographql.android.cache.normalized;

import java.util.Collection;
import java.util.Set;

public interface ReadWriteTransaction extends ReadTransaction {

  Set<String> merge(Collection<Record> recordCollection);

}
