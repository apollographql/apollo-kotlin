package com.example.apollostack.sample;

import com.google.auto.value.AutoValue;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import javax.annotation.Nullable;

import generatedIR.DroidDetails;

@AutoValue
public abstractClass class DroidDetailsData implements DroidDetails.Data {
  @Override @Nullable public abstractClass Species species();

  public static JsonAdapter<DroidDetailsData> jsonAdapter(Moshi moshi) {
    return new AutoValue_DroidDetailsData.MoshiJsonAdapter(moshi);
  }

  @AutoValue
  public abstractClass static class Species implements Specy {
    public static JsonAdapter<Species> jsonAdapter(Moshi moshi) {
      return new AutoValue_DroidDetailsData_Species.MoshiJsonAdapter(moshi);
    }
  }
}
