package org.openstructures.flow;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openstructures.flow.ValueNode.node;

public class FlowNetworkTest {
    private final Node source = node("source");
    private final Node sink = node("sink");
    private final Node nodeA = node("A");
    private final Node nodeB = node("B");
    private final Node nodeC = node("C");
    private final Node nodeD = node("D");

    private FlowNetwork flowNetwork;

    @Before
    public void setUp() {
        flowNetwork = new FlowNetwork(source, sink);
        flowNetwork.setArcCapacity(3, source, nodeA);
        flowNetwork.setArcCapacity(2, source, nodeB);
        flowNetwork.setArcCapacity(2, nodeA, nodeD);
        flowNetwork.setArcCapacity(3, nodeB, nodeA);
        flowNetwork.setArcCapacity(3, nodeB, nodeC);
        flowNetwork.setArcCapacity(3, nodeC, nodeD);
        flowNetwork.setArcCapacity(2, nodeC, sink);
        flowNetwork.setArcCapacity(1, nodeD, nodeB);
        flowNetwork.setArcCapacity(3, nodeD, sink);
    }

    @Test
    public void shouldGetSource() {
        // when and then
        assertThat(flowNetwork.getSource()).isEqualTo(source);
    }

    @Test
    public void shouldGetSink() {
        // when and then
        assertThat(flowNetwork.getSink()).isEqualTo(sink);
    }

    @Test
    public void shouldGetPredecessors() {
        // when and then
        assertThat(flowNetwork.getPredecessors(source)).isEmpty();
        assertThat(flowNetwork.getPredecessors(nodeC)).hasSize(1).contains(nodeB);
        assertThat(flowNetwork.getPredecessors(nodeD)).hasSize(2).contains(nodeA, nodeC);
        assertThat(flowNetwork.getPredecessors(sink)).hasSize(2).contains(nodeC, nodeD);
    }

    @Test
    public void shouldReturnEmptyPredecessorsIfNodeDoesNotBelongToNetwork() {
        // when and then
        assertThat(flowNetwork.getPredecessors(node("nodeF"))).isEmpty();
    }

    @Test
    public void shouldGetSuccessors() {
        // when and then
        assertThat(flowNetwork.getSuccessors(source)).hasSize(2).contains(nodeA, nodeB);
        assertThat(flowNetwork.getSuccessors(nodeC)).hasSize(2).contains(nodeD, sink);
        assertThat(flowNetwork.getSuccessors(nodeD)).hasSize(2).contains(nodeB, sink);
        assertThat(flowNetwork.getSuccessors(sink)).isEmpty();
    }

    @Test
    public void shouldReturnEmptySuccessorsIfNodeDoesNotBelongToNetwork() {
        // when and then
        assertThat(flowNetwork.getSuccessors(node("nodeF"))).isEmpty();
    }

    @Test
    public void shouldGetNumberOfNodes() {
        // when and then
        assertThat(flowNetwork.getNumberOfNodes()).isEqualTo(6);
    }

    @Test
    public void shouldGetArcCapacity() {
        // when and then
        assertThat(flowNetwork.getArcCapacity(sink, source)).isZero();
        assertThat(flowNetwork.getArcCapacity(source, nodeA)).isEqualTo(3);
        assertThat(flowNetwork.getArcCapacity(nodeB, nodeC)).isEqualTo(3);
        assertThat(flowNetwork.getArcCapacity(nodeB, nodeD)).isZero();
        assertThat(flowNetwork.getArcCapacity(nodeC, sink)).isEqualTo(2);
        assertThat(flowNetwork.getArcCapacity(nodeD, nodeB)).isEqualTo(1);
    }

    @Test
    public void shouldReturnZeroIfNodeDoesNotBelongToNetwork() {
        // when and then
        assertThat(flowNetwork.getArcCapacity(node("nodeF"), sink)).isZero();
    }

    @Test
    public void shouldNotReturnSuccessorsWithArcCapacityZero() {
        // given
        flowNetwork.setArcCapacity(0, nodeD, nodeB);
        flowNetwork.setArcCapacity(0, nodeC, sink);

        // when and then
        assertThat(flowNetwork.getSuccessors(nodeD)).hasSize(1).contains(sink);
        assertThat(flowNetwork.getSuccessors(nodeC)).hasSize(1).contains(nodeD);
    }

    @Test
    public void shouldIncreaseArcCapacity() {
        // given
        flowNetwork.increaseArcCapacity(2, source, nodeA);
        flowNetwork.increaseArcCapacity(1, nodeA, nodeC);

        // when and then
        assertThat(flowNetwork.getArcCapacity(source, nodeA)).isEqualTo(5);
        assertThat(flowNetwork.getArcCapacity(nodeA, nodeC)).isEqualTo(1);
    }

    @Test
    public void shouldGetAndRestoreState() {
        // when
        Node newNode = node("newNode");
        FlowNetwork.State memento = flowNetwork.getState();
        flowNetwork.setArcCapacity(100, source, nodeA);
        flowNetwork.setArcCapacity(1, nodeA, nodeC);
        flowNetwork.setArcCapacity(1, newNode, sink);
        flowNetwork.restore(memento);

        // then
        assertThat(flowNetwork.getArcCapacity(source, nodeA)).isEqualTo(3);
        assertThat(flowNetwork.getArcCapacity(nodeA, nodeC)).isZero();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfTryingToRestoreWithStateFromAnotherNetwork() {
        // given
        Node s = node("s");
        Node t = node("t");
        Node nodeI = node("I");
        FlowNetwork anotherFlowNetwork = new FlowNetwork(s, t);
        anotherFlowNetwork.setArcCapacity(10, s, nodeI);
        anotherFlowNetwork.setArcCapacity(10, nodeI, t);
        FlowNetwork.State anotherNetworkState = anotherFlowNetwork.getState();

        // when
        flowNetwork.restore(anotherNetworkState);

        // then expect exception
    }
}
