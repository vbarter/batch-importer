package org.neo4j.batchimport;

import org.neo4j.batchimport.importer.RelType;
import org.neo4j.batchimport.importer.RowData;
import org.neo4j.index.lucene.unsafe.batchinsert.LuceneBatchInserterIndexProvider;
import org.neo4j.kernel.impl.util.FileUtils;
import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserters;
import org.neo4j.unsafe.batchinsert.BatchInserterIndexProvider;
import org.neo4j.unsafe.batchinsert.BatchInserterIndex;

import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.kernel.EmbeddedGraphDatabase;

import java.io.*;
import java.util.*;

import static org.neo4j.index.impl.lucene.LuceneIndexImplementation.EXACT_CONFIG;
import static org.neo4j.index.impl.lucene.LuceneIndexImplementation.FULLTEXT_CONFIG;

public class Importer {
    private static Report report;
    private GraphDatabaseService db = null;
    private BatchInserterIndexProvider lucene = null;
    private BatchInserterIndex index = null;
    private BatchInserter inserter = null;
    
    public Importer(String graphDb,String type) {
        Map<String, String> config = Utils.config();
                
	 // START SNIPPET: batchDb
	if (type.equals("node") || type.equals("rel")) {
	    db = BatchInserters.batchDatabase(graphDb);
	} else {
	    inserter = BatchInserters.inserter(graphDb);
	    lucene = new LuceneBatchInserterIndexProvider(inserter);
	}
	report = createReport();
    }

    protected StdOutReport createReport() {
        return new StdOutReport(10 * 1000 * 1000, 100);
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.err.println("Usage java -jar batchimport.jar data/dir type [nodes.csv|relation.csv]");
        } else {
	    String graphDb = args[0];
            String type = args[1];
            Importer importer = new Importer(graphDb,type);
	    System.err.println(type);
            try {
	        if (type.equals("node")) {
		    for (int k=2;k<args.length;k++) {
			System.out.println("begin to build node file : " + args[k]);
		    	File nodesFile = new File(args[k]);
	            	if (nodesFile.exists()) {
                            importer.importNodes(new FileReader(nodesFile));
                    	} else {
                            System.err.println("Nodes file "+nodesFile+" does not exist");
                    	}
		    }
		} else if (type.equals("rel")) {
		    for (int k=2;k<args.length;k++) {
			System.out.println("begin to build relationship file: "+ args[k]);
		    	File relationshipsFile = new File(args[k]);
		    	if (relationshipsFile.exists()) {
                            importer.importRelationships(new FileReader(relationshipsFile));
                    	} else {
                       	    System.err.println("Relationships file "+relationshipsFile+" does not exist");
                    	}
		    }
		} else if (type.equals("nodeindex")) {
		    for (int k=2;k<args.length;k++) {
			System.out.println("begin to build node index file: " + args[k]);
			File nodeIndexFile = new File(args[k]);
		    	if (nodeIndexFile.exists()) {
                            importer.importNodeIndex(args[k]);
                    	} else {
                            System.err.println("nodeIndex file "+nodeIndexFile+" does not exist");
                    	}
		    }
		} else {
		    System.err.println("no match");
		}
	    } finally {
                importer.finish();
            }

	}
    }

    void finish() {
	if (db != null) {
        	db.shutdown();
	}
	if (inserter != null) {
		inserter.shutdown();
	}
	if (lucene != null) {
		lucene.shutdown();
	}
        report.finish();
    }

    void importNodes(Reader reader) throws IOException {
        BufferedReader bf = new BufferedReader(reader);
        String line;
        report.reset();
	int num = 1;
	String[] header = null;
        while ((line = bf.readLine()) != null) {
	    if (num == 1) {
	    	header = line.split("\t");
	    } else {
	    	Node node = db.createNode();
	    	String[] items = line.split("\t");
	    	for (int i=0;i<items.length;i++) {
	    		node.setProperty(header[i],new String(items[i].getBytes(),"UTF-8"));
	    	}
	    }
            report.dots();
	    num++;
        }
        report.finishImport("Nodes");
    }

    void importRelationships(Reader reader) throws IOException {
        BufferedReader bf = new BufferedReader(reader);
        final RowData data = new RowData(bf.readLine(), "\t", 3);
        Object[] rel = new Object[3];
        final RelType relType = new RelType();
        String line;
        report.reset();
	int i = 0;
        while ((line = bf.readLine()) != null) {
	    //System.out.println(line);
	    final Map<String, Object> properties = data.updateMap(line, rel);
	    Node start_node = db.getNodeById(id(rel[0]));
	    Node end_node = db.getNodeById(id(rel[1]));
	    RelationshipType has = DynamicRelationshipType.withName(rel[2].toString());
	    start_node.createRelationshipTo(end_node,has);
	    report.dots();
	    i += 1;
        }
        report.finishImport("Relationships");
    }

    void importNodeIndex(String nodeIndexFile) throws IOException {
	String index_name = nodeIndexFile.split(".csv")[0];
	index = lucene.nodeIndex(index_name, MapUtil.stringMap("type", "exact"));
        BufferedReader bf = new BufferedReader(new FileReader(new File(nodeIndexFile)));
        Object[] rel = new Object[3];
        String line;
        report.reset();
	int num = 1;
	String[] header = null;
        while ((line = bf.readLine()) != null) {
	    if (num == 1) {
	    	header = line.split("\t");
		index.setCacheCapacity(header[1], 500000 );
	    } else {
	    	String[] items = line.split("\t");
		Map<String, Object> properties = MapUtil.map(header[1],items[1]);
		index.add(id(items[0]),properties );
	    }
	    num += 1;
        }
	index.flush();
        report.finishImport("node index");
    }

    private long id(Object id) {
        return Long.parseLong(id.toString());
    }

}
