package com.example.input_object_type.type;

import java.lang.Double;
import javax.annotation.Nullable;

public final class ColorInput {
  private final int red;

  private final @Nullable Double green;

  private final double blue;

  ColorInput(int red, @Nullable Double green, double blue) {
    this.red = red;
    this.green = green;
    this.blue = blue;
  }

  public int red() {
    return this.red;
  }

  public @Nullable Double green() {
    return this.green;
  }

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

    public Builder red(int red) {
      this.red = red;
      return this;
    }

    public Builder green(@Nullable Double green) {
      this.green = green;
      return this;
    }

    public Builder blue(double blue) {
      this.blue = blue;
      return this;
    }

    public ColorInput build() {
      return new ColorInput(red, green, blue);
    }
  }
}
