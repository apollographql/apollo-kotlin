package com.apollographql.android.converter.type;

import com.apollographql.android.api.graphql.ScalarType;

import java.util.Date;

public enum CustomType implements ScalarType {
  DATETIME {
    public String typeName() {
      return "DateTime";
    }

    @Override public Class javaType() {
      return Date.class;
    }
  },
  UNSUPPORTEDCUSTOMSCALARTYPENUMBER {
    public String typeName() {
      return "UnsupportedCustomScalarTypeNumber";
    }

    @Override public Class javaType() {
      return Object.class;
    }
  },
  UNSUPPORTEDCUSTOMSCALARTYPEBOOL {
    public String typeName() {
      return "UnsupportedCustomScalarTypeBool";
    }

    @Override public Class javaType() {
      return Object.class;
    }
  },
  UNSUPPORTEDCUSTOMSCALARTYPESTRING {
    public String typeName() {
      return "UnsupportedCustomScalarTypeString";
    }

    @Override public Class javaType() {
      return Object.class;
    }
  }

}
