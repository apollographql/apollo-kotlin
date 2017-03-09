package com.apollographql.android.cache.normalized;

import java.util.LinkedHashMap;
import java.util.Map;

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

  public void mergeWith(Record otherRecord) {
    for (Map.Entry<String, Object> field : otherRecord.fields.entrySet()) {
      Object newFieldValue = field.getValue();
      Object oldFieldValue = this.fields.get(field.getKey());
      if (newFieldValue != null && oldFieldValue == null || !oldFieldValue.equals(newFieldValue)) {
        this.fields.put(field.getKey(), newFieldValue);
      }
    }
  }

  public Map<String, Object> fields() {
    return fields;
  }
}
