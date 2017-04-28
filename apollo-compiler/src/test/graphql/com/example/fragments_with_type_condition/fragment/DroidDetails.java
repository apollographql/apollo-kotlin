package com.example.fragments_with_type_condition.fragment;

import com.apollographql.apollo.api.Field;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ResponseReader;
import com.apollographql.apollo.api.internal.Optional;
import java.io.IOException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public class DroidDetails {
  public static final String FRAGMENT_DEFINITION = "fragment DroidDetails on Droid {\n"
      + "  __typename\n"
      + "  name\n"
      + "  primaryFunction\n"
      + "}";

  public static final List<String> POSSIBLE_TYPES = Collections.unmodifiableList(Arrays.asList( "Droid"));

  private final @Nonnull String __typename;

  private final @Nonnull String name;

  private final Optional<String> primaryFunction;

  private volatile String $toString;

  private volatile int $hashCode;

  private volatile boolean $hashCodeMemoized;

  public DroidDetails(@Nonnull String __typename, @Nonnull String name,
      @Nullable String primaryFunction) {
    this.__typename = __typename;
    this.name = name;
    this.primaryFunction = Optional.fromNullable(primaryFunction);
  }

  public @Nonnull String __typename() {
    return this.__typename;
  }

  /**
   * What others call this droid
   */
  public @Nonnull String name() {
    return this.name;
  }

  /**
   * This droid's primary function
   */
  public Optional<String> primaryFunction() {
    return this.primaryFunction;
  }

  @Override
  public String toString() {
    if ($toString == null) {
      $toString = "DroidDetails{"
        + "__typename=" + __typename + ", "
        + "name=" + name + ", "
        + "primaryFunction=" + primaryFunction
        + "}";
    }
    return $toString;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof DroidDetails) {
      DroidDetails that = (DroidDetails) o;
      return ((this.__typename == null) ? (that.__typename == null) : this.__typename.equals(that.__typename))
       && ((this.name == null) ? (that.name == null) : this.name.equals(that.name))
       && ((this.primaryFunction == null) ? (that.primaryFunction == null) : this.primaryFunction.equals(that.primaryFunction));
    }
    return false;
  }

  @Override
  public int hashCode() {
    if (!$hashCodeMemoized) {
      int h = 1;
      h *= 1000003;
      h ^= (__typename == null) ? 0 : __typename.hashCode();
      h *= 1000003;
      h ^= (name == null) ? 0 : name.hashCode();
      h *= 1000003;
      h ^= (primaryFunction == null) ? 0 : primaryFunction.hashCode();
      $hashCode = h;
      $hashCodeMemoized = true;
    }
    return $hashCode;
  }

  public static final class Mapper implements ResponseFieldMapper<DroidDetails> {
    final Field[] fields = {
      Field.forString("__typename", "__typename", null, false),
      Field.forString("name", "name", null, false),
      Field.forString("primaryFunction", "primaryFunction", null, true)
    };

    @Override
    public DroidDetails map(ResponseReader reader) throws IOException {
      final String __typename = reader.read(fields[0]);
      final String name = reader.read(fields[1]);
      final String primaryFunction = reader.read(fields[2]);
      return new DroidDetails(__typename, name, primaryFunction);
    }
  }
}
