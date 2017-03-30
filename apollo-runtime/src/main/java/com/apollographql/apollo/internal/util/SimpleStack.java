package com.apollographql.apollo.internal.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple stack data structure which accepts null elements. Backed by list.
 * @param <E>
 */
public class SimpleStack<E> {

  private List<E> backing;

  public SimpleStack() {
    backing = new ArrayList<>();
  }

  public SimpleStack(int initialSize) {
    backing = new ArrayList<>(initialSize);
  }

  public void push(E element) {
    backing.add(element);
  }

  public E pop() {
    if (isEmpty()) {
      throw new IllegalStateException("Stack is empty.");
    }
    return backing.remove(backing.size() - 1);
  }

  public boolean isEmpty() {
    return backing.isEmpty();
  }
}
