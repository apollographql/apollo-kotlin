package com.frontpage.api.posts;

import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.FragmentResponseFieldMapper;
import com.apollographql.android.api.graphql.Mutation;
import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ResponseReader;
import com.apollographql.android.api.graphql.util.UnmodifiableMapBuilder;
import com.frontpage.api.fragment.PostDetails;
import java.io.IOException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public final class Upvote implements Mutation<Upvote.Data, Upvote.Variables> {
  public static final String OPERATION_DEFINITION = "mutation Upvote($postId: Int!) {\n"
      + "  upvotePost(postId: $postId) {\n"
      + "    __typename\n"
      + "    ...PostDetails\n"
      + "  }\n"
      + "}";

  public static final String QUERY_DOCUMENT = OPERATION_DEFINITION + "\n"
   + PostDetails.FRAGMENT_DEFINITION;

  private final Upvote.Variables variables;

  public Upvote(Upvote.Variables variables) {
    this.variables = variables;
  }

  @Override
  public String queryDocument() {
    return QUERY_DOCUMENT;
  }

  @Override
  public Upvote.Variables variables() {
    return variables;
  }

  @Override
  public ResponseFieldMapper<? extends Operation.Data> responseFieldMapper() {
    return new Data.Mapper();
  }

  public static final class Variables extends Operation.Variables {
    private final int postId;

    private final Map<String, Object> valueMap = new LinkedHashMap<>();

    Variables(int postId) {
      this.postId = postId;
      this.valueMap.put("postId", postId);
    }

    public int postId() {
      return postId;
    }

    @Override
    public Map<String, Object> valueMap() {
      return Collections.unmodifiableMap(valueMap);
    }

    public static Builder builder() {
      return new Builder();
    }

    public static final class Builder {
      private int postId;

      Builder() {
      }

      public Builder postId(int postId) {
        this.postId = postId;
        return this;
      }

      public Variables build() {
        return new Variables(postId);
      }
    }
  }

  public static class Data implements Operation.Data {
    private final @Nullable UpvotePost upvotePost;

    public Data(@Nullable UpvotePost upvotePost) {
      this.upvotePost = upvotePost;
    }

    public @Nullable UpvotePost upvotePost() {
      return this.upvotePost;
    }

    @Override
    public String toString() {
      return "Data{"
        + "upvotePost=" + upvotePost
        + "}";
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof Data) {
        Data that = (Data) o;
        return ((this.upvotePost == null) ? (that.upvotePost == null) : this.upvotePost.equals(that.upvotePost));
      }
      return false;
    }

    @Override
    public int hashCode() {
      int h = 1;
      h *= 1000003;
      h ^= (upvotePost == null) ? 0 : upvotePost.hashCode();
      return h;
    }

    public static class UpvotePost {
      private final Fragments fragments;

      public UpvotePost(Fragments fragments) {
        this.fragments = fragments;
      }

      public @Nonnull Fragments fragments() {
        return this.fragments;
      }

      @Override
      public String toString() {
        return "UpvotePost{"
          + "fragments=" + fragments
          + "}";
      }

      @Override
      public boolean equals(Object o) {
        if (o == this) {
          return true;
        }
        if (o instanceof UpvotePost) {
          UpvotePost that = (UpvotePost) o;
          return ((this.fragments == null) ? (that.fragments == null) : this.fragments.equals(that.fragments));
        }
        return false;
      }

      @Override
      public int hashCode() {
        int h = 1;
        h *= 1000003;
        h ^= (fragments == null) ? 0 : fragments.hashCode();
        return h;
      }

      public static class Fragments {
        private PostDetails postDetails;

        public Fragments(PostDetails postDetails) {
          this.postDetails = postDetails;
        }

        public @Nullable PostDetails postDetails() {
          return this.postDetails;
        }

        @Override
        public String toString() {
          return "Fragments{"
            + "postDetails=" + postDetails
            + "}";
        }

        @Override
        public boolean equals(Object o) {
          if (o == this) {
            return true;
          }
          if (o instanceof Fragments) {
            Fragments that = (Fragments) o;
            return ((this.postDetails == null) ? (that.postDetails == null) : this.postDetails.equals(that.postDetails));
          }
          return false;
        }

        @Override
        public int hashCode() {
          int h = 1;
          h *= 1000003;
          h ^= (postDetails == null) ? 0 : postDetails.hashCode();
          return h;
        }

        public static final class Mapper implements FragmentResponseFieldMapper<Fragments> {
          final PostDetails.Mapper postDetailsFieldMapper = new PostDetails.Mapper();

          @Override
          public @Nonnull Fragments map(ResponseReader reader, @Nonnull String conditionalType)
              throws IOException {
            PostDetails postDetails = null;
            if (PostDetails.POSSIBLE_TYPES.contains(conditionalType)) {
              postDetails = postDetailsFieldMapper.map(reader);
            }
            return new Fragments(postDetails);
          }
        }
      }

      public static final class Mapper implements ResponseFieldMapper<UpvotePost> {
        final Fragments.Mapper fragmentsFieldMapper = new Fragments.Mapper();

        final Field[] fields = {
          Field.forConditionalType("__typename", "__typename", new Field.ConditionalTypeReader<Fragments>() {
            @Override
            public Fragments read(String conditionalType, ResponseReader reader) throws
                IOException {
              return fragmentsFieldMapper.map(reader, conditionalType);
            }
          })
        };

        @Override
        public UpvotePost map(ResponseReader reader) throws IOException {
          final Fragments fragments = reader.read(fields[0]);
          return new UpvotePost(fragments);
        }
      }
    }

    public static final class Mapper implements ResponseFieldMapper<Data> {
      final UpvotePost.Mapper upvotePostFieldMapper = new UpvotePost.Mapper();

      final Field[] fields = {
        Field.forObject("upvotePost", "upvotePost", new UnmodifiableMapBuilder<String, Object>(1)
          .put("postId", new UnmodifiableMapBuilder<String, Object>(2)
            .put("kind", "Variable")
            .put("variableName", "postId")
          .build())
        .build(), true, new Field.ObjectReader<UpvotePost>() {
          @Override public UpvotePost read(final ResponseReader reader) throws IOException {
            return upvotePostFieldMapper.map(reader);
          }
        })
      };

      @Override
      public Data map(ResponseReader reader) throws IOException {
        final UpvotePost upvotePost = reader.read(fields[0]);
        return new Data(upvotePost);
      }
    }
  }
}
