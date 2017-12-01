package com.example.union_inline_fragments.type;

import java.lang.Deprecated;
import java.lang.String;
import javax.annotation.Generated;

/**
 * The episodes in the Star Wars trilogy
 */
@Generated("Apollo GraphQL")
public enum Episode {
  /**
   * Star Wars Episode IV: A New Hope, released in 1977.
   */
  NEWHOPE,

  /**
   * Star Wars Episode V: The Empire Strikes Back, released in 1980.
   */
  EMPIRE,

  /**
   * Star Wars Episode VI: Return of the Jedi, released in 1983.
   */
  JEDI,

  /**
   * Test deprecated enum value
   * @deprecated For test purpose only
   */
  @Deprecated
  DEPRECATED,

  /**
   * Auto generated constant for unknown enum values
   */
  $UNKNOWN;

  public static Episode safeValueOf(String value) {
    for (Episode enumValue : values()) {
      if (enumValue.name().equals(value)) {
        return enumValue;
      }
    }
    return Episode.$UNKNOWN;
  }
}
