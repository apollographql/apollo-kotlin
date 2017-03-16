package com.apollographql.android.cache.normalized;

import java.util.Collection;

import javax.annotation.Nonnull;

public interface WriteTransaction {

  void writeAndFinish(@Nonnull Collection<Record> recordSet);

}
