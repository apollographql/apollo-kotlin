package com.apollographql.android.impl;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

interface CallInterceptorChain {
  CallInterceptorChain chain(CallInterceptor callInterceptor);

  CallInterceptor.InterceptorResponse proceed() throws IOException;

  void proceedAsync(ExecutorService dispatcher, CallInterceptor.CallBack callBack);

  void dispose();
}
