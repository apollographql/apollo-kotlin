package com.apollographql.android.converter.pojo.fragment;

import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ResponseReader;
import java.io.IOException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public class FilmFragment {
  public static final Creator CREATOR = new Creator() {
    @Override
    public FilmFragment create(@Nullable String title, @Nullable List<? extends String> producers) {
      return new FilmFragment(title, producers);
    }
  };

  public static final Factory FACTORY = new Factory() {
    @Override
    public Creator creator() {
      return CREATOR;
    }
  };

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

  public interface Factory {
    Creator creator();
  }

  public interface Creator {
    FilmFragment create(@Nullable String title, @Nullable List<? extends String> producers);
  }

  public static final class Mapper implements ResponseFieldMapper<FilmFragment> {
    final Factory factory;

    final Field[] fields = {
      Field.forString("title", "title", null, true),
      Field.forList("producers", "producers", null, true, new Field.ListReader<String>() {
        @Override public String read(final Field.ListItemReader reader) throws IOException {
          return reader.readString();
        }
      })
    };

    public Mapper(@Nonnull Factory factory) {
      this.factory = factory;
    }

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
      return factory.creator().create(contentValues.title, contentValues.producers);
    }

    static final class __ContentValues {
      String title;

      List<? extends String> producers;
    }
  }
}
