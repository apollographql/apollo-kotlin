package com.apollographql.android.converter.pojo;

import com.apollographql.android.api.graphql.TypeMapping;
import java.lang.Class;
import java.lang.String;
import java.util.Date;
import javax.annotation.Generated;

@Generated("Apollo GraphQL")
public enum CustomType implements TypeMapping {
  DATETIME {
    public String type() {
      return "DateTime";
    }

    public Class clazz() {
      return Date.class;
    }
  }
}
