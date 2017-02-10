package com.example.fragment_in_fragment.fragment;

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
public interface PilotFragment {
  String FRAGMENT_DEFINITION = "fragment pilotFragment on Person {\n"
      + "  name\n"
      + "  homeworld {\n"
      + "    name\n"
      + "  }\n"
      + "}";

  String TYPE_CONDITION = "Person";

  @Nullable String name();

  @Nullable Homeworld homeworld();

  interface Homeworld {
    @Nullable String name();

    final class Mapper implements ResponseFieldMapper<Homeworld> {
      final Factory factory;

      final Field[] fields = {
        Field.forString("name", "name", null, true)
      };

      public Mapper(@Nonnull Factory factory) {
        this.factory = factory;
      }

      @Override
      public Homeworld map(ResponseReader reader) throws IOException {
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
      Homeworld create(@Nullable String name);
    }
  }

  final class Mapper implements ResponseFieldMapper<PilotFragment> {
    final Factory factory;

    final Field[] fields = {
      Field.forString("name", "name", null, true),
      Field.forObject("homeworld", "homeworld", null, true, new Field.ObjectReader<Homeworld>() {
        @Override public Homeworld read(final ResponseReader reader) throws IOException {
          return new Homeworld.Mapper(factory.homeworldFactory()).map(reader);
        }
      })
    };

    public Mapper(@Nonnull Factory factory) {
      this.factory = factory;
    }

    @Override
    public PilotFragment map(ResponseReader reader) throws IOException {
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
              contentValues.homeworld = (Homeworld) value;
              break;
            }
          }
        }
      }, fields);
      return factory.creator().create(contentValues.name, contentValues.homeworld);
    }

    static final class __ContentValues {
      String name;

      Homeworld homeworld;
    }
  }

  interface Factory {
    Creator creator();

    Homeworld.Factory homeworldFactory();
  }

  interface Creator {
    PilotFragment create(@Nullable String name, @Nullable Homeworld homeworld);
  }
}
