package com.apollostack.android;

import retrofit2.Retrofit;

import static com.apollostack.android.Utils.checkNotNull;

/** TODO */
public final class Apollo {
  private final Retrofit retrofit;
  private final GraphqlService service;

  private Apollo(Retrofit retrofit, Class<? extends GraphqlService> serviceClass) {
    this.retrofit = retrofit;
    this.service = retrofit.create(serviceClass);
  }

  /** TODO */
  public GraphqlService service() {
    return service;
  }

  /**
   * Build a new {@link Apollo}. <p> Calling {@link #retrofit} is required before calling {@link #build()}. All other
   * methods are optional.
   */
  public static class Builder {
    private Retrofit retrofit;
    private Class<? extends GraphqlService> service;

    /** Set the {@link Retrofit} instance to be used by Apollo to make HTTP requests. */
    public Builder retrofit(Retrofit retrofit) {
      this.retrofit = checkNotNull(retrofit, "retrofit == null");
      return this;
    }

    /** Set the service interface to be used by Retrofit. */
    public Builder service(Class<? extends GraphqlService> service) {
      this.service = checkNotNull(service, "service == null");
      return this;
    }

    /** Create the {@link Apollo} instance using the configured values. */
    public Apollo build() {
      if (retrofit == null) {
        throw new IllegalStateException("Retrofit required.");
      }
      if (service == null) {
        throw new IllegalStateException("GraphqlService required.");
      }
      return new Apollo(retrofit, service);
    }
  }
}
