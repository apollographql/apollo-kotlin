package com.example.fragments_with_type_condition.fragment;

import com.apollographql.apollo.api.Field;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ResponseReader;
import com.apollographql.apollo.api.internal.Optional;
import java.io.IOException;
import java.lang.Double;
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
public class HumanDetails {
  public static final String FRAGMENT_DEFINITION = "fragment HumanDetails on Human {\n"
      + "  __typename\n"
      + "  name\n"
      + "  height\n"
      + "}";

  public static final List<String> POSSIBLE_TYPES = Collections.unmodifiableList(Arrays.asList( "Human"));

  private final @Nonnull String name;

  private final Optional<Double> height;

  public HumanDetails(@Nonnull String name, @Nullable Double height) {
    this.name = name;
    this.height = Optional.fromNullable(height);
  }

  public @Nonnull String name() {
    return this.name;
  }

  public Optional<Double> height() {
    return this.height;
  }

  @Override
  public String toString() {
    return "HumanDetails{"
      + "name=" + name + ", "
      + "height=" + height
      + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof HumanDetails) {
      HumanDetails that = (HumanDetails) o;
      return ((this.name == null) ? (that.name == null) : this.name.equals(that.name))
       && ((this.height == null) ? (that.height == null) : this.height.equals(that.height));
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h = 1;
    h *= 1000003;
    h ^= (name == null) ? 0 : name.hashCode();
    h *= 1000003;
    h ^= (height == null) ? 0 : height.hashCode();
    return h;
  }

  public static final class Mapper implements ResponseFieldMapper<HumanDetails> {
    final Field[] fields = {
      Field.forString("name", "name", null, false),
      Field.forDouble("height", "height", null, true)
    };

    @Override
    public HumanDetails map(ResponseReader reader) throws IOException {
      final String name = reader.read(fields[0]);
      final Double height = reader.read(fields[1]);
      return new HumanDetails(name, height);
    }
  }
}
