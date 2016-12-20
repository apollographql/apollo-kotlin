package com.example.apollostack.sample;

import java.lang.Integer;
import java.lang.String;

public final class ReviewInput {
   Integer stars;

   String commentary;

   ColorInput favoriteColor;

  ReviewInput() {
  }

  public  Integer stars() {
    return this.stars;
  }

  public  String commentary() {
    return this.commentary;
  }

  public  ColorInput favoriteColor() {
    return this.favoriteColor;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private final ReviewInput reviewInput = new ReviewInput();

    Builder() {
    }

    public Builder stars( Integer stars) {
      reviewInput.stars = stars;
      return this;
    }

    public Builder commentary( String commentary) {
      reviewInput.commentary = commentary;
      return this;
    }

    public Builder favoriteColor( ColorInput favoriteColor) {
      reviewInput.favoriteColor = favoriteColor;
      return this;
    }

    public ReviewInput build() {
      return reviewInput;
    }
  }
}
