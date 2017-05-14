package com.apollographql.apollo;

import android.os.Looper;

import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ResponseReader;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Handler;

import javax.annotation.Nonnull;

public final class TestUtils {
  public static final Query EMPTY_QUERY = new Query() {
    @Override public String queryDocument() {
      return "";
    }

    @Override public Variables variables() {
      return EMPTY_VARIABLES;
    }

    @Override public ResponseFieldMapper<Data> responseFieldMapper() {
      return new ResponseFieldMapper<Data>() {
        @Override public Data map(ResponseReader responseReader) throws IOException {
          return null;
        }
      };
    }

    @Override public Object wrapData(Data data) {
      return data;
    }

    @Nonnull @Override public OperationName name() {
      return null;
    }
  };

  public static Looper createBackgroundLooper() throws Exception {
    final AtomicReference<Looper> looperRef = new AtomicReference<>();
    new Thread() {
      @Override public void run() {
        Looper.prepare();
        synchronized (this) {
          looperRef.set(Looper.myLooper());
          notifyAll();
        }
        Looper.loop();
      }
    }.start();
    Thread.sleep(200);
    return looperRef.get();
  }

  private TestUtils() {
  }
}
