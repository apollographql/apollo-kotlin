package com.example.fragments_with_type_condition.fragment;

import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ResponseReader;
import java.io.IOException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public interface DroidDetails {
  String FRAGMENT_DEFINITION = "fragment DroidDetails on Droid {\n"
      + "  name\n"
      + "  primaryFunction\n"
      + "}";

  String TYPE_CONDITION = "Droid";

  @Nonnull String name();

  @Nullable String primaryFunction();

  final class Mapper implements ResponseFieldMapper<DroidDetails> {
    final Factory factory;

    final Field[] fields = {
      Field.forString("name", "name", null, false),
      Field.forString("primaryFunction", "primaryFunction", null, true)
    };

    public Mapper(@Nonnull Factory factory) {
      this.factory = factory;
    }

    @Override
    public DroidDetails map(ResponseReader reader) throws IOException {
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
              contentValues.primaryFunction = (String) value;
              break;
            }
          }
        }
      }, fields);
      return factory.creator().create(contentValues.name, contentValues.primaryFunction);
    }

    static final class __ContentValues {
      String name;

      String primaryFunction;
    }
  }

  interface Factory {
    @Nonnull Creator creator();
  }

  interface Creator {
    @Nonnull DroidDetails create(@Nonnull String name, @Nullable String primaryFunction);
  }
}
