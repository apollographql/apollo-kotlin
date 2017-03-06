package com.apollographql.android.cache.normalized;

import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.impl.ResponseReaderShadow;
import com.apollographql.android.impl.util.SimpleStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

public final class ResponseNormalizer implements ResponseReaderShadow<Map<String, Object>> {
  private SimpleStack<List<String>> pathStack;
  private SimpleStack<Record> recordStack;
  private SimpleStack<Object> valueStack;
  private Set<String> dependentKeys;
  private final CacheKeyResolver cacheKeyResolver;

  private List<String> path;
  private Record currentRecord;
  private RecordSet recordSet;

  ResponseNormalizer(CacheKeyResolver cacheKeyResolver) {
    this.cacheKeyResolver = cacheKeyResolver;
  }

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
    currentRecord = new Record(Cache.rootKeyForOperation(operation));
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
    String dependentKey = currentRecord.key() + "." + cacheKey;
    dependentKeys.add(dependentKey);
    currentRecord.addField(cacheKey, value);

    if (recordStack.isEmpty()) {
      recordSet.merge(currentRecord);
    }
  }

  @Override public void didParseScalar(@Nullable Object value) {
    valueStack.push(value);
  }

  @Override public void willParseObject(Map<String, Object> objectMap) {
    pathStack.push(path);

    String cacheKey = cacheKeyResolver.resolve(objectMap);
    if (cacheKey == null || cacheKey.isEmpty()) {
      cacheKey = pathToString();
    } else {
      path = new ArrayList<>();
      path.add(cacheKey);
    }
    recordStack.push(currentRecord);
    currentRecord = new Record(cacheKey);
  }

  @Override public void didParseObject(Map<String, Object> objectMap) {
    path = pathStack.pop();
    valueStack.push(new CacheReference(currentRecord.key()));
    dependentKeys.add(currentRecord.key());
    recordSet.merge(currentRecord);
    currentRecord = recordStack.pop();
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
}
