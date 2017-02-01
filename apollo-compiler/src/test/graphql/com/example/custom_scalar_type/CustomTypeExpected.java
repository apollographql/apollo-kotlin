package com.example.custom_scalar_type.type;

import com.apollographql.android.api.graphql.ScalarType;
import java.lang.String;
import javax.annotation.Generated;

@Generated("Apollo GraphQL")
public enum CustomType implements ScalarType {
  DATE {
    public String typeName() {
      return "Date";
    }
  }
}
