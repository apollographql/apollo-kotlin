package com.example.input_object_type.type;

import java.lang.IllegalStateException;
import java.lang.String;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public final class ReviewInput {
  private final int stars;

  private final @Nullable String commentary;

  private final @Nonnull ColorInput favoriteColor;

  ReviewInput(int stars, @Nullable String commentary, @Nonnull ColorInput favoriteColor) {
    this.stars = stars;
    this.commentary = commentary;
    this.favoriteColor = favoriteColor;
  }

  public int stars() {
    return this.stars;
  }

  public @Nullable String commentary() {
    return this.commentary;
  }

  public @Nonnull ColorInput favoriteColor() {
    return this.favoriteColor;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private int stars;

    private @Nullable String commentary;

    private @Nonnull ColorInput favoriteColor;

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

    public Builder favoriteColor(@Nonnull ColorInput favoriteColor) {
      this.favoriteColor = favoriteColor;
      return this;
    }

    public ReviewInput build() {
      if (favoriteColor == null) throw new IllegalStateException("favoriteColor can't be null");
      return new ReviewInput(stars, commentary, favoriteColor);
    }
  }
}
