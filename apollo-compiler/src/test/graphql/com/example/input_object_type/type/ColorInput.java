package com.example.input_object_type.type;

import com.apollographql.apollo.api.Input;
import com.apollographql.apollo.api.InputFieldMarshaller;
import com.apollographql.apollo.api.InputFieldWriter;
import com.apollographql.apollo.api.internal.Utils;
import java.io.IOException;
import java.lang.Double;
import java.lang.Override;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public final class ColorInput {
  private final int red;

  private final Input<Double> green;

  private final double blue;

  private final Input<Episode> enumWithDefaultValue;

  ColorInput(int red, Input<Double> green, double blue, Input<Episode> enumWithDefaultValue) {
    this.red = red;
    this.green = green;
    this.blue = blue;
    this.enumWithDefaultValue = enumWithDefaultValue;
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
    return this.green.value;
  }

  /**
   * Blue color
   */
  public double blue() {
    return this.blue;
  }

  /**
   * for test purpose only
   */
  public @Nullable Episode enumWithDefaultValue() {
    return this.enumWithDefaultValue.value;
  }

  public static Builder builder() {
    return new Builder();
  }

  public InputFieldMarshaller marshaller() {
    return new InputFieldMarshaller() {
      @Override
      public void marshal(InputFieldWriter writer) throws IOException {
        writer.writeInt("red", red);
        if (green.defined) {
          writer.writeDouble("green", green.value);
        }
        writer.writeDouble("blue", blue);
        if (enumWithDefaultValue.defined) {
          writer.writeString("enumWithDefaultValue", enumWithDefaultValue.value != null ? enumWithDefaultValue.value.name() : null);
        }
      }
    };
  }

  public static final class Builder {
    private int red = 1;

    private Input<Double> green = Input.fromNullable(0.0);

    private double blue = 1.5;

    private Input<Episode> enumWithDefaultValue = Input.fromNullable(Episode.JEDI);

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
      this.green = Input.fromNullable(green);
      return this;
    }

    /**
     * Blue color
     */
    public Builder blue(double blue) {
      this.blue = blue;
      return this;
    }

    /**
     * for test purpose only
     */
    public Builder enumWithDefaultValue(@Nullable Episode enumWithDefaultValue) {
      this.enumWithDefaultValue = Input.fromNullable(enumWithDefaultValue);
      return this;
    }

    /**
     * Green color
     */
    public Builder greenInput(@Nonnull Input<Double> green) {
      this.green = Utils.checkNotNull(green, "green == null");
      return this;
    }

    /**
     * for test purpose only
     */
    public Builder enumWithDefaultValueInput(@Nonnull Input<Episode> enumWithDefaultValue) {
      this.enumWithDefaultValue = Utils.checkNotNull(enumWithDefaultValue, "enumWithDefaultValue == null");
      return this;
    }

    public ColorInput build() {
      return new ColorInput(red, green, blue, enumWithDefaultValue);
    }
  }
}
