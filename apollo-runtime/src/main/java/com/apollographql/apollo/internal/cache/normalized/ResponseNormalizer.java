package com.apollographql.apollo.internal.cache.normalized;

import com.apollographql.apollo.api.Field;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.cache.normalized.CacheKey;
import com.apollographql.apollo.cache.normalized.CacheKeyResolver;
import com.apollographql.apollo.cache.normalized.CacheReference;
import com.apollographql.apollo.cache.normalized.Record;
import com.apollographql.apollo.cache.normalized.RecordSet;
import com.apollographql.apollo.internal.reader.ResponseReaderShadow;
import com.apollographql.apollo.internal.util.SimpleStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class ResponseNormalizer<R> implements ResponseReaderShadow<R> {
  private SimpleStack<List<String>> pathStack;
  private SimpleStack<Record> recordStack;
  private SimpleStack<Object> valueStack;
  private Set<String> dependentKeys;
  private List<String> path;
  private Record.Builder currentRecordBuilder;
  private RecordSet recordSet;

  public Collection<Record> records() {
    return recordSet.allRecords();
  }

  public Set<String> dependentKeys() {
    return dependentKeys;
  }

  @Override public void willResolveRootQuery(Operation operation) {
    pathStack = new SimpleStack<>();
    recordStack = new SimpleStack<>();
    valueStack = new SimpleStack<>();
    dependentKeys = new HashSet<>();

    path = new ArrayList<>();
    currentRecordBuilder = Record.builder(CacheKeyResolver.rootKeyForOperation(operation).key());
    recordSet = new RecordSet();
  }

  @Override public void willResolve(Field field, Operation.Variables variables) {
    String key = field.cacheKey(variables);
    path.add(key);
  }

  @Override public void didResolve(Field field, Operation.Variables variables) {
    path.remove(path.size() - 1);
    Object value = valueStack.pop();
    String cacheKey = field.cacheKey(variables);
    String dependentKey = currentRecordBuilder.key() + "." + cacheKey;
    dependentKeys.add(dependentKey);
    currentRecordBuilder.addField(cacheKey, value);

    if (recordStack.isEmpty()) {
      recordSet.merge(currentRecordBuilder.build());
    }
  }

  @Override public void didParseScalar(@Nullable Object value) {
    valueStack.push(value);
  }

  @Override public void willParseObject(Field field, Optional<R> objectSource) {
    pathStack.push(path);

    CacheKey cacheKey = objectSource.isPresent() ? resolveCacheKey(field, objectSource.get()) : CacheKey.NO_KEY;
    String cacheKeyValue = cacheKey.key();
    if (cacheKey == CacheKey.NO_KEY) {
      cacheKeyValue = pathToString();
    } else {
      path = new ArrayList<>();
      path.add(cacheKeyValue);
    }
    recordStack.push(currentRecordBuilder.build());
    currentRecordBuilder = Record.builder(cacheKeyValue);
  }

  @Override public void didParseObject(Field field, Optional<R> objectSource) {
    path = pathStack.pop();
    Record completedRecord = currentRecordBuilder.build();
    valueStack.push(new CacheReference(completedRecord.key()));
    dependentKeys.add(completedRecord.key());
    recordSet.merge(completedRecord);
    currentRecordBuilder = recordStack.pop().toBuilder();
  }

  @Override public void didParseList(List array) {
    List<Object> parsedArray = new ArrayList<>(array.size());
    for (int i = 0, size = array.size(); i < size; i++) {
      parsedArray.add(0, valueStack.pop());
    }
    valueStack.push(parsedArray);
  }

  @Override public void willParseElement(int atIndex) {
    path.add(Integer.toString(atIndex));
  }

  @Override public void didParseElement(int atIndex) {
    path.remove(path.size() - 1);
  }

  @Override public void didParseNull() {
    valueStack.push(null);
  }

  @Nonnull public abstract CacheKey resolveCacheKey(@Nonnull Field field, @Nonnull R record);

  private String pathToString() {
    StringBuilder stringBuilder = new StringBuilder();
    for (int i = 0, size = path.size(); i < size; i++) {
      String pathPiece = path.get(i);
      stringBuilder.append(pathPiece);
      if (i < size - 1) {
        stringBuilder.append(".");
      }
    }
    return stringBuilder.toString();
  }

  @SuppressWarnings("unchecked") static final ResponseNormalizer NO_OP_NORMALIZER = new ResponseNormalizer() {
    @Override public void willResolveRootQuery(Operation operation) {
    }

    @Override public void willResolve(Field field, Operation.Variables variables) {
    }

    @Override public void didResolve(Field field, Operation.Variables variables) {
    }

    @Override public void didParseScalar(Object value) {
    }

    @Override public void willParseObject(Field field, Optional objectSource) {
    }

    @Override public void didParseObject(Field field, Optional objectSource) {
    }

    @Override public void didParseList(List array) {
    }

    @Override public void willParseElement(int atIndex) {
    }

    @Override public void didParseElement(int atIndex) {
    }

    @Override public void didParseNull() {
    }

    @Override public Collection<Record> records() {
      return Collections.emptyList();
    }

    @Override public Set<String> dependentKeys() {
      return Collections.emptySet();
    }

    @Nonnull @Override public CacheKey resolveCacheKey(@Nonnull Field field, @Nonnull Object record) {
      return CacheKey.NO_KEY;
    }
  };
}
