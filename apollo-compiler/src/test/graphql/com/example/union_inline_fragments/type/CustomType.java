package com.example.union_inline_fragments.type;

import com.apollographql.apollo.api.ScalarType;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import javax.annotation.Generated;

@Generated("Apollo GraphQL")
public enum CustomType implements ScalarType {
  ID {
    @Override
    public String typeName() {
      return "ID";
    }

    @Override
    public Class javaType() {
      return String.class;
    }
  }
}
