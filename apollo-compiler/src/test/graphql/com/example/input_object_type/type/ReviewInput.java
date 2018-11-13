package com.example.input_object_type.type;

import com.apollographql.apollo.api.Input;
import com.apollographql.apollo.api.InputFieldMarshaller;
import com.apollographql.apollo.api.InputFieldWriter;
import com.apollographql.apollo.api.InputType;
import com.apollographql.apollo.api.internal.Utils;
import java.io.IOException;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import javax.annotation.Generated;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Generated("Apollo GraphQL")
public final class ReviewInput implements InputType {
  private final int stars;

  private final Input<Integer> nullableIntFieldWithDefaultValue;

  private final Input<String> commentary;

  private final @NotNull ColorInput favoriteColor;

  private final Input<Episode> enumWithDefaultValue;

  private final Input<Episode> nullableEnum;

  private final Input<List<Date>> listOfCustomScalar;

  private final Input<Date> customScalar;

  private final Input<List<Episode>> listOfEnums;

  private final Input<List<Integer>> listOfInt;

  private final Input<List<String>> listOfString;

  private final Input<Boolean> booleanWithDefaultValue;

  private final Input<List<List<String>>> listOfListOfString;

  private final Input<List<List<Episode>>> listOfListOfEnum;

  private final Input<List<List<Date>>> listOfListOfCustom;

  private final Input<List<List<ColorInput>>> listOfListOfObject;

  private final Input<String> capitalizedField;

  private transient volatile int $hashCode;

  private transient volatile boolean $hashCodeMemoized;

  ReviewInput(int stars, Input<Integer> nullableIntFieldWithDefaultValue, Input<String> commentary,
      @NotNull ColorInput favoriteColor, Input<Episode> enumWithDefaultValue,
      Input<Episode> nullableEnum, Input<List<Date>> listOfCustomScalar, Input<Date> customScalar,
      Input<List<Episode>> listOfEnums, Input<List<Integer>> listOfInt,
      Input<List<String>> listOfString, Input<Boolean> booleanWithDefaultValue,
      Input<List<List<String>>> listOfListOfString, Input<List<List<Episode>>> listOfListOfEnum,
      Input<List<List<Date>>> listOfListOfCustom, Input<List<List<ColorInput>>> listOfListOfObject,
      Input<String> capitalizedField) {
    this.stars = stars;
    this.nullableIntFieldWithDefaultValue = nullableIntFieldWithDefaultValue;
    this.commentary = commentary;
    this.favoriteColor = favoriteColor;
    this.enumWithDefaultValue = enumWithDefaultValue;
    this.nullableEnum = nullableEnum;
    this.listOfCustomScalar = listOfCustomScalar;
    this.customScalar = customScalar;
    this.listOfEnums = listOfEnums;
    this.listOfInt = listOfInt;
    this.listOfString = listOfString;
    this.booleanWithDefaultValue = booleanWithDefaultValue;
    this.listOfListOfString = listOfListOfString;
    this.listOfListOfEnum = listOfListOfEnum;
    this.listOfListOfCustom = listOfListOfCustom;
    this.listOfListOfObject = listOfListOfObject;
    this.capitalizedField = capitalizedField;
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
    return this.nullableIntFieldWithDefaultValue.value;
  }

  /**
   * Comment about the movie, optional
   */
  public @Nullable String commentary() {
    return this.commentary.value;
  }

  /**
   * Favorite color, optional
   */
  public @NotNull ColorInput favoriteColor() {
    return this.favoriteColor;
  }

  /**
   * for test purpose only
   */
  public @Nullable Episode enumWithDefaultValue() {
    return this.enumWithDefaultValue.value;
  }

  /**
   * for test purpose only
   */
  public @Nullable Episode nullableEnum() {
    return this.nullableEnum.value;
  }

  /**
   * for test purpose only
   */
  public @Nullable List<Date> listOfCustomScalar() {
    return this.listOfCustomScalar.value;
  }

  /**
   * for test purpose only
   */
  public @Nullable Date customScalar() {
    return this.customScalar.value;
  }

  /**
   * for test purpose only
   */
  public @Nullable List<Episode> listOfEnums() {
    return this.listOfEnums.value;
  }

  /**
   * for test purpose only
   */
  public @Nullable List<Integer> listOfInt() {
    return this.listOfInt.value;
  }

  /**
   * for test purpose only
   */
  public @Nullable List<String> listOfString() {
    return this.listOfString.value;
  }

  /**
   * for test purpose only
   */
  public @Nullable Boolean booleanWithDefaultValue() {
    return this.booleanWithDefaultValue.value;
  }

  /**
   * for test purpose only
   */
  public @Nullable List<List<String>> listOfListOfString() {
    return this.listOfListOfString.value;
  }

  /**
   * for test purpose only
   */
  public @Nullable List<List<Episode>> listOfListOfEnum() {
    return this.listOfListOfEnum.value;
  }

  /**
   * for test purpose only
   */
  public @Nullable List<List<Date>> listOfListOfCustom() {
    return this.listOfListOfCustom.value;
  }

  /**
   * for test purpose only
   */
  public @Nullable List<List<ColorInput>> listOfListOfObject() {
    return this.listOfListOfObject.value;
  }

  /**
   * for test purpose only
   */
  public @Nullable String capitalizedField() {
    return this.capitalizedField.value;
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public InputFieldMarshaller marshaller() {
    return new InputFieldMarshaller() {
      @Override
      public void marshal(InputFieldWriter writer) throws IOException {
        writer.writeInt("stars", stars);
        if (nullableIntFieldWithDefaultValue.defined) {
          writer.writeInt("nullableIntFieldWithDefaultValue", nullableIntFieldWithDefaultValue.value);
        }
        if (commentary.defined) {
          writer.writeString("commentary", commentary.value);
        }
        writer.writeObject("favoriteColor", favoriteColor.marshaller());
        if (enumWithDefaultValue.defined) {
          writer.writeString("enumWithDefaultValue", enumWithDefaultValue.value != null ? enumWithDefaultValue.value.rawValue() : null);
        }
        if (nullableEnum.defined) {
          writer.writeString("nullableEnum", nullableEnum.value != null ? nullableEnum.value.rawValue() : null);
        }
        if (listOfCustomScalar.defined) {
          writer.writeList("listOfCustomScalar", listOfCustomScalar.value != null ? new InputFieldWriter.ListWriter() {
            @Override
            public void write(InputFieldWriter.ListItemWriter listItemWriter) throws IOException {
              for (final Date $item : listOfCustomScalar.value) {
                listItemWriter.writeCustom(CustomType.DATE, $item);
              }
            }
          } : null);
        }
        if (customScalar.defined) {
          writer.writeCustom("customScalar", com.example.input_object_type.type.CustomType.DATE, customScalar.value != null ? customScalar.value : null);
        }
        if (listOfEnums.defined) {
          writer.writeList("listOfEnums", listOfEnums.value != null ? new InputFieldWriter.ListWriter() {
            @Override
            public void write(InputFieldWriter.ListItemWriter listItemWriter) throws IOException {
              for (final Episode $item : listOfEnums.value) {
                listItemWriter.writeString($item != null ? $item.rawValue() : null);
              }
            }
          } : null);
        }
        if (listOfInt.defined) {
          writer.writeList("listOfInt", listOfInt.value != null ? new InputFieldWriter.ListWriter() {
            @Override
            public void write(InputFieldWriter.ListItemWriter listItemWriter) throws IOException {
              for (final Integer $item : listOfInt.value) {
                listItemWriter.writeInt($item);
              }
            }
          } : null);
        }
        if (listOfString.defined) {
          writer.writeList("listOfString", listOfString.value != null ? new InputFieldWriter.ListWriter() {
            @Override
            public void write(InputFieldWriter.ListItemWriter listItemWriter) throws IOException {
              for (final String $item : listOfString.value) {
                listItemWriter.writeString($item);
              }
            }
          } : null);
        }
        if (booleanWithDefaultValue.defined) {
          writer.writeBoolean("booleanWithDefaultValue", booleanWithDefaultValue.value);
        }
        if (listOfListOfString.defined) {
          writer.writeList("listOfListOfString", listOfListOfString.value != null ? new InputFieldWriter.ListWriter() {
            @Override
            public void write(InputFieldWriter.ListItemWriter listItemWriter) throws IOException {
              for (final List<String> $item : listOfListOfString.value) {
                listItemWriter.writeList($item != null ? new InputFieldWriter.ListWriter() {
                  @Override
                  public void write(InputFieldWriter.ListItemWriter listItemWriter) throws
                      IOException {
                    for (final String $$item : $item) {
                      listItemWriter.writeString($$item);
                    }
                  }
                } : null);
              }
            }
          } : null);
        }
        if (listOfListOfEnum.defined) {
          writer.writeList("listOfListOfEnum", listOfListOfEnum.value != null ? new InputFieldWriter.ListWriter() {
            @Override
            public void write(InputFieldWriter.ListItemWriter listItemWriter) throws IOException {
              for (final List<Episode> $item : listOfListOfEnum.value) {
                listItemWriter.writeList($item != null ? new InputFieldWriter.ListWriter() {
                  @Override
                  public void write(InputFieldWriter.ListItemWriter listItemWriter) throws
                      IOException {
                    for (final Episode $$item : $item) {
                      listItemWriter.writeString($$item != null ? $$item.rawValue() : null);
                    }
                  }
                } : null);
              }
            }
          } : null);
        }
        if (listOfListOfCustom.defined) {
          writer.writeList("listOfListOfCustom", listOfListOfCustom.value != null ? new InputFieldWriter.ListWriter() {
            @Override
            public void write(InputFieldWriter.ListItemWriter listItemWriter) throws IOException {
              for (final List<Date> $item : listOfListOfCustom.value) {
                listItemWriter.writeList($item != null ? new InputFieldWriter.ListWriter() {
                  @Override
                  public void write(InputFieldWriter.ListItemWriter listItemWriter) throws
                      IOException {
                    for (final Date $$item : $item) {
                      listItemWriter.writeCustom(CustomType.DATE, $$item);
                    }
                  }
                } : null);
              }
            }
          } : null);
        }
        if (listOfListOfObject.defined) {
          writer.writeList("listOfListOfObject", listOfListOfObject.value != null ? new InputFieldWriter.ListWriter() {
            @Override
            public void write(InputFieldWriter.ListItemWriter listItemWriter) throws IOException {
              for (final List<ColorInput> $item : listOfListOfObject.value) {
                listItemWriter.writeList($item != null ? new InputFieldWriter.ListWriter() {
                  @Override
                  public void write(InputFieldWriter.ListItemWriter listItemWriter) throws
                      IOException {
                    for (final ColorInput $$item : $item) {
                      listItemWriter.writeObject($$item != null ? $$item.marshaller() : null);
                    }
                  }
                } : null);
              }
            }
          } : null);
        }
        if (capitalizedField.defined) {
          writer.writeString("CapitalizedField", capitalizedField.value);
        }
      }
    };
  }

  @Override
  public int hashCode() {
    if (!$hashCodeMemoized) {
      int h = 1;
      h *= 1000003;
      h ^= stars;
      h *= 1000003;
      h ^= nullableIntFieldWithDefaultValue.hashCode();
      h *= 1000003;
      h ^= commentary.hashCode();
      h *= 1000003;
      h ^= favoriteColor.hashCode();
      h *= 1000003;
      h ^= enumWithDefaultValue.hashCode();
      h *= 1000003;
      h ^= nullableEnum.hashCode();
      h *= 1000003;
      h ^= listOfCustomScalar.hashCode();
      h *= 1000003;
      h ^= customScalar.hashCode();
      h *= 1000003;
      h ^= listOfEnums.hashCode();
      h *= 1000003;
      h ^= listOfInt.hashCode();
      h *= 1000003;
      h ^= listOfString.hashCode();
      h *= 1000003;
      h ^= booleanWithDefaultValue.hashCode();
      h *= 1000003;
      h ^= listOfListOfString.hashCode();
      h *= 1000003;
      h ^= listOfListOfEnum.hashCode();
      h *= 1000003;
      h ^= listOfListOfCustom.hashCode();
      h *= 1000003;
      h ^= listOfListOfObject.hashCode();
      h *= 1000003;
      h ^= capitalizedField.hashCode();
      $hashCode = h;
      $hashCodeMemoized = true;
    }
    return $hashCode;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof ReviewInput) {
      ReviewInput that = (ReviewInput) o;
      return this.stars == that.stars
       && this.nullableIntFieldWithDefaultValue.equals(that.nullableIntFieldWithDefaultValue)
       && this.commentary.equals(that.commentary)
       && this.favoriteColor.equals(that.favoriteColor)
       && this.enumWithDefaultValue.equals(that.enumWithDefaultValue)
       && this.nullableEnum.equals(that.nullableEnum)
       && this.listOfCustomScalar.equals(that.listOfCustomScalar)
       && this.customScalar.equals(that.customScalar)
       && this.listOfEnums.equals(that.listOfEnums)
       && this.listOfInt.equals(that.listOfInt)
       && this.listOfString.equals(that.listOfString)
       && this.booleanWithDefaultValue.equals(that.booleanWithDefaultValue)
       && this.listOfListOfString.equals(that.listOfListOfString)
       && this.listOfListOfEnum.equals(that.listOfListOfEnum)
       && this.listOfListOfCustom.equals(that.listOfListOfCustom)
       && this.listOfListOfObject.equals(that.listOfListOfObject)
       && this.capitalizedField.equals(that.capitalizedField);
    }
    return false;
  }

  public static final class Builder {
    private int stars;

    private Input<Integer> nullableIntFieldWithDefaultValue = Input.fromNullable(10);

    private Input<String> commentary = Input.absent();

    private @NotNull ColorInput favoriteColor;

    private Input<Episode> enumWithDefaultValue = Input.fromNullable(Episode.safeValueOf("JEDI"));

    private Input<Episode> nullableEnum = Input.absent();

    private Input<List<Date>> listOfCustomScalar = Input.absent();

    private Input<Date> customScalar = Input.absent();

    private Input<List<Episode>> listOfEnums = Input.absent();

    private Input<List<Integer>> listOfInt = Input.fromNullable(Arrays.<Integer>asList(1, 2, 3));

    private Input<List<String>> listOfString = Input.fromNullable(Arrays.<String>asList("test1", "test2", "test3"));

    private Input<Boolean> booleanWithDefaultValue = Input.fromNullable(true);

    private Input<List<List<String>>> listOfListOfString = Input.absent();

    private Input<List<List<Episode>>> listOfListOfEnum = Input.absent();

    private Input<List<List<Date>>> listOfListOfCustom = Input.absent();

    private Input<List<List<ColorInput>>> listOfListOfObject = Input.absent();

    private Input<String> capitalizedField = Input.absent();

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
    public Builder nullableIntFieldWithDefaultValue(
        @Nullable Integer nullableIntFieldWithDefaultValue) {
      this.nullableIntFieldWithDefaultValue = Input.fromNullable(nullableIntFieldWithDefaultValue);
      return this;
    }

    /**
     * Comment about the movie, optional
     */
    public Builder commentary(@Nullable String commentary) {
      this.commentary = Input.fromNullable(commentary);
      return this;
    }

    /**
     * Favorite color, optional
     */
    public Builder favoriteColor(@NotNull ColorInput favoriteColor) {
      this.favoriteColor = favoriteColor;
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
     * for test purpose only
     */
    public Builder nullableEnum(@Nullable Episode nullableEnum) {
      this.nullableEnum = Input.fromNullable(nullableEnum);
      return this;
    }

    /**
     * for test purpose only
     */
    public Builder listOfCustomScalar(@Nullable List<Date> listOfCustomScalar) {
      this.listOfCustomScalar = Input.fromNullable(listOfCustomScalar);
      return this;
    }

    /**
     * for test purpose only
     */
    public Builder customScalar(@Nullable Date customScalar) {
      this.customScalar = Input.fromNullable(customScalar);
      return this;
    }

    /**
     * for test purpose only
     */
    public Builder listOfEnums(@Nullable List<Episode> listOfEnums) {
      this.listOfEnums = Input.fromNullable(listOfEnums);
      return this;
    }

    /**
     * for test purpose only
     */
    public Builder listOfInt(@Nullable List<Integer> listOfInt) {
      this.listOfInt = Input.fromNullable(listOfInt);
      return this;
    }

    /**
     * for test purpose only
     */
    public Builder listOfString(@Nullable List<String> listOfString) {
      this.listOfString = Input.fromNullable(listOfString);
      return this;
    }

    /**
     * for test purpose only
     */
    public Builder booleanWithDefaultValue(@Nullable Boolean booleanWithDefaultValue) {
      this.booleanWithDefaultValue = Input.fromNullable(booleanWithDefaultValue);
      return this;
    }

    /**
     * for test purpose only
     */
    public Builder listOfListOfString(@Nullable List<List<String>> listOfListOfString) {
      this.listOfListOfString = Input.fromNullable(listOfListOfString);
      return this;
    }

    /**
     * for test purpose only
     */
    public Builder listOfListOfEnum(@Nullable List<List<Episode>> listOfListOfEnum) {
      this.listOfListOfEnum = Input.fromNullable(listOfListOfEnum);
      return this;
    }

    /**
     * for test purpose only
     */
    public Builder listOfListOfCustom(@Nullable List<List<Date>> listOfListOfCustom) {
      this.listOfListOfCustom = Input.fromNullable(listOfListOfCustom);
      return this;
    }

    /**
     * for test purpose only
     */
    public Builder listOfListOfObject(@Nullable List<List<ColorInput>> listOfListOfObject) {
      this.listOfListOfObject = Input.fromNullable(listOfListOfObject);
      return this;
    }

    /**
     * for test purpose only
     */
    public Builder capitalizedField(@Nullable String capitalizedField) {
      this.capitalizedField = Input.fromNullable(capitalizedField);
      return this;
    }

    /**
     * for test purpose only
     */
    public Builder nullableIntFieldWithDefaultValueInput(
        @NotNull Input<Integer> nullableIntFieldWithDefaultValue) {
      this.nullableIntFieldWithDefaultValue = Utils.checkNotNull(nullableIntFieldWithDefaultValue, "nullableIntFieldWithDefaultValue == null");
      return this;
    }

    /**
     * Comment about the movie, optional
     */
    public Builder commentaryInput(@NotNull Input<String> commentary) {
      this.commentary = Utils.checkNotNull(commentary, "commentary == null");
      return this;
    }

    /**
     * for test purpose only
     */
    public Builder enumWithDefaultValueInput(@NotNull Input<Episode> enumWithDefaultValue) {
      this.enumWithDefaultValue = Utils.checkNotNull(enumWithDefaultValue, "enumWithDefaultValue == null");
      return this;
    }

    /**
     * for test purpose only
     */
    public Builder nullableEnumInput(@NotNull Input<Episode> nullableEnum) {
      this.nullableEnum = Utils.checkNotNull(nullableEnum, "nullableEnum == null");
      return this;
    }

    /**
     * for test purpose only
     */
    public Builder listOfCustomScalarInput(@NotNull Input<List<Date>> listOfCustomScalar) {
      this.listOfCustomScalar = Utils.checkNotNull(listOfCustomScalar, "listOfCustomScalar == null");
      return this;
    }

    /**
     * for test purpose only
     */
    public Builder customScalarInput(@NotNull Input<Date> customScalar) {
      this.customScalar = Utils.checkNotNull(customScalar, "customScalar == null");
      return this;
    }

    /**
     * for test purpose only
     */
    public Builder listOfEnumsInput(@NotNull Input<List<Episode>> listOfEnums) {
      this.listOfEnums = Utils.checkNotNull(listOfEnums, "listOfEnums == null");
      return this;
    }

    /**
     * for test purpose only
     */
    public Builder listOfIntInput(@NotNull Input<List<Integer>> listOfInt) {
      this.listOfInt = Utils.checkNotNull(listOfInt, "listOfInt == null");
      return this;
    }

    /**
     * for test purpose only
     */
    public Builder listOfStringInput(@NotNull Input<List<String>> listOfString) {
      this.listOfString = Utils.checkNotNull(listOfString, "listOfString == null");
      return this;
    }

    /**
     * for test purpose only
     */
    public Builder booleanWithDefaultValueInput(@NotNull Input<Boolean> booleanWithDefaultValue) {
      this.booleanWithDefaultValue = Utils.checkNotNull(booleanWithDefaultValue, "booleanWithDefaultValue == null");
      return this;
    }

    /**
     * for test purpose only
     */
    public Builder listOfListOfStringInput(@NotNull Input<List<List<String>>> listOfListOfString) {
      this.listOfListOfString = Utils.checkNotNull(listOfListOfString, "listOfListOfString == null");
      return this;
    }

    /**
     * for test purpose only
     */
    public Builder listOfListOfEnumInput(@NotNull Input<List<List<Episode>>> listOfListOfEnum) {
      this.listOfListOfEnum = Utils.checkNotNull(listOfListOfEnum, "listOfListOfEnum == null");
      return this;
    }

    /**
     * for test purpose only
     */
    public Builder listOfListOfCustomInput(@NotNull Input<List<List<Date>>> listOfListOfCustom) {
      this.listOfListOfCustom = Utils.checkNotNull(listOfListOfCustom, "listOfListOfCustom == null");
      return this;
    }

    /**
     * for test purpose only
     */
    public Builder listOfListOfObjectInput(
        @NotNull Input<List<List<ColorInput>>> listOfListOfObject) {
      this.listOfListOfObject = Utils.checkNotNull(listOfListOfObject, "listOfListOfObject == null");
      return this;
    }

    /**
     * for test purpose only
     */
    public Builder capitalizedFieldInput(@NotNull Input<String> capitalizedField) {
      this.capitalizedField = Utils.checkNotNull(capitalizedField, "capitalizedField == null");
      return this;
    }

    public ReviewInput build() {
      Utils.checkNotNull(favoriteColor, "favoriteColor == null");
      return new ReviewInput(stars, nullableIntFieldWithDefaultValue, commentary, favoriteColor, enumWithDefaultValue, nullableEnum, listOfCustomScalar, customScalar, listOfEnums, listOfInt, listOfString, booleanWithDefaultValue, listOfListOfString, listOfListOfEnum, listOfListOfCustom, listOfListOfObject, capitalizedField);
    }
  }
}
