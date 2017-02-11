package com.example.fragments_with_type_condition.fragment;

import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ResponseReader;
import java.io.IOException;
import java.lang.Double;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public interface HumanDetails {
  String FRAGMENT_DEFINITION = "fragment HumanDetails on Human {\n"
      + "  name\n"
      + "  height\n"
      + "}";

  String TYPE_CONDITION = "Human";

  @Nonnull String name();

  @Nullable Double height();

  final class Mapper implements ResponseFieldMapper<HumanDetails> {
    final Factory factory;

    final Field[] fields = {
      Field.forString("name", "name", null, false),
      Field.forDouble("height", "height", null, true)
    };

    public Mapper(@Nonnull Factory factory) {
      this.factory = factory;
    }

    @Override
    public HumanDetails map(ResponseReader reader) throws IOException {
      final __ContentValues contentValues = new __ContentValues();
      reader.read(new ResponseReader.ValueHandler() {
        @Override
        public void handle(final int fieldIndex, final Object value) throws IOException {
          switch (fieldIndex) {
            case 0: {
              contentValues.name = (String) value;
              break;
            }
            case 1: {
              contentValues.height = (Double) value;
              break;
            }
          }
        }
      }, fields);
      return factory.creator().create(contentValues.name, contentValues.height);
    }

    static final class __ContentValues {
      String name;

      Double height;
    }
  }

  interface Factory {
    @Nonnull Creator creator();
  }

  interface Creator {
    @Nonnull HumanDetails create(@Nonnull String name, @Nullable Double height);
  }
}
