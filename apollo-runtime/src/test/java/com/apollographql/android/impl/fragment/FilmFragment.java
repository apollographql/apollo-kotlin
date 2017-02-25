package com.apollographql.android.impl.fragment;

import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ResponseReader;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nullable;

public class FilmFragment {
  public static final String FRAGMENT_DEFINITION = "fragment FilmFragment on Film {\n"
      + "  title\n"
      + "  producers\n"
      + "}";

  public static final String TYPE_CONDITION = "Film";

  private @Nullable String title;

  private @Nullable List<String> producers;

  public FilmFragment(@Nullable String title, @Nullable List<String> producers) {
    this.title = title;
    this.producers = producers;
  }

  public @Nullable String title() {
    return this.title;
  }

  public @Nullable List<String> producers() {
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
    public FilmFragment map(final ResponseReader reader) throws IOException {
      final String title = reader.read(fields[0]);
      final List<String> producers = reader.read(fields[1]);
      return new FilmFragment(title, producers);
    }
  }
}
