package com.example.apollostack.sample;

import com.google.auto.value.AutoValue;

import com.example.DroidDetails;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import javax.annotation.Nullable;

@AutoValue
public abstract class DroidDetailsData implements DroidDetails.Data {
  @Override @Nullable public abstract Species species();

  public static JsonAdapter<DroidDetailsData> jsonAdapter(Moshi moshi) {
    return new AutoValue_DroidDetailsData.MoshiJsonAdapter(moshi);
  }

  @AutoValue
  public abstract static class Species implements Specy {
    public static JsonAdapter<Species> jsonAdapter(Moshi moshi) {
      return new AutoValue_DroidDetailsData_Species.MoshiJsonAdapter(moshi);
    }
  }
}
