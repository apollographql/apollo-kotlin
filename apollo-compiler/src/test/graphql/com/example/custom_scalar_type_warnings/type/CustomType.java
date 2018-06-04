package com.example.custom_scalar_type_warnings.type;

import com.apollographql.apollo.api.ScalarType;
import java.lang.Class;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import javax.annotation.Generated;

@Generated("Apollo GraphQL")
public enum CustomType implements ScalarType {
  URL {
    @Override
    public String typeName() {
      return "URL";
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Class javaType() {
      return Object.class;
    }
  },

  ID {
    @Override
    public String typeName() {
      return "ID";
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Class javaType() {
      return String.class;
    }
  }
}
