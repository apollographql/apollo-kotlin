package com.starwars.api.type;

import java.lang.String;
import javax.annotation.Generated;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public final class ReviewInput {
  private final int stars;

  private final @Nullable String commentary;

  ReviewInput(int stars, @Nullable String commentary) {
    this.stars = stars;
    this.commentary = commentary;
  }

  public int stars() {
    return this.stars;
  }

  public @Nullable String commentary() {
    return this.commentary;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private int stars;

    private @Nullable String commentary;

    Builder() {
    }

    public Builder stars(int stars) {
      this.stars = stars;
      return this;
    }

    public Builder commentary(@Nullable String commentary) {
      this.commentary = commentary;
      return this;
    }

    public ReviewInput build() {
      return new ReviewInput(stars, commentary);
    }
  }
}
