package com.apollographql.android.converter.pojo;

import com.apollographql.android.api.graphql.ScalarType;

import java.lang.Class;
import java.lang.String;
import java.util.Date;
import javax.annotation.Generated;

@Generated("Apollo GraphQL")
public enum CustomType implements ScalarType {
  DATETIME {
    public String typeName() {
      return "DateTime";
    }
  }
}
