/*
 *  Copyright (C) 2016
 *  This file is part of GRASSMARLIN.
 */
package core.document.graph;

import core.document.serialization.xml.XmlElement;
import core.importmodule.ImportItem;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipOutputStream;

/**
 * A bi-directional connection between two nodes.
 * .hashCode() and .equals(MeshEdge) must agree for use as a HashMap key.
 * Comparable needed for use with the MeshVisualization.
 */
public class MeshEdge extends AbstractBidirectionalEdge<MeshNode> {
    public MeshEdge(MeshNode source, MeshNode destination) {
        super(source, destination);
    }

    @Override
    public XmlElement toXml(List<MeshNode> nodes, List<ImportItem> items, ZipOutputStream zos) throws IOException{

        zos.write(super.toXml(nodes, items, zos).toString().getBytes(StandardCharsets.UTF_8));

        return null;
    }
}
