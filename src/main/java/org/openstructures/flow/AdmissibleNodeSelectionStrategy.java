package org.openstructures.flow;

import java.util.Optional;

/**
 * Admissible node is a node that is 1 closer to the sink than the current node (n):
 * distance(n) = distance(admissibleNode) + 1
 * Distance function has to satisfy the following condition:
 * distance(sink) = 0 and d(i) &lt;= d(j) + 1 where i,j is the arc that belongs to this network
 * Distance function basically determines the distance of the node from the sink.
 * The first condition states that the sink is 0 away from itself.
 * The second condition states that the node can't be further from the sink than 1 plus the distance from the sink of its adjacent node.
 */
public interface AdmissibleNodeSelectionStrategy {
    Optional<Node> getAdmissibleNode(PushRelabelMaxFlow pushRelabelMaxFlow, Node n);
}
