package com.apollographql.android.cache.normalized;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class Record {

  private final String key;
  private final Map<String, Object> fields;
  private static final int UNKNOWN_SIZE_ESTIMATE = -1;
  private volatile int sizeInBytes = UNKNOWN_SIZE_ESTIMATE;

  public static class Builder {
    private final Map<String, Object> fields;
    private final String key;

    public Builder(String key) {
      this(key, new LinkedHashMap<String, Object>());
    }

    public Builder(String key, Map<String, Object> fields) {
      this.key = key;
      this.fields = fields;
    }

    public Builder addField(String key, Object value) {
      fields.put(key, value);
      return this;
    }

    public String key() {
      return key;
    }

    public Record build() {
      return new Record(key, fields);
    }
  }

  public static Builder builder(String key) {
    return new Builder(key);
  }

  public Builder toBuilder() {
    return new Builder(key(), this.fields);
  }

  public Record(String cacheKey) {
    this.key = cacheKey;
    fields = new LinkedHashMap<>();
  }

  public Record(String key, Map<String, Object> fields) {
    this.key = key;
    this.fields = fields;
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
        adjustSizeEstimate(newFieldValue, oldFieldValue);
      }
    }
    return changedKeys;
  }

  public Map<String, Object> fields() {
    return fields;
  }

  public int sizeEstimateBytes() {
    if (sizeInBytes == UNKNOWN_SIZE_ESTIMATE) {
      sizeInBytes = RecordWeigher.calculateBytes(this);
    }
    return sizeInBytes;
  }

  private void adjustSizeEstimate(Object newFieldValue, Object oldFieldValue) {
    if (sizeInBytes != UNKNOWN_SIZE_ESTIMATE) {
      sizeInBytes += RecordWeigher.byteChange(newFieldValue, oldFieldValue);
    }
  }

}
