package com.example.custom_scalar_type.type;

import com.apollographql.android.api.graphql.TypeMapping;
import java.lang.Class;
import java.lang.String;
import java.util.Date;
import javax.annotation.Generated;

@Generated("Apollo GraphQL")
public enum CustomType implements TypeMapping {
  DATE {
    public String type() {
      return "Date";
    }

    public Class clazz() {
      return Date.class;
    }
  }
}
