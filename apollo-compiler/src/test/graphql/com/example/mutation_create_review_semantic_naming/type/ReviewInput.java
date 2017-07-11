package com.example.mutation_create_review_semantic_naming.type;

import java.lang.IllegalStateException;
import java.lang.Integer;
import java.lang.String;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public final class ReviewInput {
  private final int stars;

  private final @Nullable Integer nullableIntFieldWithDefaultValue;

  private final @Nullable String commentary;

  private final @Nonnull ColorInput favoriteColor;

  private final @Nullable Episode enumWithDefaultValue;

  ReviewInput(int stars, @Nullable Integer nullableIntFieldWithDefaultValue,
      @Nullable String commentary, @Nonnull ColorInput favoriteColor,
      @Nullable Episode enumWithDefaultValue) {
    this.stars = stars;
    this.nullableIntFieldWithDefaultValue = nullableIntFieldWithDefaultValue;
    this.commentary = commentary;
    this.favoriteColor = favoriteColor;
    this.enumWithDefaultValue = enumWithDefaultValue;
  }

  /**
   * 0-5 stars
   */
  public int stars() {
    return this.stars;
  }

  /**
   * for test purpose only
   */
  public @Nullable Integer nullableIntFieldWithDefaultValue() {
    return this.nullableIntFieldWithDefaultValue;
  }

  /**
   * Comment about the movie, optional
   */
  public @Nullable String commentary() {
    return this.commentary;
  }

  /**
   * Favorite color, optional
   */
  public @Nonnull ColorInput favoriteColor() {
    return this.favoriteColor;
  }

  /**
   * for test purpose only
   */
  public @Nullable Episode enumWithDefaultValue() {
    return this.enumWithDefaultValue;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private int stars;

    private @Nullable Integer nullableIntFieldWithDefaultValue = 10;

    private @Nullable String commentary;

    private @Nonnull ColorInput favoriteColor;

    private @Nullable Episode enumWithDefaultValue = Episode.JEDI;

    Builder() {
    }

    /**
     * 0-5 stars
     */
    public Builder stars(int stars) {
      this.stars = stars;
      return this;
    }

    /**
     * for test purpose only
     */
    public Builder nullableIntFieldWithDefaultValue(@Nullable Integer nullableIntFieldWithDefaultValue) {
      this.nullableIntFieldWithDefaultValue = nullableIntFieldWithDefaultValue;
      return this;
    }

    /**
     * Comment about the movie, optional
     */
    public Builder commentary(@Nullable String commentary) {
      this.commentary = commentary;
      return this;
    }

    /**
     * Favorite color, optional
     */
    public Builder favoriteColor(@Nonnull ColorInput favoriteColor) {
      this.favoriteColor = favoriteColor;
      return this;
    }

    /**
     * for test purpose only
     */
    public Builder enumWithDefaultValue(@Nullable Episode enumWithDefaultValue) {
      this.enumWithDefaultValue = enumWithDefaultValue;
      return this;
    }

    public ReviewInput build() {
      if (favoriteColor == null) throw new IllegalStateException("favoriteColor can't be null");
      return new ReviewInput(stars, nullableIntFieldWithDefaultValue, commentary, favoriteColor, enumWithDefaultValue);
    }
  }
}
