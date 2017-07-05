package com.example.simple_fragment.fragment;

import com.apollographql.apollo.api.GraphqlFragment;
import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ResponseFieldMarshaller;
import com.apollographql.apollo.api.ResponseReader;
import com.apollographql.apollo.api.ResponseWriter;
import java.lang.NullPointerException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Generated;
import javax.annotation.Nonnull;

@Generated("Apollo GraphQL")
public class HeroDetails implements GraphqlFragment {
  static final ResponseField[] $responseFields = {
    ResponseField.forString("__typename", "__typename", null, false),
    ResponseField.forString("name", "name", null, false)
  };

  public static final String FRAGMENT_DEFINITION = "fragment HeroDetails on Character {\n"
      + "  __typename\n"
      + "  name\n"
      + "}";

  public static final List<String> POSSIBLE_TYPES = Collections.unmodifiableList(Arrays.asList( "Human", "Droid"));

  final @Nonnull String __typename;

  final @Nonnull String name;

  private volatile String $toString;

  private volatile int $hashCode;

  private volatile boolean $hashCodeMemoized;

  public HeroDetails(@Nonnull String __typename, @Nonnull String name) {
    if (__typename == null) {
      throw new NullPointerException("__typename can't be null");
    }
    this.__typename = __typename;
    if (name == null) {
      throw new NullPointerException("name can't be null");
    }
    this.name = name;
  }

  public @Nonnull String __typename() {
    return this.__typename;
  }

  /**
   * The name of the character
   */
  public @Nonnull String name() {
    return this.name;
  }

  public ResponseFieldMarshaller marshaller() {
    return new ResponseFieldMarshaller() {
      @Override
      public void marshal(ResponseWriter writer) {
        writer.writeString($responseFields[0], __typename);
        writer.writeString($responseFields[1], name);
      }
    };
  }

  @Override
  public String toString() {
    if ($toString == null) {
      $toString = "HeroDetails{"
        + "__typename=" + __typename + ", "
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
      return this.__typename.equals(that.__typename)
       && this.name.equals(that.name);
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
      $hashCode = h;
      $hashCodeMemoized = true;
    }
    return $hashCode;
  }

  public static final class Mapper implements ResponseFieldMapper<HeroDetails> {
    @Override
    public HeroDetails map(ResponseReader reader) {
      final String __typename = reader.readString($responseFields[0]);
      final String name = reader.readString($responseFields[1]);
      return new HeroDetails(__typename, name);
    }
  }
}
