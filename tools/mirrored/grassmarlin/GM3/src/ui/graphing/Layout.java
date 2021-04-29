package ui.graphing;

import core.document.graph.IEdge;
import core.document.graph.INode;

public interface Layout<TNode extends INode<TNode>, TEdge extends IEdge<TNode>> {
    void layoutAll(Visualization<TNode, TEdge> visualization);
    void layoutSingle(Visualization<TNode, TEdge> visualization, Cell<TNode> cell, TNode node);
}
