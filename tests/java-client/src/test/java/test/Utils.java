package test;

import com.apollographql.apollo3.api.ApolloResponse;
import com.apollographql.apollo3.api.Mutation;
import com.apollographql.apollo3.api.Query;
import com.apollographql.java.client.ApolloClient;
import com.apollographql.java.rx3.Rx3Apollo;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import org.jetbrains.annotations.NotNull;

public class Utils {
  public static void sleep(int millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @NotNull
  public static <D extends Query.Data> ApolloResponse<D> blockingQuery(ApolloClient apolloClient, Query<D> query) {
    return Rx3Apollo.single(apolloClient.query(query), BackpressureStrategy.BUFFER).blockingGet();
  }

  @NotNull
  public static <D extends Mutation.Data> ApolloResponse<D> blockingMutation(ApolloClient apolloClient, Mutation<D> mutation) {
    return Rx3Apollo.single(apolloClient.mutation(mutation), BackpressureStrategy.BUFFER).blockingGet();
  }

}
