package com.example.apollostack.sample;

import java.lang.Double;

public final class ColorInput {
  int red = 1;

   Double green = 0.0;

  double blue = 1.5;

  ColorInput() {
  }

  public int red() {
    return this.red;
  }

  public  Double green() {
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

    public Builder green( Double green) {
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
