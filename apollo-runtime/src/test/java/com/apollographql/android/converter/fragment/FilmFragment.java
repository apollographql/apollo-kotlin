package com.apollographql.android.converter.fragment;

import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ResponseReader;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FilmFragment {
  public static final String FRAGMENT_DEFINITION = "fragment FilmFragment on Film {\n"
      + "  title\n"
      + "  producers\n"
      + "}";

  public static final String TYPE_CONDITION = "Film";

  private @Nullable String title;

  private @Nullable List<? extends String> producers;

  public FilmFragment(@Nullable String title, @Nullable List<? extends String> producers) {
    this.title = title;
    this.producers = producers;
  }

  public @Nullable String title() {
    return this.title;
  }

  public @Nullable List<? extends String> producers() {
    return this.producers;
  }

  public static final class Mapper implements ResponseFieldMapper<FilmFragment> {
    final Field[] fields = {
      Field.forString("title", "title", null, true),
      Field.forList("producers", "producers", null, true, new Field.ListReader<String>() {
        @Override public String read(final Field.ListItemReader reader) throws IOException {
          return reader.readString();
        }
      })
    };

    @Override
    public FilmFragment map(ResponseReader reader) throws IOException {
      final __ContentValues contentValues = new __ContentValues();
      reader.read(new ResponseReader.ValueHandler() {
        @Override
        public void handle(final int fieldIndex, final Object value) throws IOException {
          switch (fieldIndex) {
            case 0: {
              contentValues.title = (String) value;
              break;
            }
            case 1: {
              contentValues.producers = (List<? extends String>) value;
              break;
            }
          }
        }
      }, fields);
      return new FilmFragment(contentValues.title, contentValues.producers);
    }

    static final class __ContentValues {
      String title;

      List<? extends String> producers;
    }
  }
}
