package org.openstructures.flow;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openstructures.flow.ValueNode.node;

public class PushRelabelMaxFlowTest {
    private PushRelabelMaxFlow pushRelabelMaxFlow;
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
        flowNetwork.setArcCapacity(2, nodeB, nodeC);
        flowNetwork.setArcCapacity(3, nodeC, nodeD);
        flowNetwork.setArcCapacity(2, nodeC, sink);
        flowNetwork.setArcCapacity(1, nodeD, nodeB);
        flowNetwork.setArcCapacity(3, nodeD, sink);
        pushRelabelMaxFlow = new PushRelabelMaxFlow(flowNetwork);
    }

    @Test
    public void shouldPushFlowFromSource() {
        // when
        pushRelabelMaxFlow.pushFlow(1, source, nodeB);

        // then
        assertThat(flowNetwork.getArcCapacity(source, nodeB)).isEqualTo(1);
        assertThat(flowNetwork.getArcCapacity(nodeB, source)).isEqualTo(1);

        // and
        assertThat(pushRelabelMaxFlow.getNodeExcess(nodeB)).isEqualTo(1);
    }

    @Test
    public void shouldPushFlowFromIntermediateNodes() {
        // when
        pushRelabelMaxFlow.pushFlow(3, source, nodeA);
        pushRelabelMaxFlow.pushFlow(2, nodeA, nodeD);
        pushRelabelMaxFlow.pushFlow(1, nodeD, sink);

        // then
        assertThat(flowNetwork.getArcCapacity(source, nodeA)).isZero();
        assertThat(flowNetwork.getArcCapacity(nodeA, source)).isEqualTo(3);

        assertThat(flowNetwork.getArcCapacity(nodeA, nodeD)).isZero();
        assertThat(flowNetwork.getArcCapacity(nodeD, nodeA)).isEqualTo(2);

        assertThat(flowNetwork.getArcCapacity(nodeD, sink)).isEqualTo(2);
        assertThat(flowNetwork.getArcCapacity(sink, nodeD)).isEqualTo(1);

        // and
        assertThat(pushRelabelMaxFlow.getNodeExcess(nodeA)).isEqualTo(1);
        assertThat(pushRelabelMaxFlow.getNodeExcess(nodeD)).isEqualTo(1);
        assertThat(pushRelabelMaxFlow.getNodeExcess(sink)).isEqualTo(1);
    }

    @Test
    public void shouldGetZeroFlowAmount() {
        // when and then
        assertThat(pushRelabelMaxFlow.getFlowAmount()).isZero();
    }

    @Test
    public void shouldGetFlowAmount() {
        // given
        pushRelabelMaxFlow.pushFlow(2, source, nodeA);
        pushRelabelMaxFlow.pushFlow(2, nodeA, nodeD);
        pushRelabelMaxFlow.pushFlow(2, nodeD, sink);

        pushRelabelMaxFlow.pushFlow(1, source, nodeB);
        pushRelabelMaxFlow.pushFlow(1, nodeB, nodeC);
        pushRelabelMaxFlow.pushFlow(1, nodeC, sink);

        // when and then
        assertThat(pushRelabelMaxFlow.getFlowAmount()).isEqualTo(3);
    }

    /**
     * Should find maximum flow.
     * The example for this test is taken from chapter 7.6 of Network Flows Theory, Algorithms, and Applications by Ravindra K. Ahuja
     */
    @Test
    public void shouldFindMaximFlow() {
        // given
        FlowNetwork flowNetwork = new FlowNetwork(node(1), node(4));
        flowNetwork.setArcCapacity(2, node(1), node(2));
        flowNetwork.setArcCapacity(4, node(1), node(3));
        flowNetwork.setArcCapacity(3, node(2), node(3));
        flowNetwork.setArcCapacity(5, node(3), node(4));
        flowNetwork.setArcCapacity(1, node(2), node(4));
        PushRelabelMaxFlow flow = new PushRelabelMaxFlow(flowNetwork);

        // when
        flow.preflowPush();

        // then
        assertThat(flow.getFlowAmount()).isEqualTo(6);
    }

    /**
     * More complex example
     */
    @Test
    public void shouldFindMaximumFlow2() {
        // given
        FlowNetwork flowNetwork = new FlowNetwork(source, sink);
        flowNetwork.setArcCapacity(200, source, nodeA);
        flowNetwork.setArcCapacity(1, source, nodeB);
        flowNetwork.setArcCapacity(10, nodeA, nodeD);
        flowNetwork.setArcCapacity(2, nodeA, nodeC);
        flowNetwork.setArcCapacity(10, nodeB, sink);
        flowNetwork.setArcCapacity(6, nodeC, nodeB);
        flowNetwork.setArcCapacity(5, nodeD, nodeC);
        flowNetwork.setArcCapacity(2, nodeD, sink);
        PushRelabelMaxFlow flow = new PushRelabelMaxFlow(flowNetwork);

        // when
        flow.preflowPush();

        // then
        assertThat(flow.getFlowAmount()).isEqualTo(9);
    }

    @Test
    public void shouldPreprocess() {
        // when
        pushRelabelMaxFlow.preprocess();

        // then
        assertThat(pushRelabelMaxFlow.getArcCapacity(source, nodeA)).isZero();
        assertThat(pushRelabelMaxFlow.getArcCapacity(source, nodeB)).isZero();
        assertThat(pushRelabelMaxFlow.getNodeExcess(nodeA)).isEqualTo(3);
        assertThat(pushRelabelMaxFlow.getNodeExcess(nodeB)).isEqualTo(2);
        assertThat(pushRelabelMaxFlow.getArcCapacity(nodeA, source)).isEqualTo(3);
        assertThat(pushRelabelMaxFlow.getArcCapacity(nodeB, source)).isEqualTo(2);

        // and
        assertThat(pushRelabelMaxFlow.getNodeDistance(sink)).isZero();
        assertThat(pushRelabelMaxFlow.getNodeDistance(source)).isEqualTo(6);
        assertThat(pushRelabelMaxFlow.getNodeDistance(nodeC)).isEqualTo(1);
        assertThat(pushRelabelMaxFlow.getNodeDistance(nodeD)).isEqualTo(1);
        assertThat(pushRelabelMaxFlow.getNodeDistance(nodeA)).isEqualTo(2);
        assertThat(pushRelabelMaxFlow.getNodeDistance(nodeB)).isEqualTo(2);
    }

    /**
     * Should calculate and re-calculate nodes' distances and flood all nodes adjacent to the source.
     */
    @Test
    public void shouldPreprocessMoreThanOnce() {
        // given
        pushRelabelMaxFlow.preprocess();
        pushRelabelMaxFlow.pushFlow(2, nodeB, nodeC);
        pushRelabelMaxFlow.pushFlow(2, nodeC, sink);

        // when
        pushRelabelMaxFlow.preprocess();

        // then
        assertThat(pushRelabelMaxFlow.getNodeDistance(sink)).isZero();
        assertThat(pushRelabelMaxFlow.getNodeDistance(source)).isEqualTo(6);
        assertThat(pushRelabelMaxFlow.getNodeDistance(nodeC)).isEqualTo(2);
        assertThat(pushRelabelMaxFlow.getNodeDistance(nodeD)).isEqualTo(1);
        assertThat(pushRelabelMaxFlow.getNodeDistance(nodeA)).isEqualTo(2);
        assertThat(pushRelabelMaxFlow.getNodeDistance(nodeB)).isEqualTo(3);

    }

    @Test
    public void shouldGetActiveNodes() {
        // given
        pushRelabelMaxFlow.pushFlow(2, source, nodeA);
        pushRelabelMaxFlow.pushFlow(1, nodeA, nodeD);
        pushRelabelMaxFlow.pushFlow(1, nodeD, sink);
        pushRelabelMaxFlow.pushFlow(1, source, nodeB);

        // when and then
        assertThat(pushRelabelMaxFlow.getActiveNodes()).hasSize(2).contains(nodeA, nodeB);
    }

    @Test
    public void shouldPushExcessBackToSource() {
        // given
        FlowNetwork flowNetwork = new FlowNetwork(source, sink);
        flowNetwork.setArcCapacity(1, source, nodeA);
        flowNetwork.setArcCapacity(1, source, nodeC);
        flowNetwork.setArcCapacity(1, nodeA, nodeB);
        flowNetwork.setArcCapacity(1, nodeC, nodeB);
        flowNetwork.setArcCapacity(1, nodeB, sink);

        pushRelabelMaxFlow = new PushRelabelMaxFlow(flowNetwork);

        pushRelabelMaxFlow.pushFlow(1, source, nodeA);
        pushRelabelMaxFlow.pushFlow(1, nodeA, nodeB);
        pushRelabelMaxFlow.pushFlow(1, nodeB, sink);

        // when
        pushRelabelMaxFlow.preflowPush();

        // then
        assertThat(pushRelabelMaxFlow.getFlowAmount()).isEqualTo(1);
        assertThat(pushRelabelMaxFlow.getArcCapacity(nodeA, source)).isEqualTo(1);
        assertThat(pushRelabelMaxFlow.getArcCapacity(source, nodeC)).isEqualTo(1);
    }
}
