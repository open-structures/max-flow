package org.openstructures.flow;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ValueNodeTest {

    @Test
    public void shouldReturnTrueIfValuesAreEqual() {
        // given
        ValueNode<String> node1 = ValueNode.node("value");
        ValueNode<String> node2 = ValueNode.node("value");

        // when and then
        assertThat(node1.equals(node2)).isTrue();
    }

    @Test
    public void shouldReturnFalseIfValuesAreNotEqual() {
        // given
        ValueNode<String> node1 = ValueNode.node("value");
        ValueNode<String> node2 = ValueNode.node("different value");

        // when and then
        assertThat(node1.equals(node2)).isFalse();
    }

    @Test
    public void shouldReturnFalseIfComparingWithNull() {
        // given
        ValueNode<String> someNode = ValueNode.node("value");

        // when and then
        assertThat(someNode.equals(null)).isFalse();
    }

    @Test
    public void shouldReturnHashCodeOfTheValue() {
        // given
        String value = "value";
        ValueNode<String> node = ValueNode.node(value);

        // when and then
        assertThat(node.hashCode()).isEqualTo(value.hashCode());
    }
}
