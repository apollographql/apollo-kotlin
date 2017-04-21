package com.example.simple_fragment.fragment;

import com.apollographql.apollo.api.Field;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ResponseReader;
import java.io.IOException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Generated;
import javax.annotation.Nonnull;

@Generated("Apollo GraphQL")
public class HeroDetails {
  public static final String FRAGMENT_DEFINITION = "fragment HeroDetails on Character {\n"
      + "  __typename\n"
      + "  name\n"
      + "}";

  public static final List<String> POSSIBLE_TYPES = Collections.unmodifiableList(Arrays.asList( "Human", "Droid"));

  private final @Nonnull String name;

  private volatile String $toString;

  private volatile int $hashCode;

  private volatile boolean $hashCodeMemoized;

  public HeroDetails(@Nonnull String name) {
    this.name = name;
  }

  /**
   * The name of the character
   */
  public @Nonnull String name() {
    return this.name;
  }

  @Override
  public String toString() {
    if ($toString == null) {
      $toString = "HeroDetails{"
        + "name=" + name
        + "}";
    }
    return $toString;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof HeroDetails) {
      HeroDetails that = (HeroDetails) o;
      return ((this.name == null) ? (that.name == null) : this.name.equals(that.name));
    }
    return false;
  }

  @Override
  public int hashCode() {
    if (!$hashCodeMemoized) {
      int h = 1;
      h *= 1000003;
      h ^= (name == null) ? 0 : name.hashCode();
      $hashCode = h;
      $hashCodeMemoized = true;
    }
    return $hashCode;
  }

  public static final class Mapper implements ResponseFieldMapper<HeroDetails> {
    final Field[] fields = {
      Field.forString("name", "name", null, false)
    };

    @Override
    public HeroDetails map(ResponseReader reader) throws IOException {
      final String name = reader.read(fields[0]);
      return new HeroDetails(name);
    }
  }
}
