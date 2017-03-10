package com.example;

import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.FragmentResponseFieldMapper;
import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Query;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ResponseReader;
import com.apollographql.android.api.graphql.util.UnmodifiableMapBuilder;
import fragment.SpeciesInformation;
import java.io.IOException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public final class DroidDetailsSpeciesInfo implements Query<DroidDetailsSpeciesInfo.Data, Operation.Variables> {
  public static final String OPERATION_DEFINITION = "query DroidDetailsSpeciesInfo {\n"
      + "  species(id: \"c3BlY2llczoy\") {\n"
      + "    __typename\n"
      + "    id\n"
      + "    name\n"
      + "    ...SpeciesInformation\n"
      + "  }\n"
      + "}";

  public static final String QUERY_DOCUMENT = OPERATION_DEFINITION + "\n"
   + SpeciesInformation.FRAGMENT_DEFINITION;

  private final Operation.Variables variables;

  public DroidDetailsSpeciesInfo() {
    this.variables = Operation.EMPTY_VARIABLES;
  }

  @Override
  public String queryDocument() {
    return QUERY_DOCUMENT;
  }

  @Override
  public Operation.Variables variables() {
    return variables;
  }

  @Override
  public ResponseFieldMapper<? extends Operation.Data> responseFieldMapper() {
    return new Data.Mapper();
  }

  public static class Data implements Operation.Data {
    private final @Nullable Species species;

    public Data(@Nullable Species species) {
      this.species = species;
    }

    public @Nullable Species species() {
      return this.species;
    }

    @Override
    public String toString() {
      return "Data{"
        + "species=" + species
        + "}";
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof Data) {
        Data that = (Data) o;
        return ((this.species == null) ? (that.species == null) : this.species.equals(that.species));
      }
      return false;
    }

    @Override
    public int hashCode() {
      int h = 1;
      h *= 1000003;
      h ^= (species == null) ? 0 : species.hashCode();
      return h;
    }

    public static class Species {
      private final @Nonnull String id;

      private final @Nullable String name;

      private final Fragments fragments;

      public Species(@Nonnull String id, @Nullable String name, Fragments fragments) {
        this.id = id;
        this.name = name;
        this.fragments = fragments;
      }

      public @Nonnull String id() {
        return this.id;
      }

      public @Nullable String name() {
        return this.name;
      }

      public @Nonnull Fragments fragments() {
        return this.fragments;
      }

      @Override
      public String toString() {
        return "Species{"
          + "id=" + id + ", "
          + "name=" + name + ", "
          + "fragments=" + fragments
          + "}";
      }

      @Override
      public boolean equals(Object o) {
        if (o == this) {
          return true;
        }
        if (o instanceof Species) {
          Species that = (Species) o;
          return ((this.id == null) ? (that.id == null) : this.id.equals(that.id))
           && ((this.name == null) ? (that.name == null) : this.name.equals(that.name))
           && ((this.fragments == null) ? (that.fragments == null) : this.fragments.equals(that.fragments));
        }
        return false;
      }

      @Override
      public int hashCode() {
        int h = 1;
        h *= 1000003;
        h ^= (id == null) ? 0 : id.hashCode();
        h *= 1000003;
        h ^= (name == null) ? 0 : name.hashCode();
        h *= 1000003;
        h ^= (fragments == null) ? 0 : fragments.hashCode();
        return h;
      }

      public static class Fragments {
        private SpeciesInformation speciesInformation;

        public Fragments(SpeciesInformation speciesInformation) {
          this.speciesInformation = speciesInformation;
        }

        public @Nullable SpeciesInformation speciesInformation() {
          return this.speciesInformation;
        }

        @Override
        public String toString() {
          return "Fragments{"
            + "speciesInformation=" + speciesInformation
            + "}";
        }

        @Override
        public boolean equals(Object o) {
          if (o == this) {
            return true;
          }
          if (o instanceof Fragments) {
            Fragments that = (Fragments) o;
            return ((this.speciesInformation == null) ? (that.speciesInformation == null) : this.speciesInformation.equals(that.speciesInformation));
          }
          return false;
        }

        @Override
        public int hashCode() {
          int h = 1;
          h *= 1000003;
          h ^= (speciesInformation == null) ? 0 : speciesInformation.hashCode();
          return h;
        }

        public static final class Mapper implements FragmentResponseFieldMapper<Fragments> {
          final SpeciesInformation.Mapper speciesInformationFieldMapper = new SpeciesInformation.Mapper();

          @Override
          public @Nonnull Fragments map(ResponseReader reader, @Nonnull String conditionalType)
              throws IOException {
            SpeciesInformation speciesInformation = null;
            if (SpeciesInformation.POSSIBLE_TYPES.contains(conditionalType)) {
              speciesInformation = speciesInformationFieldMapper.map(reader);
            }
            return new Fragments(speciesInformation);
          }
        }
      }

      public static final class Mapper implements ResponseFieldMapper<Species> {
        final Fragments.Mapper fragmentsFieldMapper = new Fragments.Mapper();

        final Field[] fields = {
          Field.forString("id", "id", null, false),
          Field.forString("name", "name", null, true),
          Field.forConditionalType("__typename", "__typename", new Field.ConditionalTypeReader<Fragments>() {
            @Override
            public Fragments read(String conditionalType, ResponseReader reader) throws
                IOException {
              return fragmentsFieldMapper.map(reader, conditionalType);
            }
          })
        };

        @Override
        public Species map(ResponseReader reader) throws IOException {
          final String id = reader.read(fields[0]);
          final String name = reader.read(fields[1]);
          final Fragments fragments = reader.read(fields[2]);
          return new Species(id, name, fragments);
        }
      }
    }

    public static final class Mapper implements ResponseFieldMapper<Data> {
      final Species.Mapper speciesFieldMapper = new Species.Mapper();

      final Field[] fields = {
        Field.forObject("species", "species", new UnmodifiableMapBuilder<String, Object>(1)
          .put("id", "c3BlY2llczoy")
        .build(), true, new Field.ObjectReader<Species>() {
          @Override public Species read(final ResponseReader reader) throws IOException {
            return speciesFieldMapper.map(reader);
          }
        })
      };

      @Override
      public Data map(ResponseReader reader) throws IOException {
        final Species species = reader.read(fields[0]);
        return new Data(species);
      }
    }
  }
}
