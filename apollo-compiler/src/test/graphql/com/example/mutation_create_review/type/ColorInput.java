package com.example.mutation_create_review.type;

import java.lang.Double;
import javax.annotation.Generated;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public final class ColorInput {
  private final int red;

  private final @Nullable Double green;

  private final double blue;

  ColorInput(int red, @Nullable Double green, double blue) {
    this.red = red;
    this.green = green;
    this.blue = blue;
  }

  /**
   * Red color
   */
  public int red() {
    return this.red;
  }

  /**
   * Green color
   */
  public @Nullable Double green() {
    return this.green;
  }

  /**
   * Blue color
   */
  public double blue() {
    return this.blue;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private int red = 1;

    private @Nullable Double green = 0.0;

    private double blue = 1.5;

    Builder() {
    }

    /**
     * Red color
     */
    public Builder red(int red) {
      this.red = red;
      return this;
    }

    /**
     * Green color
     */
    public Builder green(@Nullable Double green) {
      this.green = green;
      return this;
    }

    /**
     * Blue color
     */
    public Builder blue(double blue) {
      this.blue = blue;
      return this;
    }

    public ColorInput build() {
      return new ColorInput(red, green, blue);
    }
  }
}
