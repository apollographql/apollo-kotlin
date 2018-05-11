package com.example.fragments_with_type_condition.fragment;

import com.apollographql.apollo.api.GraphqlFragment;
import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ResponseFieldMarshaller;
import com.apollographql.apollo.api.ResponseReader;
import com.apollographql.apollo.api.ResponseWriter;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.api.internal.Utils;
import java.lang.Double;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Generated;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Generated("Apollo GraphQL")
public class HumanDetails implements GraphqlFragment {
  static final ResponseField[] $responseFields = {
    ResponseField.forString("__typename", "__typename", null, false, Collections.<ResponseField.Condition>emptyList()),
    ResponseField.forString("name", "name", null, false, Collections.<ResponseField.Condition>emptyList()),
    ResponseField.forDouble("height", "height", null, true, Collections.<ResponseField.Condition>emptyList())
  };

  public static final String FRAGMENT_DEFINITION = "fragment HumanDetails on Human {\n"
      + "  __typename\n"
      + "  name\n"
      + "  height\n"
      + "}";

  public static final List<String> POSSIBLE_TYPES = Collections.unmodifiableList(Arrays.asList( "Human"));

  final @NotNull String __typename;

  final @NotNull String name;

  final Optional<Double> height;

  private volatile String $toString;

  private volatile int $hashCode;

  private volatile boolean $hashCodeMemoized;

  public HumanDetails(@NotNull String __typename, @NotNull String name, @Nullable Double height) {
    this.__typename = Utils.checkNotNull(__typename, "__typename == null");
    this.name = Utils.checkNotNull(name, "name == null");
    this.height = Optional.fromNullable(height);
  }

  public @NotNull String __typename() {
    return this.__typename;
  }

  /**
   * What this human calls themselves
   */
  public @NotNull String name() {
    return this.name;
  }

  /**
   * Height in the preferred unit, default is meters
   */
  public Optional<Double> height() {
    return this.height;
  }

  public ResponseFieldMarshaller marshaller() {
    return new ResponseFieldMarshaller() {
      @Override
      public void marshal(ResponseWriter writer) {
        writer.writeString($responseFields[0], __typename);
        writer.writeString($responseFields[1], name);
        writer.writeDouble($responseFields[2], height.isPresent() ? height.get() : null);
      }
    };
  }

  @Override
  public String toString() {
    if ($toString == null) {
      $toString = "HumanDetails{"
        + "__typename=" + __typename + ", "
        + "name=" + name + ", "
        + "height=" + height
        + "}";
    }
    return $toString;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof HumanDetails) {
      HumanDetails that = (HumanDetails) o;
      return this.__typename.equals(that.__typename)
       && this.name.equals(that.name)
       && this.height.equals(that.height);
    }
    return false;
  }

  @Override
  public int hashCode() {
    if (!$hashCodeMemoized) {
      int h = 1;
      h *= 1000003;
      h ^= __typename.hashCode();
      h *= 1000003;
      h ^= name.hashCode();
      h *= 1000003;
      h ^= height.hashCode();
      $hashCode = h;
      $hashCodeMemoized = true;
    }
    return $hashCode;
  }

  public static final class Mapper implements ResponseFieldMapper<HumanDetails> {
    @Override
    public HumanDetails map(ResponseReader reader) {
      final String __typename = reader.readString($responseFields[0]);
      final String name = reader.readString($responseFields[1]);
      final Double height = reader.readDouble($responseFields[2]);
      return new HumanDetails(__typename, name, height);
    }
  }
}
