package com.apollographql.android.impl;

import com.apollographql.android.api.graphql.Operation;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

final class RealCallInterceptorChain implements CallInterceptorChain {
  private static final CallInterceptorChain TERMINAL_INTERCEPTOR_CHAIN = new CallInterceptorChain() {
    @Override public CallInterceptorChain chain(CallInterceptor callInterceptor) {
      throw new IllegalStateException("terminal interceptor reached");
    }

    @Override public CallInterceptor.InterceptorResponse proceed() throws IOException {
      throw new IllegalStateException("terminal interceptor reached");
    }

    @Override public void proceedAsync(ExecutorService dispatcher, CallInterceptor.CallBack callBack) {
      throw new IllegalStateException("terminal interceptor reached");
    }

    @Override public void dispose() {
    }
  };

  private final Operation operation;
  private volatile CallInterceptor currentInterceptor;
  private volatile CallInterceptorChain nextInterceptorChain = TERMINAL_INTERCEPTOR_CHAIN;

  RealCallInterceptorChain(Operation operation) {
    this(operation, new CallInterceptor() {
      @Override
      public InterceptorResponse intercept(Operation operation, CallInterceptorChain chain) throws IOException {
        return chain.proceed();
      }

      @Override
      public void interceptAsync(Operation operation, CallInterceptorChain chain, ExecutorService dispatcher,
          CallBack callBack) {
        chain.proceedAsync(dispatcher, callBack);
      }

      @Override public void dispose() {
        // no op
      }
    });
  }

  private RealCallInterceptorChain(Operation operation, CallInterceptor currentInterceptor) {
    this.operation = operation;
    this.currentInterceptor = currentInterceptor;
  }

  @Override public CallInterceptorChain chain(CallInterceptor callInterceptor) {
    RealCallInterceptorChain lasInterceptorChain = this;
    while (lasInterceptorChain.nextInterceptorChain != null
        && lasInterceptorChain.nextInterceptorChain != TERMINAL_INTERCEPTOR_CHAIN) {
      lasInterceptorChain = (RealCallInterceptorChain) lasInterceptorChain.nextInterceptorChain;
    }
    lasInterceptorChain.nextInterceptorChain = new RealCallInterceptorChain(operation, callInterceptor);
    return this;
  }

  @Override public CallInterceptor.InterceptorResponse proceed() throws IOException {
    CallInterceptor currentInterceptor = this.currentInterceptor;
    if (currentInterceptor == null) throw new IllegalStateException();

    CallInterceptorChain nextInterceptorChain = this.nextInterceptorChain;
    if (nextInterceptorChain == null) throw new IllegalStateException();

    return currentInterceptor.intercept(operation, nextInterceptorChain);
  }

  @Override public void proceedAsync(ExecutorService dispatcher, CallInterceptor.CallBack callBack) {
    CallInterceptor currentInterceptor = this.currentInterceptor;
    if (currentInterceptor == null) {
      callBack.onFailure(new IllegalStateException());
      return;
    }

    CallInterceptorChain nextInterceptorChain = this.nextInterceptorChain;
    if (nextInterceptorChain == null) {
      callBack.onFailure(new IllegalStateException());
      return;
    }

    currentInterceptor.interceptAsync(operation, nextInterceptorChain, dispatcher, callBack);
  }

  @Override public void dispose() {
    CallInterceptor currentInterceptor = this.currentInterceptor;
    if (currentInterceptor != null) {
      currentInterceptor.dispose();
    }
    this.currentInterceptor = null;

    CallInterceptorChain nextInterceptorChain = this.nextInterceptorChain;
    if (nextInterceptorChain != null) {
      nextInterceptorChain.dispose();
    }
    this.nextInterceptorChain = null;
  }
}
