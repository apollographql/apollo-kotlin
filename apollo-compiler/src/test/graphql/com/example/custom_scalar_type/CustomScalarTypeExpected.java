package com.example.custom_scalar_type.type;

import com.apollographql.android.api.graphql.ScalarTypeMapping;
import java.lang.Class;
import java.lang.String;
import java.util.Date;
import javax.annotation.Generated;

@Generated("Apollo GraphQL")
public enum CustomScalarType implements ScalarTypeMapping {
  DATE {
    public String type() {
      return "Date";
    }

    public Class clazz() {
      return Date.class;
    }
  }
}
