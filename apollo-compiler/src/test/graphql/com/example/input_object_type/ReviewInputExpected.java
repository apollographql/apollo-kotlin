package com.example.input_object_type.type;

import java.lang.IllegalStateException;
import java.lang.Integer;
import java.lang.String;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ReviewInput {
  private final @Nullable Integer stars;

  private final @Nonnull String commentary;

  private final @Nonnull ColorInput favoriteColor;

  ReviewInput(@Nullable Integer stars, @Nonnull String commentary,
      @Nonnull ColorInput favoriteColor) {
    this.stars = stars;
    this.commentary = commentary;
    this.favoriteColor = favoriteColor;
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
    private @Nullable Integer stars;

    private @Nonnull String commentary;

    private @Nonnull ColorInput favoriteColor;

    Builder() {
    }

    public Builder stars(@Nullable Integer stars) {
      this.stars = stars;
      return this;
    }

    public Builder commentary(@Nonnull String commentary) {
      this.commentary = commentary;
      return this;
    }

    public Builder favoriteColor(@Nonnull ColorInput favoriteColor) {
      this.favoriteColor = favoriteColor;
      return this;
    }

    public ReviewInput build() {
      if (commentary == null) throw new IllegalStateException("commentary can't be null");
      if (favoriteColor == null) throw new IllegalStateException("favoriteColor can't be null");
      return new ReviewInput(stars, commentary, favoriteColor);
    }
  }
}
