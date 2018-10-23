package com.example.subscriptions;

import com.apollographql.apollo.api.InputFieldMarshaller;
import com.apollographql.apollo.api.InputFieldWriter;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ResponseFieldMarshaller;
import com.apollographql.apollo.api.ResponseReader;
import com.apollographql.apollo.api.ResponseWriter;
import com.apollographql.apollo.api.Subscription;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.api.internal.UnmodifiableMapBuilder;
import com.apollographql.apollo.api.internal.Utils;
import java.io.IOException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Generated;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Generated("Apollo GraphQL")
public final class TestSubscription implements Subscription<TestSubscription.Data, Optional<TestSubscription.Data>, TestSubscription.Variables> {
  public static final String OPERATION_DEFINITION = "subscription TestSubscription($repo: String!) {\n"
      + "  commentAdded(repoFullName: $repo) {\n"
      + "    __typename\n"
      + "    id\n"
      + "    content\n"
      + "  }\n"
      + "}";

  public static final String OPERATION_ID = "8f1972cf9af58c4659da0ae72d02b97faf5fa6e6b794070d2cbcb034e2881fb8";

  public static final String QUERY_DOCUMENT = OPERATION_DEFINITION;

  public static final OperationName OPERATION_NAME = new OperationName() {
    @Override
    public String name() {
      return "TestSubscription";
    }
  };

  private final TestSubscription.Variables variables;

  public TestSubscription(@NotNull String repo) {
    Utils.checkNotNull(repo, "repo == null");
    variables = new TestSubscription.Variables(repo);
  }

  @Override
  public String operationId() {
    return OPERATION_ID;
  }

  @Override
  public String queryDocument() {
    return QUERY_DOCUMENT;
  }

  @Override
  public Optional<TestSubscription.Data> wrapData(TestSubscription.Data data) {
    return Optional.fromNullable(data);
  }

  @Override
  public TestSubscription.Variables variables() {
    return variables;
  }

  @Override
  public ResponseFieldMapper<TestSubscription.Data> responseFieldMapper() {
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
    private @NotNull String repo;

    Builder() {
    }

    public Builder repo(@NotNull String repo) {
      this.repo = repo;
      return this;
    }

    public TestSubscription build() {
      Utils.checkNotNull(repo, "repo == null");
      return new TestSubscription(repo);
    }
  }

  public static final class Variables extends Operation.Variables {
    private final @NotNull String repo;

    private final transient Map<String, Object> valueMap = new LinkedHashMap<>();

    Variables(@NotNull String repo) {
      this.repo = repo;
      this.valueMap.put("repo", repo);
    }

    public @NotNull String repo() {
      return repo;
    }

    @Override
    public Map<String, Object> valueMap() {
      return Collections.unmodifiableMap(valueMap);
    }

    @Override
    public InputFieldMarshaller marshaller() {
      return new InputFieldMarshaller() {
        @Override
        public void marshal(InputFieldWriter writer) throws IOException {
          writer.writeString("repo", repo);
        }
      };
    }
  }

  public static class Data implements Operation.Data {
    static final ResponseField[] $responseFields = {
      ResponseField.forObject("commentAdded", "commentAdded", new UnmodifiableMapBuilder<String, Object>(1)
      .put("repoFullName", new UnmodifiableMapBuilder<String, Object>(2)
        .put("kind", "Variable")
        .put("variableName", "repo")
        .build())
      .build(), true, Collections.<ResponseField.Condition>emptyList())
    };

    final Optional<CommentAdded> commentAdded;

    private transient volatile String $toString;

    private transient volatile int $hashCode;

    private transient volatile boolean $hashCodeMemoized;

    public Data(@Nullable CommentAdded commentAdded) {
      this.commentAdded = Optional.fromNullable(commentAdded);
    }

    /**
     * Subscription fires on every comment added
     */
    public Optional<CommentAdded> commentAdded() {
      return this.commentAdded;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeObject($responseFields[0], commentAdded.isPresent() ? commentAdded.get().marshaller() : null);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Data{"
          + "commentAdded=" + commentAdded
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
        return this.commentAdded.equals(that.commentAdded);
      }
      return false;
    }

    @Override
    public int hashCode() {
      if (!$hashCodeMemoized) {
        int h = 1;
        h *= 1000003;
        h ^= commentAdded.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<Data> {
      final CommentAdded.Mapper commentAddedFieldMapper = new CommentAdded.Mapper();

      @Override
      public Data map(ResponseReader reader) {
        final CommentAdded commentAdded = reader.readObject($responseFields[0], new ResponseReader.ObjectReader<CommentAdded>() {
          @Override
          public CommentAdded read(ResponseReader reader) {
            return commentAddedFieldMapper.map(reader);
          }
        });
        return new Data(commentAdded);
      }
    }
  }

  public static class CommentAdded {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forInt("id", "id", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("content", "content", null, false, Collections.<ResponseField.Condition>emptyList())
    };

    final @NotNull String __typename;

    final int id;

    final @NotNull String content;

    private transient volatile String $toString;

    private transient volatile int $hashCode;

    private transient volatile boolean $hashCodeMemoized;

    public CommentAdded(@NotNull String __typename, int id, @NotNull String content) {
      this.__typename = Utils.checkNotNull(__typename, "__typename == null");
      this.id = id;
      this.content = Utils.checkNotNull(content, "content == null");
    }

    public @NotNull String __typename() {
      return this.__typename;
    }

    /**
     * The SQL ID of this entry
     */
    public int id() {
      return this.id;
    }

    /**
     * The text of the comment
     */
    public @NotNull String content() {
      return this.content;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          writer.writeInt($responseFields[1], id);
          writer.writeString($responseFields[2], content);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "CommentAdded{"
          + "__typename=" + __typename + ", "
          + "id=" + id + ", "
          + "content=" + content
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof CommentAdded) {
        CommentAdded that = (CommentAdded) o;
        return this.__typename.equals(that.__typename)
         && this.id == that.id
         && this.content.equals(that.content);
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
        h ^= id;
        h *= 1000003;
        h ^= content.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<CommentAdded> {
      @Override
      public CommentAdded map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final int id = reader.readInt($responseFields[1]);
        final String content = reader.readString($responseFields[2]);
        return new CommentAdded(__typename, id, content);
      }
    }
  }
}
