package com.example.pojo_all_planets;

import com.apollostack.api.graphql.Field;
import com.apollostack.api.graphql.ResponseReader;
import java.io.IOException;
import java.lang.String;
import java.util.List;
import javax.annotation.Nullable;

public class FilmFragment {
  public static final String FRAGMENT_DEFINITION = "fragment FilmFragment on Film {\n"
      + "  title\n"
      + "  producers\n"
      + "}";

  public static final String TYPE_CONDITION = "Film";

  private @Nullable String title;

  private @Nullable List<? extends String> producers;

  public FilmFragment(ResponseReader reader) throws IOException {
    reader.read(
      new ResponseReader.ValueHandler() {
        @Override public void handle(int fieldIndex__, Object value__) throws IOException {
          switch (fieldIndex__) {
            case 0: {
              title = (String) value__;
              break;
            }
            case 1: {
              producers = (List<? extends String>) value__;
              break;
            }
          }
        }
      },
      Field.forString("title", "title", null, true),
      Field.forList("producers", "producers", null, true, new Field.ListReader<String>() {
        @Override public String read(Field.ListItemReader reader) throws IOException {
          return reader.readString();
        }
      })
    );
  }

  public @Nullable String title() {
    return this.title;
  }

  public @Nullable List<? extends String> producers() {
    return this.producers;
  }
}
