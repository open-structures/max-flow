package org.openstructures.flow;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Objects.requireNonNull;

public class FlowNetwork {

    private final Node source, sink;

    private final Table<Node, Node, Integer> capacitiesTable = HashBasedTable.create();

    public FlowNetwork(Node source, Node sink) {
        this.source = requireNonNull(source);
        this.sink = requireNonNull(sink);
    }

    public Set<Node> getPredecessors(Node head) {
        checkNotNull(head);
        return capacitiesTable.column(head).keySet();
    }

    public Set<Node> getSuccessors(Node tail) {
        checkNotNull(tail);
        return capacitiesTable.row(tail).keySet();
    }

    public int getArcCapacity(Node tail, Node head) {
        checkNotNull(tail);
        checkNotNull(head);
        return capacitiesTable.contains(tail, head) ? capacitiesTable.get(tail, head) : 0;
    }

    public void setArcCapacity(int capacity, Node tail, Node head) {
        checkArgument(capacity >= 0);
        checkNotNull(tail);
        checkNotNull(head);
        if (capacity == 0) {
            if (capacitiesTable.contains(tail, head)) {
                capacitiesTable.remove(tail, head);
            }
        } else {
            capacitiesTable.put(tail, head, capacity);
        }
    }

    public int getNumberOfNodes() {
        Set<Node> allNodes = newHashSet();
        allNodes.addAll(capacitiesTable.rowKeySet());
        allNodes.addAll(capacitiesTable.columnKeySet());
        allNodes.add(source);
        allNodes.add(sink);
        return allNodes.size();
    }

    public void increaseArcCapacity(int capacityToAdd, Node tail, Node head) {
        checkNotNull(tail);
        checkNotNull(head);
        checkArgument(capacityToAdd > 0);

        int existingCapacity = getArcCapacity(tail, head);
        setArcCapacity(existingCapacity + capacityToAdd, tail, head);
    }

    public Node getSink() {
        return sink;
    }

    public Node getSource() {
        return source;
    }
}
