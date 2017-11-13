package com.apollographql.apollo.internal.cache.normalized;

import com.apollographql.apollo.api.GraphqlFragment;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.cache.CacheHeaders;
import com.apollographql.apollo.cache.normalized.ApolloStore;
import com.apollographql.apollo.cache.normalized.ApolloStoreOperation;
import com.apollographql.apollo.cache.normalized.CacheKey;
import com.apollographql.apollo.cache.normalized.CacheKeyResolver;
import com.apollographql.apollo.cache.normalized.NormalizedCache;
import com.apollographql.apollo.cache.normalized.OptimisticNormalizedCache;
import com.apollographql.apollo.cache.normalized.Record;
import com.apollographql.apollo.internal.ApolloLogger;
import com.apollographql.apollo.internal.field.CacheFieldValueResolver;
import com.apollographql.apollo.internal.response.RealResponseWriter;
import com.apollographql.apollo.internal.response.RealResponseReader;
import com.apollographql.apollo.response.ScalarTypeAdapters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

public final class RealApolloStore implements ApolloStore, ReadableStore, WriteableStore {
  private final OptimisticNormalizedCache optimisticCache;
  private final CacheKeyResolver cacheKeyResolver;
  private final ScalarTypeAdapters scalarTypeAdapters;
  private final ReadWriteLock lock;
  private final Set<RecordChangeSubscriber> subscribers;
  private final Executor dispatcher;
  private final ApolloLogger logger;

  public RealApolloStore(@Nonnull NormalizedCache normalizedCache, @Nonnull CacheKeyResolver cacheKeyResolver,
      @Nonnull final ScalarTypeAdapters scalarTypeAdapters, @Nonnull Executor dispatcher,
      @Nonnull ApolloLogger logger) {
    checkNotNull(normalizedCache, "cacheStore == null");

    this.optimisticCache = (OptimisticNormalizedCache) new OptimisticNormalizedCache().chain(normalizedCache);
    this.cacheKeyResolver = checkNotNull(cacheKeyResolver, "cacheKeyResolver == null");
    this.scalarTypeAdapters = checkNotNull(scalarTypeAdapters, "scalarTypeAdapters == null");
    this.dispatcher = checkNotNull(dispatcher, "dispatcher == null");
    this.logger = checkNotNull(logger, "logger == null");
    this.lock = new ReentrantReadWriteLock();
    this.subscribers = Collections.newSetFromMap(new WeakHashMap<RecordChangeSubscriber, Boolean>());
  }

  @Override public ResponseNormalizer<Map<String, Object>> networkResponseNormalizer() {
    return new ResponseNormalizer<Map<String, Object>>() {
      @Nonnull @Override public CacheKey resolveCacheKey(@Nonnull ResponseField field,
          @Nonnull Map<String, Object> record) {
        return cacheKeyResolver.fromFieldRecordSet(field, record);
      }
    };
  }

  @Override public ResponseNormalizer<Record> cacheResponseNormalizer() {
    return new ResponseNormalizer<Record>() {
      @Nonnull @Override public CacheKey resolveCacheKey(@Nonnull ResponseField field, @Nonnull Record record) {
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

  @Override public void publish(@Nonnull final Set<String> changedKeys) {
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

  @Override @Nonnull public ApolloStoreOperation<Boolean> clearAll() {
    return new ApolloStoreOperation<Boolean>(dispatcher) {
      @Override public Boolean perform() {
        return writeTransaction(new Transaction<WriteableStore, Boolean>() {
          @Override public Boolean execute(WriteableStore cache) {
            optimisticCache.clearAll();
            return Boolean.TRUE;
          }
        });
      }
    };
  }

  @Override @Nonnull public ApolloStoreOperation<Boolean> remove(@Nonnull final CacheKey cacheKey) {
    checkNotNull(cacheKey, "cacheKey == null");
    return new ApolloStoreOperation<Boolean>(dispatcher) {
      @Override protected Boolean perform() {
        return writeTransaction(new Transaction<WriteableStore, Boolean>() {
          @Override public Boolean execute(WriteableStore cache) {
            return optimisticCache.remove(cacheKey);
          }
        });
      }
    };
  }

  @Override @Nonnull public ApolloStoreOperation<Integer> remove(@Nonnull final List<CacheKey> cacheKeys) {
    checkNotNull(cacheKeys, "cacheKey == null");
    return new ApolloStoreOperation<Integer>(dispatcher) {
      @Override protected Integer perform() {
        return writeTransaction(new Transaction<WriteableStore, Integer>() {
          @Override public Integer execute(WriteableStore cache) {
            int count = 0;
            for (CacheKey cacheKey : cacheKeys) {
              if (optimisticCache.remove(cacheKey)) {
                count++;
              }
            }
            return count;
          }
        });
      }
    };
  }

  @Override public <R> R readTransaction(Transaction<ReadableStore, R> transaction) {
    lock.readLock().lock();
    try {
      return transaction.execute(RealApolloStore.this);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override public <R> R writeTransaction(Transaction<WriteableStore, R> transaction) {
    lock.writeLock().lock();
    try {
      return transaction.execute(RealApolloStore.this);
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override public NormalizedCache normalizedCache() {
    return optimisticCache;
  }

  @Nullable public Record read(@Nonnull String key, @Nonnull CacheHeaders cacheHeaders) {
    return optimisticCache.loadRecord(checkNotNull(key, "key == null"), cacheHeaders);
  }

  @Nonnull public Collection<Record> read(@Nonnull Collection<String> keys, @Nonnull CacheHeaders cacheHeaders) {
    return optimisticCache.loadRecords(checkNotNull(keys, "keys == null"), cacheHeaders);
  }

  @Nonnull public Set<String> merge(@Nonnull Collection<Record> recordSet, @Nonnull CacheHeaders cacheHeaders) {
    return optimisticCache.merge(checkNotNull(recordSet, "recordSet == null"), cacheHeaders);
  }

  @Override public Set<String> merge(Record record, @Nonnull CacheHeaders cacheHeaders) {
    return optimisticCache.merge(checkNotNull(record, "record == null"), cacheHeaders);
  }

  @Override public CacheKeyResolver cacheKeyResolver() {
    return cacheKeyResolver;
  }

  @Override @Nonnull public <D extends Operation.Data, T, V extends Operation.Variables> ApolloStoreOperation<T> read(
      @Nonnull final Operation<D, T, V> operation) {
    checkNotNull(operation, "operation == null");
    return new ApolloStoreOperation<T>(dispatcher) {
      @Override protected T perform() {
        return doRead(operation);
      }
    };
  }

  @Override @Nonnull public <D extends Operation.Data, T, V extends Operation.Variables>
  ApolloStoreOperation<Response<T>> read(@Nonnull final Operation<D, T, V> operation,
      @Nonnull final ResponseFieldMapper<D> responseFieldMapper,
      @Nonnull final ResponseNormalizer<Record> responseNormalizer, @Nonnull final CacheHeaders cacheHeaders) {
    checkNotNull(operation, "operation == null");
    checkNotNull(responseNormalizer, "responseNormalizer == null");
    return new ApolloStoreOperation<Response<T>>(dispatcher) {
      @Override protected Response<T> perform() {
        return doRead(operation, responseFieldMapper, responseNormalizer, cacheHeaders);
      }
    };
  }

  @Override @Nonnull public <F extends GraphqlFragment> ApolloStoreOperation<F> read(
      @Nonnull final ResponseFieldMapper<F> responseFieldMapper, @Nonnull final CacheKey cacheKey,
      @Nonnull final Operation.Variables variables) {
    checkNotNull(responseFieldMapper, "responseFieldMapper == null");
    checkNotNull(cacheKey, "cacheKey == null");
    checkNotNull(variables, "variables == null");
    return new ApolloStoreOperation<F>(dispatcher) {
      @Override protected F perform() {
        return doRead(responseFieldMapper, cacheKey, variables);
      }
    };
  }

  @Override @Nonnull public <D extends Operation.Data, T, V extends Operation.Variables>
  ApolloStoreOperation<Set<String>> write(@Nonnull final Operation<D, T, V> operation, @Nonnull final D operationData) {
    checkNotNull(operation, "operation == null");
    checkNotNull(operationData, "operationData == null");
    return new ApolloStoreOperation<Set<String>>(dispatcher) {
      @Override protected Set<String> perform() {
        return doWrite(operation, operationData, false, null);
      }
    };
  }

  @Override @Nonnull public <D extends Operation.Data, T, V extends Operation.Variables> ApolloStoreOperation<Boolean>
  writeAndPublish(@Nonnull final Operation<D, T, V> operation, @Nonnull final D operationData) {
    return new ApolloStoreOperation<Boolean>(dispatcher) {
      @Override protected Boolean perform() {
        Set<String> changedKeys = doWrite(operation, operationData, false, null);
        publish(changedKeys);
        return Boolean.TRUE;
      }
    };
  }

  @Override @Nonnull public ApolloStoreOperation<Set<String>> write(@Nonnull final GraphqlFragment fragment,
      @Nonnull final CacheKey cacheKey, @Nonnull final Operation.Variables variables) {
    checkNotNull(fragment, "fragment == null");
    checkNotNull(cacheKey, "cacheKey == null");
    checkNotNull(variables, "operation == null");

    if (cacheKey == CacheKey.NO_KEY) {
      throw new IllegalArgumentException("undefined cache key");
    }

    return new ApolloStoreOperation<Set<String>>(dispatcher) {
      @Override protected Set<String> perform() {
        return writeTransaction(new Transaction<WriteableStore, Set<String>>() {
          @Override public Set<String> execute(WriteableStore cache) {
            return doWrite(fragment, cacheKey, variables);
          }
        });
      }
    };
  }

  @Override @Nonnull public ApolloStoreOperation<Boolean> writeAndPublish(@Nonnull final GraphqlFragment fragment,
      @Nonnull final CacheKey cacheKey, @Nonnull final Operation.Variables variables) {
    return new ApolloStoreOperation<Boolean>(dispatcher) {
      @Override protected Boolean perform() {
        Set<String> changedKeys = doWrite(fragment, cacheKey, variables);
        publish(changedKeys);
        return Boolean.TRUE;
      }
    };
  }

  @Nonnull @Override
  public <D extends Operation.Data, T, V extends Operation.Variables> ApolloStoreOperation<Set<String>>
  writeOptimisticUpdates(@Nonnull final Operation<D, T, V> operation, @Nonnull final D operationData,
      @Nonnull final UUID mutationId) {
    return new ApolloStoreOperation<Set<String>>(dispatcher) {
      @Override protected Set<String> perform() {
        return doWrite(operation, operationData, true, mutationId);
      }
    };
  }

  @Nonnull @Override
  public <D extends Operation.Data, T, V extends Operation.Variables> ApolloStoreOperation<Boolean>
  writeOptimisticUpdatesAndPublish(@Nonnull final Operation<D, T, V> operation, @Nonnull final D operationData,
      @Nonnull final UUID mutationId) {
    return new ApolloStoreOperation<Boolean>(dispatcher) {
      @Override protected Boolean perform() {
        Set<String> changedKeys = doWrite(operation, operationData, true, mutationId);
        publish(changedKeys);
        return Boolean.TRUE;
      }
    };
  }

  @Nonnull @Override
  public ApolloStoreOperation<Set<String>> rollbackOptimisticUpdates(@Nonnull final UUID mutationId) {
    return new ApolloStoreOperation<Set<String>>(dispatcher) {
      @Override protected Set<String> perform() {
        return writeTransaction(new Transaction<WriteableStore, Set<String>>() {
          @Override public Set<String> execute(WriteableStore cache) {
            return optimisticCache.removeOptimisticUpdates(mutationId);
          }
        });
      }
    };
  }

  @Nonnull @Override
  public ApolloStoreOperation<Boolean> rollbackOptimisticUpdatesAndPublish(@Nonnull final UUID mutationId) {
    return new ApolloStoreOperation<Boolean>(dispatcher) {
      @Override protected Boolean perform() {
        Set<String> changedKeys = writeTransaction(new Transaction<WriteableStore, Set<String>>() {
          @Override public Set<String> execute(WriteableStore cache) {
            return optimisticCache.removeOptimisticUpdates(mutationId);
          }
        });
        publish(changedKeys);
        return Boolean.TRUE;
      }
    };
  }

  private <D extends Operation.Data, T, V extends Operation.Variables> T doRead(final Operation<D, T, V> operation) {
    return readTransaction(new Transaction<ReadableStore, T>() {
      @Nullable @Override public T execute(ReadableStore cache) {
        Record rootRecord = cache.read(CacheKeyResolver.rootKeyForOperation(operation).key(), CacheHeaders.NONE);
        if (rootRecord == null) {
          return null;
        }

        ResponseFieldMapper<D> responseFieldMapper = operation.responseFieldMapper();
        CacheFieldValueResolver fieldValueResolver = new CacheFieldValueResolver(cache, operation.variables(),
            cacheKeyResolver(), CacheHeaders.NONE);
        //noinspection unchecked
        RealResponseReader<Record> responseReader = new RealResponseReader<>(operation.variables(), rootRecord,
            fieldValueResolver, scalarTypeAdapters, ResponseNormalizer.NO_OP_NORMALIZER);
        return operation.wrapData(responseFieldMapper.map(responseReader));
      }
    });
  }

  private <D extends Operation.Data, T, V extends Operation.Variables> Response<T> doRead(
      final Operation<D, T, V> operation, final ResponseFieldMapper<D> responseFieldMapper,
      final ResponseNormalizer<Record> responseNormalizer, final CacheHeaders cacheHeaders) {
    return readTransaction(new Transaction<ReadableStore, Response<T>>() {
      @Nonnull @Override public Response<T> execute(ReadableStore cache) {
        Record rootRecord = cache.read(CacheKeyResolver.rootKeyForOperation(operation).key(), cacheHeaders);
        if (rootRecord == null) {
          return Response.<T>builder(operation).fromCache(true).build();
        }

        CacheFieldValueResolver fieldValueResolver = new CacheFieldValueResolver(cache, operation.variables(),
            cacheKeyResolver(), cacheHeaders);
        RealResponseReader<Record> responseReader = new RealResponseReader<>(operation.variables(), rootRecord,
            fieldValueResolver, scalarTypeAdapters, responseNormalizer);
        try {
          responseNormalizer.willResolveRootQuery(operation);
          T data = operation.wrapData(responseFieldMapper.map(responseReader));
          return Response.<T>builder(operation)
              .data(data)
              .fromCache(true)
              .dependentKeys(responseNormalizer.dependentKeys())
              .build();
        } catch (Exception e) {
          logger.e(e, "Failed to read cache response");
          return Response.<T>builder(operation).fromCache(true).build();
        }
      }
    });
  }

  private <F extends GraphqlFragment> F doRead(final ResponseFieldMapper<F> responseFieldMapper,
      final CacheKey cacheKey, final Operation.Variables variables) {
    return readTransaction(new Transaction<ReadableStore, F>() {
      @Nullable @Override public F execute(ReadableStore cache) {
        Record rootRecord = cache.read(cacheKey.key(), CacheHeaders.NONE);
        if (rootRecord == null) {
          return null;
        }

        CacheFieldValueResolver fieldValueResolver = new CacheFieldValueResolver(cache, variables,
            cacheKeyResolver(), CacheHeaders.NONE);
        //noinspection unchecked
        RealResponseReader<Record> responseReader = new RealResponseReader<>(variables, rootRecord,
            fieldValueResolver, scalarTypeAdapters, ResponseNormalizer.NO_OP_NORMALIZER);
        return responseFieldMapper.map(responseReader);
      }
    });
  }

  private <D extends Operation.Data, T, V extends Operation.Variables> Set<String> doWrite(
      final Operation<D, T, V> operation, final D operationData, final boolean optimistic,
      final UUID mutationId) {
    return writeTransaction(new Transaction<WriteableStore, Set<String>>() {
      @Override public Set<String> execute(WriteableStore cache) {
        RealResponseWriter responseWriter = new RealResponseWriter(operation.variables(), scalarTypeAdapters);
        operationData.marshaller().marshal(responseWriter);

        ResponseNormalizer<Map<String, Object>> responseNormalizer = networkResponseNormalizer();
        responseNormalizer.willResolveRootQuery(operation);
        responseWriter.resolveFields(responseNormalizer);
        if (optimistic) {
          List<Record> updatedRecords = new ArrayList<>();
          for (Record record : responseNormalizer.records()) {
            updatedRecords.add(record.toBuilder().mutationId(mutationId).build());
          }
          return optimisticCache.mergeOptimisticUpdates(updatedRecords);
        } else {
          return optimisticCache.merge(responseNormalizer.records(), CacheHeaders.NONE);
        }
      }
    });
  }

  private Set<String> doWrite(final GraphqlFragment fragment, final CacheKey cacheKey,
      final Operation.Variables variables) {
    return writeTransaction(new Transaction<WriteableStore, Set<String>>() {
      @Override public Set<String> execute(WriteableStore cache) {
        RealResponseWriter responseWriter = new RealResponseWriter(variables, scalarTypeAdapters);
        fragment.marshaller().marshal(responseWriter);

        ResponseNormalizer<Map<String, Object>> responseNormalizer = networkResponseNormalizer();
        responseNormalizer.willResolveRecord(cacheKey);
        responseWriter.resolveFields(responseNormalizer);

        return merge(responseNormalizer.records(), CacheHeaders.NONE);
      }
    });
  }
}
