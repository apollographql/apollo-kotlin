package com.example.custom_scalar_type.type;

import com.apollographql.apollo.api.ScalarType;
import java.lang.Class;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.Date;
import javax.annotation.Generated;

@Generated("Apollo GraphQL")
public enum CustomType implements ScalarType {
  DATE {
    @Override
    public String typeName() {
      return "Date";
    }

    @Override
    public Class javaType() {
      return Date.class;
    }
  },

  UNSUPPORTEDTYPE {
    @Override
    public String typeName() {
      return "UnsupportedType";
    }

    @Override
    public Class javaType() {
      return Object.class;
    }
  },

  URL {
    @Override
    public String typeName() {
      return "URL";
    }

    @Override
    public Class javaType() {
      return String.class;
    }
  },

  ID {
    @Override
    public String typeName() {
      return "ID";
    }

    @Override
    public Class javaType() {
      return Integer.class;
    }
  }
}
