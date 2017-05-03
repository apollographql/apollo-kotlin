package com.apollographql.apollo.internal.cache.normalized;


import com.apollographql.apollo.CustomTypeAdapter;
import com.apollographql.apollo.api.Field;
import com.apollographql.apollo.api.Fragment;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ScalarType;
import com.apollographql.apollo.cache.normalized.ApolloStore;
import com.apollographql.apollo.cache.normalized.CacheKey;
import com.apollographql.apollo.cache.normalized.CacheKeyResolver;
import com.apollographql.apollo.cache.normalized.NormalizedCache;
import com.apollographql.apollo.cache.normalized.Record;
import com.apollographql.apollo.internal.field.CacheFieldValueResolver;
import com.apollographql.apollo.internal.reader.RealResponseReader;
import com.apollographql.apollo.internal.util.ApolloLogger;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

public final class RealApolloStore implements ApolloStore, ReadableCache, WriteableCache {
  private final NormalizedCache normalizedCache;
  private final CacheKeyResolver cacheKeyResolver;
  private final Map<ScalarType, CustomTypeAdapter> customTypeAdapters;
  private final ApolloLogger logger;
  private final ReadWriteLock lock;
  private final Set<RecordChangeSubscriber> subscribers;

  public RealApolloStore(@Nonnull NormalizedCache normalizedCache, @Nonnull CacheKeyResolver cacheKeyResolver,
      @Nonnull final Map<ScalarType, CustomTypeAdapter> customTypeAdapters, @Nonnull ApolloLogger logger) {
    this.normalizedCache = checkNotNull(normalizedCache, "cacheStore == null");
    this.cacheKeyResolver = checkNotNull(cacheKeyResolver, "cacheKeyResolver == null");
    this.customTypeAdapters = checkNotNull(customTypeAdapters, "customTypeAdapters == null");
    this.logger = checkNotNull(logger, "logger == null");
    this.lock = new ReentrantReadWriteLock();
    this.subscribers = Collections.newSetFromMap(new WeakHashMap<RecordChangeSubscriber, Boolean>());
  }

  @Override public ResponseNormalizer<Map<String, Object>> networkResponseNormalizer() {
    return new ResponseNormalizer<Map<String, Object>>() {
      @Nonnull @Override public CacheKey resolveCacheKey(@Nonnull Field field, @Nonnull Map<String, Object> record) {
        return cacheKeyResolver.fromFieldRecordSet(field, record);
      }
    };
  }

  @Override public ResponseNormalizer<Record> cacheResponseNormalizer() {
    return new ResponseNormalizer<Record>() {
      @Nonnull @Override public CacheKey resolveCacheKey(@Nonnull Field field, @Nonnull Record record) {
        return CacheKey.from(record.key());
      }
    };
  }

  @Override public synchronized void subscribe(RecordChangeSubscriber subscriber) {
    subscribers.add(subscriber);
  }

  @Override public synchronized void unsubscribe(RecordChangeSubscriber subscriber) {
    subscribers.remove(subscriber);
  }

  @Override public void publish(@Nonnull Set<String> changedKeys) {
    checkNotNull(changedKeys, "changedKeys == null");
    if (changedKeys.isEmpty()) {
      return;
    }
    Set<RecordChangeSubscriber> iterableSubscribers;
    synchronized (this) {
      iterableSubscribers = new LinkedHashSet<>(subscribers);
    }
    for (RecordChangeSubscriber subscriber : iterableSubscribers) {
      subscriber.onCacheRecordsChanged(changedKeys);
    }
  }

  @Override public void clearAll() {
    writeTransaction(new Transaction<WriteableCache, Boolean>() {
      @Override public Boolean execute(WriteableCache cache) {
        cache.clearAll();
        return true;
      }
    });
  }

  @Override public <R> R readTransaction(Transaction<ReadableCache, R> transaction) {
    lock.readLock().lock();
    try {
      return transaction.execute(RealApolloStore.this);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override public <R> R writeTransaction(Transaction<WriteableCache, R> transaction) {
    lock.writeLock().lock();
    try {
      return transaction.execute(RealApolloStore.this);
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override public NormalizedCache normalizedCache() {
    return normalizedCache;
  }

  @Nullable public Record read(@Nonnull String key) {
    return normalizedCache.loadRecord(checkNotNull(key, "key == null"));
  }

  @Nonnull public Collection<Record> read(@Nonnull Collection<String> keys) {
    return normalizedCache.loadRecords(checkNotNull(keys, "keys == null"));
  }

  @Nonnull public Set<String> merge(@Nonnull Collection<Record> recordSet) {
    return normalizedCache.merge(checkNotNull(recordSet, "recordSet == null"));
  }

  @Override public CacheKeyResolver cacheKeyResolver() {
    return cacheKeyResolver;
  }

  @Nullable @Override public <D extends Operation.Data, T, V extends Operation.Variables> T read(
      @Nonnull final Operation<D, T, V> operation) {
    checkNotNull(operation, "operation == null");
    return readTransaction(new Transaction<ReadableCache, T>() {
      @Nullable @Override public T execute(ReadableCache cache) {
        Record rootRecord = cache.read(CacheKeyResolver.rootKeyForOperation(operation).key());
        if (rootRecord == null) {
          return null;
        }

        ResponseFieldMapper<D> responseFieldMapper = operation.responseFieldMapper();
        CacheFieldValueResolver fieldValueResolver = new CacheFieldValueResolver(cache, operation.variables(),
            cacheKeyResolver());
        //noinspection unchecked
        RealResponseReader<Record> responseReader = new RealResponseReader<>(operation.variables(), rootRecord,
            fieldValueResolver, customTypeAdapters, ResponseNormalizer.NO_OP_NORMALIZER);
        try {
          return operation.wrapData(responseFieldMapper.map(responseReader));
        } catch (final Exception e) {
          logger.e(e, "Failed to read cached operation data. Operation: %s", operation);
          return null;
        }
      }
    });
  }

  @Nonnull @Override public <D extends Operation.Data, T, V extends Operation.Variables> Response<T> read(
      @Nonnull final Operation<D, T, V> operation, @Nonnull final ResponseFieldMapper<D> responseFieldMapper,
      @Nonnull final ResponseNormalizer<Record> responseNormalizer) {
    checkNotNull(operation, "operation == null");
    checkNotNull(responseNormalizer, "responseNormalizer == null");
    checkNotNull(customTypeAdapters, "customTypeAdapters == null");

    return readTransaction(new Transaction<ReadableCache, Response<T>>() {
      @Nonnull @Override public Response<T> execute(ReadableCache cache) {
        Record rootRecord = cache.read(CacheKeyResolver.rootKeyForOperation(operation).key());
        if (rootRecord == null) {
          return new Response<>(operation);
        }

        CacheFieldValueResolver fieldValueResolver = new CacheFieldValueResolver(cache, operation.variables(),
            cacheKeyResolver());
        RealResponseReader<Record> responseReader = new RealResponseReader<>(operation.variables(), rootRecord,
            fieldValueResolver, customTypeAdapters, responseNormalizer);
        try {
          responseNormalizer.willResolveRootQuery(operation);
          T data = operation.wrapData(responseFieldMapper.map(responseReader));
          return new Response<T>(operation, data, null, responseNormalizer.dependentKeys());
        } catch (final Exception e) {
          logger.e(e, "Failed to read cached operation data. Operation: %s", operation);
          return new Response<>(operation);
        }
      }
    });
  }

  @Nullable @Override public <F extends Fragment> F read(@Nonnull final ResponseFieldMapper<F> responseFieldMapper,
      @Nonnull final CacheKey cacheKey, @Nonnull final Operation.Variables variables) {
    checkNotNull(responseFieldMapper, "responseFieldMapper == null");
    checkNotNull(cacheKey, "cacheKey == null");
    checkNotNull(variables, "variables == null");

    return readTransaction(new Transaction<ReadableCache, F>() {
      @Nullable @Override public F execute(ReadableCache cache) {
        Record rootRecord = cache.read(cacheKey.key());
        if (rootRecord == null) {
          return null;
        }

        CacheFieldValueResolver fieldValueResolver = new CacheFieldValueResolver(cache, variables, cacheKeyResolver());
        //noinspection unchecked
        RealResponseReader<Record> responseReader = new RealResponseReader<>(variables, rootRecord, fieldValueResolver,
            customTypeAdapters, ResponseNormalizer.NO_OP_NORMALIZER);
        try {
          return responseFieldMapper.map(responseReader);
        } catch (final Exception e) {
          logger.e(e, "Failed to read cached fragment data");
          return null;
        }
      }
    });
  }
}
