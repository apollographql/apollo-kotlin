package com.example.fragment_with_inline_fragment;

import com.apollographql.apollo.api.FragmentResponseFieldMapper;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ResponseFieldMarshaller;
import com.apollographql.apollo.api.ResponseReader;
import com.apollographql.apollo.api.ResponseWriter;
import com.apollographql.apollo.api.internal.Mutator;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.api.internal.Utils;
import com.example.fragment_with_inline_fragment.fragment.HeroDetails;
import com.example.fragment_with_inline_fragment.type.Episode;
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
public final class TestQuery implements Query<TestQuery.Data, Optional<TestQuery.Data>, Operation.Variables> {
  public static final String OPERATION_DEFINITION = "query TestQuery {\n"
      + "  hero {\n"
      + "    __typename\n"
      + "    name\n"
      + "    ...HeroDetails\n"
      + "    appearsIn\n"
      + "  }\n"
      + "}";

  public static final String QUERY_DOCUMENT = OPERATION_DEFINITION + "\n"
   + HeroDetails.FRAGMENT_DEFINITION;

  public static final OperationName OPERATION_NAME = new OperationName() {
    @Override
    public String name() {
      return "TestQuery";
    }
  };

  private final Operation.Variables variables;

  public TestQuery() {
    this.variables = Operation.EMPTY_VARIABLES;
  }

  @Override
  public String operationId() {
    return "82ed8ea1e7e7b7d0344905a747cc94a1228c7ea59c809e9feb9c9d3604ff07cd";
  }

  @Override
  public String queryDocument() {
    return QUERY_DOCUMENT;
  }

  @Override
  public Optional<TestQuery.Data> wrapData(TestQuery.Data data) {
    return Optional.fromNullable(data);
  }

  @Override
  public Operation.Variables variables() {
    return variables;
  }

  @Override
  public ResponseFieldMapper<TestQuery.Data> responseFieldMapper() {
    return new Data.Mapper();
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public OperationName name() {
    return OPERATION_NAME;
  }

  public static final class Builder {
    Builder() {
    }

    public TestQuery build() {
      return new TestQuery();
    }
  }

  public static class Data implements Operation.Data {
    static final ResponseField[] $responseFields = {
      ResponseField.forObject("hero", "hero", null, true, Collections.<ResponseField.Condition>emptyList())
    };

    final Optional<Hero> hero;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Data(@Nullable Hero hero) {
      this.hero = Optional.fromNullable(hero);
    }

    public Optional<Hero> hero() {
      return this.hero;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeObject($responseFields[0], hero.isPresent() ? hero.get().marshaller() : null);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Data{"
          + "hero=" + hero
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof Data) {
        Data that = (Data) o;
        return this.hero.equals(that.hero);
      }
      return false;
    }

    @Override
    public int hashCode() {
      if (!$hashCodeMemoized) {
        int h = 1;
        h *= 1000003;
        h ^= hero.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public Builder toBuilder() {
      Builder builder = new Builder();
      builder.hero = hero.isPresent() ? hero.get() : null;
      return builder;
    }

    public static Builder builder() {
      return new Builder();
    }

    public static final class Mapper implements ResponseFieldMapper<Data> {
      final Hero.Mapper heroFieldMapper = new Hero.Mapper();

      @Override
      public Data map(ResponseReader reader) {
        final Hero hero = reader.readObject($responseFields[0], new ResponseReader.ObjectReader<Hero>() {
          @Override
          public Hero read(ResponseReader reader) {
            return heroFieldMapper.map(reader);
          }
        });
        return new Data(hero);
      }
    }

    public static final class Builder {
      private @Nullable Hero hero;

      Builder() {
      }

      public Builder hero(@Nullable Hero hero) {
        this.hero = hero;
        return this;
      }

      public Builder hero(@Nonnull Mutator<Hero.Builder> mutator) {
        Utils.checkNotNull(mutator, "mutator == null");
        Hero.Builder builder = this.hero != null ? this.hero.toBuilder() : Hero.builder();
        mutator.accept(builder);
        this.hero = builder.build();
        return this;
      }

      public Data build() {
        return new Data(hero);
      }
    }
  }

  public static class Hero {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("name", "name", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forList("appearsIn", "appearsIn", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forFragment("__typename", "__typename", Arrays.asList("Human",
      "Droid"))
    };

    final @Nonnull String __typename;

    final @Nonnull String name;

    final @Nonnull List<Episode> appearsIn;

    private final @Nonnull Fragments fragments;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Hero(@Nonnull String __typename, @Nonnull String name, @Nonnull List<Episode> appearsIn,
        @Nonnull Fragments fragments) {
      this.__typename = Utils.checkNotNull(__typename, "__typename == null");
      this.name = Utils.checkNotNull(name, "name == null");
      this.appearsIn = Utils.checkNotNull(appearsIn, "appearsIn == null");
      this.fragments = Utils.checkNotNull(fragments, "fragments == null");
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

    /**
     * The movies this character appears in
     */
    public @Nonnull List<Episode> appearsIn() {
      return this.appearsIn;
    }

    public @Nonnull Fragments fragments() {
      return this.fragments;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          writer.writeString($responseFields[1], name);
          writer.writeList($responseFields[2], appearsIn, new ResponseWriter.ListWriter() {
            @Override
            public void write(Object value, ResponseWriter.ListItemWriter listItemWriter) {
              listItemWriter.writeString(((com.example.fragment_with_inline_fragment.type.Episode) value).name());
            }
          });
          fragments.marshaller().marshal(writer);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Hero{"
          + "__typename=" + __typename + ", "
          + "name=" + name + ", "
          + "appearsIn=" + appearsIn + ", "
          + "fragments=" + fragments
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof Hero) {
        Hero that = (Hero) o;
        return this.__typename.equals(that.__typename)
         && this.name.equals(that.name)
         && this.appearsIn.equals(that.appearsIn)
         && this.fragments.equals(that.fragments);
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
        h ^= appearsIn.hashCode();
        h *= 1000003;
        h ^= fragments.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public Builder toBuilder() {
      Builder builder = new Builder();
      builder.__typename = __typename;
      builder.name = name;
      builder.appearsIn = appearsIn;
      builder.fragments = fragments;
      return builder;
    }

    public static Builder builder() {
      return new Builder();
    }

    public static class Fragments {
      final @Nonnull HeroDetails heroDetails;

      private volatile String $toString;

      private volatile int $hashCode;

      private volatile boolean $hashCodeMemoized;

      public Fragments(@Nonnull HeroDetails heroDetails) {
        this.heroDetails = Utils.checkNotNull(heroDetails, "heroDetails == null");
      }

      public @Nonnull HeroDetails heroDetails() {
        return this.heroDetails;
      }

      public ResponseFieldMarshaller marshaller() {
        return new ResponseFieldMarshaller() {
          @Override
          public void marshal(ResponseWriter writer) {
            final HeroDetails $heroDetails = heroDetails;
            if ($heroDetails != null) {
              $heroDetails.marshaller().marshal(writer);
            }
          }
        };
      }

      @Override
      public String toString() {
        if ($toString == null) {
          $toString = "Fragments{"
            + "heroDetails=" + heroDetails
            + "}";
        }
        return $toString;
      }

      @Override
      public boolean equals(Object o) {
        if (o == this) {
          return true;
        }
        if (o instanceof Fragments) {
          Fragments that = (Fragments) o;
          return this.heroDetails.equals(that.heroDetails);
        }
        return false;
      }

      @Override
      public int hashCode() {
        if (!$hashCodeMemoized) {
          int h = 1;
          h *= 1000003;
          h ^= heroDetails.hashCode();
          $hashCode = h;
          $hashCodeMemoized = true;
        }
        return $hashCode;
      }

      public Builder toBuilder() {
        Builder builder = new Builder();
        builder.heroDetails = heroDetails;
        return builder;
      }

      public static Builder builder() {
        return new Builder();
      }

      public static final class Mapper implements FragmentResponseFieldMapper<Fragments> {
        final HeroDetails.Mapper heroDetailsFieldMapper = new HeroDetails.Mapper();

        @Override
        public @Nonnull Fragments map(ResponseReader reader, @Nonnull String conditionalType) {
          HeroDetails heroDetails = null;
          if (HeroDetails.POSSIBLE_TYPES.contains(conditionalType)) {
            heroDetails = heroDetailsFieldMapper.map(reader);
          }
          return new Fragments(Utils.checkNotNull(heroDetails, "heroDetails == null"));
        }
      }

      public static final class Builder {
        private @Nonnull HeroDetails heroDetails;

        Builder() {
        }

        public Builder heroDetails(@Nonnull HeroDetails heroDetails) {
          this.heroDetails = heroDetails;
          return this;
        }

        public Fragments build() {
          Utils.checkNotNull(heroDetails, "heroDetails == null");
          return new Fragments(heroDetails);
        }
      }
    }

    public static final class Mapper implements ResponseFieldMapper<Hero> {
      final Fragments.Mapper fragmentsFieldMapper = new Fragments.Mapper();

      @Override
      public Hero map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final String name = reader.readString($responseFields[1]);
        final List<Episode> appearsIn = reader.readList($responseFields[2], new ResponseReader.ListReader<Episode>() {
          @Override
          public Episode read(ResponseReader.ListItemReader listItemReader) {
            return Episode.safeValueOf(listItemReader.readString());
          }
        });
        final Fragments fragments = reader.readConditional($responseFields[3], new ResponseReader.ConditionalTypeReader<Fragments>() {
          @Override
          public Fragments read(String conditionalType, ResponseReader reader) {
            return fragmentsFieldMapper.map(reader, conditionalType);
          }
        });
        return new Hero(__typename, name, appearsIn, fragments);
      }
    }

    public static final class Builder {
      private @Nonnull String __typename;

      private @Nonnull String name;

      private @Nonnull List<Episode> appearsIn;

      private @Nonnull Fragments fragments;

      Builder() {
      }

      public Builder __typename(@Nonnull String __typename) {
        this.__typename = __typename;
        return this;
      }

      public Builder name(@Nonnull String name) {
        this.name = name;
        return this;
      }

      public Builder appearsIn(@Nonnull List<Episode> appearsIn) {
        this.appearsIn = appearsIn;
        return this;
      }

      public Builder fragments(@Nonnull Fragments fragments) {
        this.fragments = fragments;
        return this;
      }

      public Builder fragments(@Nonnull Mutator<Fragments.Builder> mutator) {
        Utils.checkNotNull(mutator, "mutator == null");
        Fragments.Builder builder = this.fragments != null ? this.fragments.toBuilder() : Fragments.builder();
        mutator.accept(builder);
        this.fragments = builder.build();
        return this;
      }

      public Hero build() {
        Utils.checkNotNull(__typename, "__typename == null");
        Utils.checkNotNull(name, "name == null");
        Utils.checkNotNull(appearsIn, "appearsIn == null");
        Utils.checkNotNull(fragments, "fragments == null");
        return new Hero(__typename, name, appearsIn, fragments);
      }
    }
  }
}
