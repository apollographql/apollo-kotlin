package com.example.input_object_type.type;

import com.apollographql.apollo.api.Input;
import com.apollographql.apollo.api.InputFieldMarshaller;
import com.apollographql.apollo.api.InputFieldWriter;
import com.apollographql.apollo.api.internal.Utils;
import java.io.IOException;
import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import java.util.Date;
import java.util.List;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public final class ReviewInput {
  private final int stars;

  private final Input<Integer> nullableIntFieldWithDefaultValue;

  private final Input<String> commentary;

  private final @Nonnull ColorInput favoriteColor;

  private final Input<Episode> enumWithDefaultValue;

  private final Input<Episode> nullableEnum;

  private final Input<List<Date>> listOfCustomScalar;

  private final Input<List<Episode>> listOfEnums;

  ReviewInput(int stars, Input<Integer> nullableIntFieldWithDefaultValue, Input<String> commentary,
      @Nonnull ColorInput favoriteColor, Input<Episode> enumWithDefaultValue,
      Input<Episode> nullableEnum, Input<List<Date>> listOfCustomScalar,
      Input<List<Episode>> listOfEnums) {
    this.stars = stars;
    this.nullableIntFieldWithDefaultValue = nullableIntFieldWithDefaultValue;
    this.commentary = commentary;
    this.favoriteColor = favoriteColor;
    this.enumWithDefaultValue = enumWithDefaultValue;
    this.nullableEnum = nullableEnum;
    this.listOfCustomScalar = listOfCustomScalar;
    this.listOfEnums = listOfEnums;
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
  public @Nonnull ColorInput favoriteColor() {
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
  public @Nullable List<Episode> listOfEnums() {
    return this.listOfEnums.value;
  }

  public static Builder builder() {
    return new Builder();
  }

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
          writer.writeString("enumWithDefaultValue", enumWithDefaultValue.value != null ? enumWithDefaultValue.value.name() : null);
        }
        if (nullableEnum.defined) {
          writer.writeString("nullableEnum", nullableEnum.value != null ? nullableEnum.value.name() : null);
        }
        if (listOfCustomScalar.defined) {
          writer.writeList("listOfCustomScalar", listOfCustomScalar.value != null ? new InputFieldWriter.ListWriter() {
            @Override
            public void write(InputFieldWriter.ListItemWriter listItemWriter) throws IOException {
              for (Date $item : listOfCustomScalar.value) {
                listItemWriter.writeCustom(CustomType.DATE, $item);
              }
            }
          } : null);
        }
        if (listOfEnums.defined) {
          writer.writeList("listOfEnums", listOfEnums.value != null ? new InputFieldWriter.ListWriter() {
            @Override
            public void write(InputFieldWriter.ListItemWriter listItemWriter) throws IOException {
              for (Episode $item : listOfEnums.value) {
                listItemWriter.writeString($item.name());
              }
            }
          } : null);
        }
      }
    };
  }

  public static final class Builder {
    private int stars;

    private Input<Integer> nullableIntFieldWithDefaultValue = Input.fromNullable(10);

    private Input<String> commentary = Input.absent();

    private @Nonnull ColorInput favoriteColor;

    private Input<Episode> enumWithDefaultValue = Input.fromNullable(Episode.JEDI);

    private Input<Episode> nullableEnum = Input.absent();

    private Input<List<Date>> listOfCustomScalar = Input.absent();

    private Input<List<Episode>> listOfEnums = Input.absent();

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
    public Builder favoriteColor(@Nonnull ColorInput favoriteColor) {
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
    public Builder listOfEnums(@Nullable List<Episode> listOfEnums) {
      this.listOfEnums = Input.fromNullable(listOfEnums);
      return this;
    }

    public ReviewInput build() {
      Utils.checkNotNull(favoriteColor, "favoriteColor == null");
      return new ReviewInput(stars, nullableIntFieldWithDefaultValue, commentary, favoriteColor, enumWithDefaultValue, nullableEnum, listOfCustomScalar, listOfEnums);
    }
  }
}
