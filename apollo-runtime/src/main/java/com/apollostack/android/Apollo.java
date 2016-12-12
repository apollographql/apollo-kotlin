package com.apollostack.android;

import retrofit2.Retrofit;

import static com.apollostack.android.Utils.checkNotNull;

/** TODO */
public final class Apollo {
  private final Retrofit retrofit;

  private Apollo(Retrofit retrofit) {
    this.retrofit = retrofit;
  }

  /**
   * Build a new {@link Apollo}. <p> Calling {@link #retrofit} is required before calling {@link #build()}. All other
   * methods are optional.
   */
  public static class Builder {
    private Retrofit retrofit;

    /** Set the {@link Retrofit} instance to be used by Apollo to make HTTP requests. */
    public Builder retrofit(Retrofit retrofit) {
      this.retrofit = checkNotNull(retrofit, "retrofit == null");
      return this;
    }

    /** Create the {@link Apollo} instance using the configured values. */
    public Apollo build() {
      if (retrofit == null) {
        throw new IllegalStateException("Retrofit required.");
      }
      return new Apollo(retrofit);
    }
  }
}
