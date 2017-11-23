package com.apollographql.apollo.cache.normalized;

import com.apollographql.apollo.internal.cache.normalized.RecordWeigher;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

/**
 * A normalized entry that corresponds to a response object. Object fields are stored if they are a GraphQL Scalars. If
 * a field is a GraphQL Object a {@link CacheReference} will be stored instead.
 */
public final class Record {
  private static final int UNKNOWN_SIZE_ESTIMATE = -1;

  private final String key;
  private final Map<String, Object> fields;
  private volatile UUID mutationId;
  private int sizeInBytes = UNKNOWN_SIZE_ESTIMATE;

  public static class Builder {
    private final Map<String, Object> fields;
    private final String key;
    private UUID mutationId;

    public Builder(String key, Map<String, Object> fields, UUID mutationId) {
      this.key = key;
      this.fields = new LinkedHashMap<>(fields);
      this.mutationId = mutationId;
    }

    public Builder addField(@Nonnull String key, @Nullable Object value) {
      fields.put(checkNotNull(key, "key == null"), value);
      return this;
    }

    public Builder addFields(@Nonnull Map<String, Object> fields) {
      checkNotNull(fields, "fields == null");
      this.fields.putAll(fields);
      return this;
    }

    public String key() {
      return key;
    }

    public Builder mutationId(UUID mutationId) {
      this.mutationId = mutationId;
      return this;
    }

    public Record build() {
      return new Record(key, fields, mutationId);
    }
  }

  public static Builder builder(@Nonnull String key) {
    return new Builder(checkNotNull(key, "key == null"), new LinkedHashMap<String, Object>(), null);
  }

  public Builder toBuilder() {
    return new Builder(key(), this.fields, mutationId);
  }

  Record(String key, Map<String, Object> fields, UUID mutationId) {
    this.key = key;
    this.fields = fields;
    this.mutationId = mutationId;
  }

  public Object field(String fieldKey) {
    return fields.get(fieldKey);
  }

  public boolean hasField(String fieldKey) {
    return fields.containsKey(fieldKey);
  }

  public String key() {
    return key;
  }

  public UUID mutationId() {
    return mutationId;
  }

  @Override
  public Record clone() {
    return toBuilder().build();
  }

  /**
   * @param otherRecord The record to merge into this record.
   * @return A set of field keys which have changed, or were added. A field key incorporates any GraphQL arguments in
   * addition to the field name.
   */
  public Set<String> mergeWith(Record otherRecord) {
    Set<String> changedKeys = new HashSet<>();
    for (Map.Entry<String, Object> field : otherRecord.fields.entrySet()) {
      Object newFieldValue = field.getValue();
      boolean hasOldFieldValue = this.fields.containsKey(field.getKey());
      Object oldFieldValue = this.fields.get(field.getKey());

      if (!hasOldFieldValue
          || (oldFieldValue == null && newFieldValue != null)
          || (oldFieldValue != null && !oldFieldValue.equals(newFieldValue))) {
        this.fields.put(field.getKey(), newFieldValue);
        changedKeys.add(key() + "." + field.getKey());
        adjustSizeEstimate(newFieldValue, oldFieldValue);
      }
    }
    mutationId = otherRecord.mutationId;
    return changedKeys;
  }

  /**
   * @return A map of fieldName to fieldValue. Where fieldValue is a GraphQL Scalar or {@link CacheReference} if it is a
   * GraphQL Object type.
   */
  public Map<String, Object> fields() {
    return fields;
  }

  /**
   * @return An approximate number of bytes this Record takes up.
   */
  public synchronized int sizeEstimateBytes() {
    if (sizeInBytes == UNKNOWN_SIZE_ESTIMATE) {
      sizeInBytes = RecordWeigher.calculateBytes(this);
    }
    return sizeInBytes;
  }

  private synchronized void adjustSizeEstimate(Object newFieldValue, Object oldFieldValue) {
    if (sizeInBytes != UNKNOWN_SIZE_ESTIMATE) {
      sizeInBytes += RecordWeigher.byteChange(newFieldValue, oldFieldValue);
    }
  }

}
