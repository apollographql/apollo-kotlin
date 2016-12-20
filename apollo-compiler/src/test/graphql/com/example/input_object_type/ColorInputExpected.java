package com.example.input_object_type;

import java.lang.Double;
import javax.annotation.Nullable;

public final class ColorInput {
  int red = 1;

  @Nullable Double green = 0.0;

  double blue = 1.5;

  ColorInput() {
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
    private final ColorInput colorInput = new ColorInput();

    Builder() {
    }

    public Builder red(int red) {
      colorInput.red = red;
      return this;
    }

    public Builder green(@Nullable Double green) {
      colorInput.green = green;
      return this;
    }

    public Builder blue(double blue) {
      colorInput.blue = blue;
      return this;
    }

    public ColorInput build() {
      return colorInput;
    }
  }
}
