package com.apollographql.android.cache.normalized;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

final class RecordWeigher {

  private static final int SIZE_OF_BOOLEAN = 16;
  private static final int SIZE_OF_BIG_DECIMAL = 32;
  private static final int SIZE_OF_ARRAY_OVERHEAD = 16;
  private static final int SIZE_OF_RECORD_OVERHEAD = 16;
  private static final int SIZE_OF_CACHE_REFERENCE_OVERHEAD = 16;

  static int byteChange(Object newValue, Object oldValue) {
    return weighField(newValue) - weighField(oldValue);
  }

  static int calculateBytes(Record record) {
    int size = SIZE_OF_RECORD_OVERHEAD + record.key().getBytes().length;
    for (Map.Entry<String, Object> field : record.fields().entrySet()) {
      size += (field.getKey().getBytes().length + weighField(field.getValue()));
    }
    return size;
  }

  private static int weighField(Object field) {
    if (field instanceof List) {
      int size = SIZE_OF_ARRAY_OVERHEAD;
      for (Object listItem : (List) field) {
        size += weighField(listItem);
      }
      return size;
    }
    if (field instanceof String) {
      return ((String) field).getBytes().length;
    } else if (field instanceof Boolean) {
      return SIZE_OF_BOOLEAN;
    } else if (field instanceof BigDecimal) {
      return SIZE_OF_BIG_DECIMAL;
    } else if (field instanceof CacheReference) {
      return SIZE_OF_CACHE_REFERENCE_OVERHEAD + ((CacheReference) field).key().getBytes().length;
    }
    throw new IllegalStateException("Unknown field type in Record. " + field.getClass().getName());
  }

}
