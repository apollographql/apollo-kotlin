package com.example.input_object_type;

import java.lang.Integer;
import java.lang.String;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ReviewInput {
  @Nullable Integer stars;

  @Nonnull String commentary;

  @Nonnull ColorInput favoriteColor;

  ReviewInput() {
  }

  public @Nullable Integer stars() {
    return this.stars;
  }

  public @Nonnull String commentary() {
    return this.commentary;
  }

  public @Nonnull ColorInput favoriteColor() {
    return this.favoriteColor;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private final ReviewInput reviewInput = new ReviewInput();

    Builder() {
    }

    public Builder stars(@Nullable Integer stars) {
      reviewInput.stars = stars;
      return this;
    }

    public Builder commentary(@Nonnull String commentary) {
      reviewInput.commentary = commentary;
      return this;
    }

    public Builder favoriteColor(@Nonnull ColorInput favoriteColor) {
      reviewInput.favoriteColor = favoriteColor;
      return this;
    }

    public ReviewInput build() {
      return reviewInput;
    }
  }
}
