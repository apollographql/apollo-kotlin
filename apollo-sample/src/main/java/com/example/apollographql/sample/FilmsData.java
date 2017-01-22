package com.example.apollostack.sample;

import com.google.auto.value.AutoValue;

import com.example.Films;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.util.List;

import javax.annotation.Nullable;


@AutoValue
public abstract class FilmsData implements Films.Data {
  @Override @Nullable public abstract AllFilms allFilms();

  public static JsonAdapter<FilmsData> jsonAdapter(Moshi moshi) {
    return new AutoValue_FilmsData.MoshiJsonAdapter(moshi);
  }

  @AutoValue
  public abstract static class AllFilms implements AllFilm {
    @Override @Nullable public abstract List<FilmImpl> films();

    public static JsonAdapter<AllFilms> jsonAdapter(Moshi moshi) {
      return new AutoValue_FilmsData_AllFilms.MoshiJsonAdapter(moshi);
    }

    @AutoValue
    public abstract static class FilmImpl implements Film {
      public static JsonAdapter<FilmImpl> jsonAdapter(Moshi moshi) {
        return new AutoValue_FilmsData_AllFilms_FilmImpl.MoshiJsonAdapter(moshi);
      }
    }
  }
}
