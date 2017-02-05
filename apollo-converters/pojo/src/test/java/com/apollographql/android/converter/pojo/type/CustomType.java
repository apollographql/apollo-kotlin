package com.apollographql.android.converter.pojo.type;

import com.apollographql.android.api.graphql.ScalarType;
import java.lang.String;
import javax.annotation.Generated;

@Generated("Apollo GraphQL")
public enum CustomType implements ScalarType {
  DATETIME {
    public String typeName() {
      return "DateTime";
    }
  }
}
