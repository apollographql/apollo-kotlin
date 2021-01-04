package com.apollographql.apollo.internal;

import com.apollographql.apollo.api.Adaptable;
import com.apollographql.apollo.api.GraphqlFragment;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.api.CustomScalarAdapters;
import com.apollographql.apollo.api.internal.ApolloLogger;
import com.apollographql.apollo.api.internal.ResolveDelegate;
import com.apollographql.apollo.api.internal.ResponseAdapter;
import com.apollographql.apollo.cache.CacheHeaders;
import com.apollographql.apollo.cache.normalized.ApolloStore;
import com.apollographql.apollo.cache.normalized.ApolloStoreOperation;
import com.apollographql.apollo.cache.normalized.CacheKey;
import com.apollographql.apollo.cache.normalized.CacheKeyResolver;
import com.apollographql.apollo.cache.normalized.NormalizedCache;
import com.apollographql.apollo.cache.normalized.OptimisticNormalizedCache;
import com.apollographql.apollo.cache.normalized.Record;
import com.apollographql.apollo.cache.normalized.internal.CacheFieldValueResolver;
import com.apollographql.apollo.cache.normalized.internal.CacheKeyBuilder;
import com.apollographql.apollo.cache.normalized.internal.ReadableStore;
import com.apollographql.apollo.cache.normalized.internal.RealCacheKeyBuilder;
import com.apollographql.apollo.cache.normalized.internal.ResponseNormalizer;
import com.apollographql.apollo.cache.normalized.internal.Transaction;
import com.apollographql.apollo.cache.normalized.internal.WriteableStore;
import com.apollographql.apollo.api.internal.RealResponseReader;
import com.apollographql.apollo.internal.response.RealResponseWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

public final class RealApolloStore implements ApolloStore, ReadableStore, WriteableStore {
  final OptimisticNormalizedCache optimisticCache;
  final CacheKeyResolver cacheKeyResolver;
  final CustomScalarAdapters customScalarAdapters;
  private final ReadWriteLock lock;
  private final Set<RecordChangeSubscriber> subscribers;
  private final Executor dispatcher;
  private final CacheKeyBuilder cacheKeyBuilder;
  final ApolloLogger logger;

  public RealApolloStore(@NotNull NormalizedCache normalizedCache, @NotNull CacheKeyResolver cacheKeyResolver,
      @NotNull final CustomScalarAdapters customScalarAdapters, @NotNull Executor dispatcher,
      @NotNull ApolloLogger logger) {
    checkNotNull(normalizedCache, "cacheStore == null");

    this.optimisticCache = (OptimisticNormalizedCache) new OptimisticNormalizedCache().chain(normalizedCache);
    this.cacheKeyResolver = checkNotNull(cacheKeyResolver, "cacheKeyResolver == null");
    this.customScalarAdapters = checkNotNull(customScalarAdapters, "scalarTypeAdapters == null");
    this.dispatcher = checkNotNull(dispatcher, "dispatcher == null");
    this.logger = checkNotNull(logger, "logger == null");
    this.lock = new ReentrantReadWriteLock();
    this.subscribers = Collections.newSetFromMap(new WeakHashMap<RecordChangeSubscriber, Boolean>());
    this.cacheKeyBuilder = new RealCacheKeyBuilder();
  }

  @Override public ResponseNormalizer<Map<String, Object>> networkResponseNormalizer() {
    return new ResponseNormalizer<Map<String, Object>>() {
      @NotNull @Override public CacheKey resolveCacheKey(@NotNull ResponseField field,
          @NotNull Map<String, Object> record) {
        return cacheKeyResolver.fromFieldRecordSet(field, record);
      }

      @NotNull @Override public CacheKeyBuilder cacheKeyBuilder() {
        return cacheKeyBuilder;
      }
    };
  }

  @Override public ResponseNormalizer<Record> cacheResponseNormalizer() {
    return new ResponseNormalizer<Record>() {
      @NotNull @Override public CacheKey resolveCacheKey(@NotNull ResponseField field, @NotNull Record record) {
        return new CacheKey(record.getKey());
      }

      @NotNull @Override public CacheKeyBuilder cacheKeyBuilder() {
        return cacheKeyBuilder;
      }
    };
  }

  @Override public synchronized void subscribe(RecordChangeSubscriber subscriber) {
    subscribers.add(subscriber);
  }

  @Override public synchronized void unsubscribe(RecordChangeSubscriber subscriber) {
    subscribers.remove(subscriber);
  }

  @Override public void publish(@NotNull final Set<String> changedKeys) {
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

  @Override @NotNull public ApolloStoreOperation<Boolean> clearAll() {
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

  @Override @NotNull public ApolloStoreOperation<Boolean> remove(@NotNull final CacheKey cacheKey) {
    return remove(cacheKey, false);
  }

  @NotNull @Override public ApolloStoreOperation<Boolean> remove(@NotNull final CacheKey cacheKey,
      final boolean cascade) {
    checkNotNull(cacheKey, "cacheKey == null");
    return new ApolloStoreOperation<Boolean>(dispatcher) {
      @Override protected Boolean perform() {
        return writeTransaction(new Transaction<WriteableStore, Boolean>() {
          @Override public Boolean execute(WriteableStore cache) {
            return optimisticCache.remove(cacheKey, cascade);
          }
        });
      }
    };
  }

  @Override @NotNull public ApolloStoreOperation<Integer> remove(@NotNull final List<CacheKey> cacheKeys) {
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

  @Override
  @Nullable public Record read(@NotNull String key, @NotNull CacheHeaders cacheHeaders) {
    return optimisticCache.loadRecord(checkNotNull(key, "key == null"), cacheHeaders);
  }

  @Override
  @NotNull public Collection<Record> read(@NotNull Collection<String> keys, @NotNull CacheHeaders cacheHeaders) {
    return optimisticCache.loadRecords(checkNotNull(keys, "keys == null"), cacheHeaders);
  }

  @Override
  @NotNull public Set<String> merge(@NotNull Collection<Record> recordSet, @NotNull CacheHeaders cacheHeaders) {
    return optimisticCache.merge(checkNotNull(recordSet, "recordSet == null"), cacheHeaders);
  }

  @Override public Set<String> merge(@NotNull Record record, @NotNull CacheHeaders cacheHeaders) {
    return optimisticCache.merge(checkNotNull(record, "record == null"), cacheHeaders);
  }

  @Override public CacheKeyResolver cacheKeyResolver() {
    return cacheKeyResolver;
  }

  @Override @NotNull public <D extends Operation.Data> ApolloStoreOperation<D> read(
      @NotNull final Operation<D> operation) {
    checkNotNull(operation, "operation == null");
    return new ApolloStoreOperation<D>(dispatcher) {
      @Override protected D perform() {
        return doRead(operation);
      }
    };
  }

  @Override @NotNull public <D extends Operation.Data>
  ApolloStoreOperation<Response<D>> read(
      @NotNull final Operation<D> operation,
      @NotNull final ResponseNormalizer<Record> responseNormalizer,
      @NotNull final CacheHeaders cacheHeaders) {
    checkNotNull(operation, "operation == null");
    checkNotNull(responseNormalizer, "responseNormalizer == null");
    return new ApolloStoreOperation<Response<D>>(dispatcher) {
      @Override protected Response<D> perform() {
        return doRead(operation, responseNormalizer, cacheHeaders);
      }
    };
  }

  @Override @NotNull public <F> ApolloStoreOperation<F> read(
      @NotNull final ResponseAdapter<F> adapter,
      @NotNull final CacheKey cacheKey,
      @NotNull final Operation.Variables variables) {
    checkNotNull(cacheKey, "cacheKey == null");
    checkNotNull(variables, "variables == null");
    return new ApolloStoreOperation<F>(dispatcher) {
      @Override protected F perform() {
        return doRead(adapter, cacheKey, variables);
      }
    };
  }

  @Override @NotNull public <D extends Operation.Data>
  ApolloStoreOperation<Set<String>> write(@NotNull final Operation<D> operation, @NotNull final D operationData) {
    checkNotNull(operation, "operation == null");
    checkNotNull(operationData, "operationData == null");
    return new ApolloStoreOperation<Set<String>>(dispatcher) {
      @Override protected Set<String> perform() {
        return doWrite(operation, operationData, false, null);
      }
    };
  }

  @Override @NotNull public <D extends Operation.Data> ApolloStoreOperation<Boolean>
  writeAndPublish(@NotNull final Operation<D> operation, @NotNull final D operationData) {
    return new ApolloStoreOperation<Boolean>(dispatcher) {
      @Override protected Boolean perform() {
        Set<String> changedKeys = doWrite(operation, operationData, false, null);
        publish(changedKeys);
        return Boolean.TRUE;
      }
    };
  }

  @Override @NotNull public ApolloStoreOperation<Set<String>> write(@NotNull final Adaptable adaptable,
      @NotNull final CacheKey cacheKey, @NotNull final Operation.Variables variables) {
    checkNotNull(adaptable, "adaptable == null");
    checkNotNull(cacheKey, "cacheKey == null");
    checkNotNull(variables, "operation == null");

    if (cacheKey.equals(CacheKey.NO_KEY)) {
      throw new IllegalArgumentException("undefined cache key");
    }

    return new ApolloStoreOperation<Set<String>>(dispatcher) {
      @Override protected Set<String> perform() {
        return writeTransaction(new Transaction<WriteableStore, Set<String>>() {
          @Override public Set<String> execute(WriteableStore cache) {
            return doWrite(adaptable, cacheKey, variables);
          }
        });
      }
    };
  }

  @Override @NotNull public ApolloStoreOperation<Boolean> writeAndPublish(@NotNull final Adaptable adaptable,
      @NotNull final CacheKey cacheKey, @NotNull final Operation.Variables variables) {
    return new ApolloStoreOperation<Boolean>(dispatcher) {
      @Override protected Boolean perform() {
        Set<String> changedKeys = doWrite(adaptable, cacheKey, variables);
        publish(changedKeys);
        return Boolean.TRUE;
      }
    };
  }

  @NotNull @Override
  public <D extends Operation.Data> ApolloStoreOperation<Set<String>>
  writeOptimisticUpdates(@NotNull final Operation<D> operation, @NotNull final D operationData,
      @NotNull final UUID mutationId) {
    return new ApolloStoreOperation<Set<String>>(dispatcher) {
      @Override protected Set<String> perform() {
        return doWrite(operation, operationData, true, mutationId);
      }
    };
  }

  @NotNull @Override
  public <D extends Operation.Data> ApolloStoreOperation<Boolean>
  writeOptimisticUpdatesAndPublish(@NotNull final Operation<D> operation, @NotNull final D operationData,
      @NotNull final UUID mutationId) {
    return new ApolloStoreOperation<Boolean>(dispatcher) {
      @Override protected Boolean perform() {
        Set<String> changedKeys = doWrite(operation, operationData, true, mutationId);
        publish(changedKeys);
        return Boolean.TRUE;
      }
    };
  }

  @NotNull @Override
  public ApolloStoreOperation<Set<String>> rollbackOptimisticUpdates(@NotNull final UUID mutationId) {
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

  @NotNull @Override
  public ApolloStoreOperation<Boolean> rollbackOptimisticUpdatesAndPublish(@NotNull final UUID mutationId) {
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

  <D extends Operation.Data> D doRead(final Operation<D> operation) {
    return readTransaction(new Transaction<ReadableStore, D>() {
      @Nullable @Override public D execute(ReadableStore cache) {
        Record rootRecord = cache.read(CacheKeyResolver.rootKeyForOperation(operation).getKey(), CacheHeaders.NONE);
        if (rootRecord == null) {
          return null;
        }

        CacheFieldValueResolver fieldValueResolver = new CacheFieldValueResolver(
            cache,
            operation.variables(),
            cacheKeyResolver(),
            CacheHeaders.NONE,
            cacheKeyBuilder);
        //noinspection unchecked
        RealResponseReader<Record> responseReader = new RealResponseReader<Record>(operation.variables(), rootRecord,
            fieldValueResolver, customScalarAdapters, (ResolveDelegate<Record>) ResponseNormalizer.NO_OP_NORMALIZER);
        return operation.adapter().fromResponse(responseReader, null);
      }
    });
  }

  <D extends Operation.Data> Response<D> doRead(
      final Operation<D> operation,
      final ResponseNormalizer<Record> responseNormalizer,
      final CacheHeaders cacheHeaders) {
    return readTransaction(new Transaction<ReadableStore, Response<D>>() {
      @NotNull @Override public Response<D> execute(ReadableStore cache) {
        Record rootRecord = cache.read(CacheKeyResolver.rootKeyForOperation(operation).getKey(), cacheHeaders);
        if (rootRecord == null) {
          return Response.<D>builder(operation).fromCache(true).build();
        }

        CacheFieldValueResolver fieldValueResolver = new CacheFieldValueResolver(
            cache,
            operation.variables(),
            cacheKeyResolver(),
            cacheHeaders,
            cacheKeyBuilder);
        RealResponseReader<Record> responseReader = new RealResponseReader<>(operation.variables(), rootRecord,
            fieldValueResolver, customScalarAdapters, responseNormalizer);
        try {
          responseNormalizer.willResolveRootQuery(operation);
          D data = operation.adapter().fromResponse(responseReader, null);
          return Response.<D>builder(operation)
              .data(data)
              .fromCache(true)
              .dependentKeys(responseNormalizer.dependentKeys())
              .build();
        } catch (Exception e) {
          logger.e(e, "Failed to read cache response");
          return Response.<D>builder(operation).fromCache(true).build();
        }
      }
    });
  }

  <F> F doRead(final ResponseAdapter<F> adapter,
      final CacheKey cacheKey, final Operation.Variables variables) {
    return readTransaction(new Transaction<ReadableStore, F>() {
      @Nullable @Override public F execute(ReadableStore cache) {
        Record rootRecord = cache.read(cacheKey.getKey(), CacheHeaders.NONE);
        if (rootRecord == null) {
          return null;
        }

        CacheFieldValueResolver fieldValueResolver = new CacheFieldValueResolver(cache, variables,
            cacheKeyResolver(), CacheHeaders.NONE, cacheKeyBuilder);
        //noinspection unchecked
        RealResponseReader<Record> responseReader = new RealResponseReader<Record>(variables, rootRecord,
            fieldValueResolver, customScalarAdapters, (ResolveDelegate<Record>) ResponseNormalizer.NO_OP_NORMALIZER);
        return adapter.fromResponse(responseReader, null);
      }
    });
  }

  <D extends Operation.Data> Set<String> doWrite(
      final Operation<D> operation, final D operationData, final boolean optimistic,
      final UUID mutationId) {
    return writeTransaction(new Transaction<WriteableStore, Set<String>>() {
      @Override public Set<String> execute(WriteableStore cache) {
        RealResponseWriter responseWriter = new RealResponseWriter(operation.variables(), customScalarAdapters);
        operation.adapter().toResponse(responseWriter, operationData);

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

  Set<String> doWrite(final Adaptable adaptable, final CacheKey cacheKey, final Operation.Variables variables) {
    return writeTransaction(new Transaction<WriteableStore, Set<String>>() {
      @Override public Set<String> execute(WriteableStore cache) {
        RealResponseWriter responseWriter = new RealResponseWriter(variables, customScalarAdapters);
        adaptable.adapter().toResponse(responseWriter, adaptable);

        ResponseNormalizer<Map<String, Object>> responseNormalizer = networkResponseNormalizer();
        responseNormalizer.willResolveRecord(cacheKey);
        responseWriter.resolveFields(responseNormalizer);

        return merge(responseNormalizer.records(), CacheHeaders.NONE);
      }
    });
  }
}
