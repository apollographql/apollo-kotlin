package com.example.input_object_type;

import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.Mutation;
import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ResponseReader;
import com.example.input_object_type.type.Episode;
import com.example.input_object_type.type.ReviewInput;
import java.io.IOException;
import java.lang.IllegalStateException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public final class TestQuery implements Mutation<TestQuery.Variables> {
  public static final String OPERATION_DEFINITION = "mutation TestQuery($ep: Episode!, $review: ReviewInput!) {\n"
      + "  createReview(episode: $ep, review: $review) {\n"
      + "    stars\n"
      + "    commentary\n"
      + "  }\n"
      + "}";

  public static final String QUERY_DOCUMENT = OPERATION_DEFINITION;

  private final TestQuery.Variables variables;

  public TestQuery(TestQuery.Variables variables) {
    this.variables = variables;
  }

  @Override
  public String queryDocument() {
    return QUERY_DOCUMENT;
  }

  @Override
  public TestQuery.Variables variables() {
    return variables;
  }

  public static final class Variables extends Operation.Variables {
    private final @Nonnull Episode ep;

    private final @Nonnull ReviewInput review;

    Variables(@Nonnull Episode ep, @Nonnull ReviewInput review) {
      this.ep = ep;
      this.review = review;
    }

    public @Nonnull Episode ep() {
      return ep;
    }

    public @Nonnull ReviewInput review() {
      return review;
    }

    public static Builder builder() {
      return new Builder();
    }

    public static final class Builder {
      private @Nonnull Episode ep;

      private @Nonnull ReviewInput review;

      Builder() {
      }

      public Builder ep(@Nonnull Episode ep) {
        this.ep = ep;
        return this;
      }

      public Builder review(@Nonnull ReviewInput review) {
        this.review = review;
        return this;
      }

      public Variables build() {
        if (ep == null) throw new IllegalStateException("ep can't be null");
        if (review == null) throw new IllegalStateException("review can't be null");
        return new Variables(ep, review);
      }
    }
  }

  public interface Data extends Operation.Data {
    @Nullable CreateReview createReview();

    interface CreateReview {
      int stars();

      @Nullable String commentary();

      final class Mapper implements ResponseFieldMapper<CreateReview> {
        final Factory factory;

        final Field[] fields = {
          Field.forInt("stars", "stars", null, false),
          Field.forString("commentary", "commentary", null, true)
        };

        public Mapper(@Nonnull Factory factory) {
          this.factory = factory;
        }

        @Override
        public CreateReview map(ResponseReader reader) throws IOException {
          final __ContentValues contentValues = new __ContentValues();
          reader.read(new ResponseReader.ValueHandler() {
            @Override
            public void handle(final int fieldIndex, final Object value) throws IOException {
              switch (fieldIndex) {
                case 0: {
                  contentValues.stars = (int) value;
                  break;
                }
                case 1: {
                  contentValues.commentary = (String) value;
                  break;
                }
              }
            }
          }, fields);
          return factory.creator().create(contentValues.stars, contentValues.commentary);
        }

        static final class __ContentValues {
          int stars;

          String commentary;
        }
      }

      interface Factory {
        Creator creator();
      }

      interface Creator {
        CreateReview create(int stars, @Nullable String commentary);
      }
    }

    final class Mapper implements ResponseFieldMapper<Data> {
      final Factory factory;

      final Field[] fields = {
        Field.forObject("createReview", "createReview", null, true, new Field.ObjectReader<CreateReview>() {
          @Override public CreateReview read(final ResponseReader reader) throws IOException {
            return new CreateReview.Mapper(factory.createReviewFactory()).map(reader);
          }
        })
      };

      public Mapper(@Nonnull Factory factory) {
        this.factory = factory;
      }

      @Override
      public Data map(ResponseReader reader) throws IOException {
        final __ContentValues contentValues = new __ContentValues();
        reader.read(new ResponseReader.ValueHandler() {
          @Override
          public void handle(final int fieldIndex, final Object value) throws IOException {
            switch (fieldIndex) {
              case 0: {
                contentValues.createReview = (CreateReview) value;
                break;
              }
            }
          }
        }, fields);
        return factory.creator().create(contentValues.createReview);
      }

      static final class __ContentValues {
        CreateReview createReview;
      }
    }

    interface Factory {
      Creator creator();

      CreateReview.Factory createReviewFactory();
    }

    interface Creator {
      Data create(@Nullable CreateReview createReview);
    }
  }
}
