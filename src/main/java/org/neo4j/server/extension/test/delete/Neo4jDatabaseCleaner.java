package org.neo4j.server.extension.test.delete;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.kernel.AbstractGraphDatabase;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author mh
 * @since 02.03.11
 */
public class Neo4jDatabaseCleaner {
    private AbstractGraphDatabase graph;

    public Neo4jDatabaseCleaner(AbstractGraphDatabase graph) {
        this.graph = graph;
    }

    public Map<String, Object> cleanDb() {
        return cleanDb(Long.MAX_VALUE);
    }
    public Map<String, Object> cleanDb(long maxNodesToDelete) {
        Map<String, Object> result = new HashMap<String, Object>();
        Transaction tx = graph.beginTx();
        try {
            clearIndex(result);
            removeNodes(result,maxNodesToDelete);
            tx.success();
        } finally {
            tx.finish();
        }
        return result;
    }

    private void removeNodes(Map<String, Object> result, long maxNodesToDelete) {
        Node refNode = graph.getReferenceNode();
        long nodes = 0, relationships = 0;
        for (Node node : graph.getAllNodes()) {
            for (Relationship rel : node.getRelationships()) {
                rel.delete();
                relationships++;
            }
            if (!refNode.equals(node)) {
                node.delete();
                nodes++;
            }
            if (nodes >= maxNodesToDelete) break;
        }
        result.put("maxNodesToDelete", maxNodesToDelete);
        result.put("nodes", nodes);
        result.put("relationships", relationships);

    }

    private void clearIndex(Map<String, Object> result) {
        IndexManager indexManager = graph.index();
        result.put("node-indexes", Arrays.asList(indexManager.nodeIndexNames()));
        result.put("relationship-indexes", Arrays.asList(indexManager.relationshipIndexNames()));
        for (String ix : indexManager.nodeIndexNames()) {
            indexManager.forNodes(ix).delete();
        }
        for (String ix : indexManager.relationshipIndexNames()) {
            indexManager.forRelationships(ix).delete();
        }
    }
}
