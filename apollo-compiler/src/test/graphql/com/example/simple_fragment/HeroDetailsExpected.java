package com.example.simple_fragment.fragment;

import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ResponseReader;
import java.io.IOException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import javax.annotation.Generated;
import javax.annotation.Nonnull;

@Generated("Apollo GraphQL")
public interface HeroDetails {
  String FRAGMENT_DEFINITION = "fragment HeroDetails on Character {\n"
      + "  __typename\n"
      + "  name\n"
      + "}";

  String TYPE_CONDITION = "Character";

  @Nonnull String name();

  final class Mapper implements ResponseFieldMapper<HeroDetails> {
    final Factory factory;

    final Field[] fields = {
      Field.forString("name", "name", null, false)
    };

    public Mapper(@Nonnull Factory factory) {
      this.factory = factory;
    }

    @Override
    public HeroDetails map(ResponseReader reader) throws IOException {
      final __ContentValues contentValues = new __ContentValues();
      reader.read(new ResponseReader.ValueHandler() {
        @Override
        public void handle(final int fieldIndex, final Object value) throws IOException {
          switch (fieldIndex) {
            case 0: {
              contentValues.name = (String) value;
              break;
            }
          }
        }
      }, fields);
      return factory.creator().create(contentValues.name);
    }

    static final class __ContentValues {
      String name;
    }
  }

  interface Factory {
    Creator creator();
  }

  interface Creator {
    HeroDetails create(@Nonnull String name);
  }
}
