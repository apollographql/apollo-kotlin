package com.apollographql.apollo3.rx2;

import com.apollographql.apollo3.ApolloCall;
import com.apollographql.apollo3.ApolloPrefetch;
import com.apollographql.apollo3.ApolloQueryWatcher;
import com.apollographql.apollo3.ApolloSubscriptionCall;
import com.apollographql.apollo3.api.Operation;
import com.apollographql.apollo3.api.ApolloResponse;
import com.apollographql.apollo3.api.exception.ApolloException;
import com.apollographql.apollo3.internal.subscription.ApolloSubscriptionTerminatedException;
import com.apollographql.apollo3.internal.util.Cancelable;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.CompletableEmitter;
import io.reactivex.CompletableOnSubscribe;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.annotations.CheckReturnValue;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import org.jetbrains.annotations.NotNull;

import static com.apollographql.apollo3.api.internal.Utils.checkNotNull;

package com.apollographql.apollo3.cache.normalized.internal

import com.apollographql.apollo3.cache.normalized.ApolloStore

class RxApolloStore(private val delegate:ApolloStore) :ApolloStore by delegate{
    fun remove() {

    }
}
