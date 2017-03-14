package com.apollographql.android.cache.normalized;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

//Todo: Enhance this class to better support generalized serialization
// https://github.com/apollographql/apollo-android/issues/265
public final class Record {

  private final String key;
  private final Map<String, Object> fields;

  public Record(String cacheKey) {
    this.key = cacheKey;
    fields = new LinkedHashMap<>();
  }

  public Record(String key, Map<String, Object> fields) {
    this.key = key;
    this.fields = fields;
  }

  public void addField(String key, Object value) {
    fields.put(key, value);
  }

  public Object field(String fieldKey) {
    return fields.get(fieldKey);
  }

  public String key() {
    return key;
  }

  public Set<String> mergeWith(Record otherRecord) {
    Set<String> changedKeys = new HashSet<>();
    for (Map.Entry<String, Object> field : otherRecord.fields.entrySet()) {
      Object newFieldValue = field.getValue();
      Object oldFieldValue = this.fields.get(field.getKey());
      if ((oldFieldValue == null && newFieldValue != null)
          || (oldFieldValue != null && !oldFieldValue.equals(newFieldValue))) {
        this.fields.put(field.getKey(), newFieldValue);
        changedKeys.add(key() + "." + field.getKey());
      }
    }
    return changedKeys;
  }

  public Map<String, Object> fields() {
    return fields;
  }
}
