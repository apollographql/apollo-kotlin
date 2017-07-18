package com.example.mutation_create_review_semantic_naming.type;

import com.apollographql.apollo.api.InputFieldMarshaller;
import com.apollographql.apollo.api.InputFieldWriter;
import java.io.IOException;
import java.lang.Double;
import java.lang.Override;
import javax.annotation.Generated;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public final class ColorInput {
  private final int red;

  private final @Nullable Double green;

  private final double blue;

  private final @Nullable Episode enumWithDefaultValue;

  ColorInput(int red, @Nullable Double green, double blue, @Nullable Episode enumWithDefaultValue) {
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
    return this.green;
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
    return this.enumWithDefaultValue;
  }

  public static Builder builder() {
    return new Builder();
  }

  public InputFieldMarshaller marshaller() {
    return new InputFieldMarshaller() {
      @Override
      public void marshal(InputFieldWriter writer) throws IOException {
        writer.writeInt("red", red);
        writer.writeDouble("green", green);
        writer.writeDouble("blue", blue);
        writer.writeString("enumWithDefaultValue", enumWithDefaultValue != null ? enumWithDefaultValue.name() : null);
      }
    };
  }

  public static final class Builder {
    private int red = 1;

    private @Nullable Double green = 0.0;

    private double blue = 1.5;

    private @Nullable Episode enumWithDefaultValue = Episode.JEDI;

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

    /**
     * for test purpose only
     */
    public Builder enumWithDefaultValue(@Nullable Episode enumWithDefaultValue) {
      this.enumWithDefaultValue = enumWithDefaultValue;
      return this;
    }

    public ColorInput build() {
      return new ColorInput(red, green, blue, enumWithDefaultValue);
    }
  }
}
