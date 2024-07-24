package org.openstructures.flow;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

public class ValueNode<T> implements Node {

  private final T value;

  private ValueNode(T value) {
    this.value = Objects.requireNonNull(value);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ValueNode) {
      return ((ValueNode<?>) obj).getValue().equals(value);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }

  public T getValue() {
    return value;
  }

  public static <T> ValueNode<T> node(T value) {
    checkNotNull(value);
    return new ValueNode<>(value);
  }
}
