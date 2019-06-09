package de.upb.cs.dice.sw9000_factchecker;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.apache.jena.util.FileManager;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import javax.xml.bind.DatatypeConverter;

/**
 * This class takes the train data {@link args[0]}, test data {@link args[1]} and the knowledge base {@link args[2]} as inputs, computes the truth values of the given test data and stores the results in turtle format {@link args[3]}
 * @author Sibar
 *
 */

public class App2
{
	  /**
	   * 
	   * @param args[0] path of the input file of train data.
	   * @param args[1] path of the input file of test data.
	   * @param args[2] path of the input file of knowledge base graph.
	   * @param args[3] path of the output file of results.
	   * @throws IOException
	   * @throws InterruptedException
	   * @throws NoSuchAlgorithmException
	   */
	  
    public static void main( String[] args ) throws IOException, InterruptedException, NoSuchAlgorithmException
    {
    	// Create in-memory models for the input and output	
        Model model_in_train = ModelFactory.createDefaultModel();
        Model model_in_test = ModelFactory.createDefaultModel();
        Model model_out = ModelFactory.createDefaultModel();
        

        // Initializing input and output streams to read and write the models
        model_in_train.read(FileManager.get().open( args[0] ), "", "N-TRIPLE");
        model_in_test.read(FileManager.get().open( args[1] ), "", "N-TRIPLE");
        FileOutputStream out = new FileOutputStream(args[3]);
        Model dataset =   ModelFactory.createDefaultModel().read(FileManager.get().open( args[2]), "", "TTL");
      
        // MessageDigest to be used later for generate hashes
        MessageDigest md = MessageDigest.getInstance("SHA-256");
             
        // Use the training data to create a trained model
        HashMap<String,LinkedList<String>> trainedModel = new HashMap<String,LinkedList<String>>();
        
        // Query all predicates in the training data and iterate over them
        ResultSet results = QueryExecutionFactory.create("SELECT DISTINCT ?y WHERE {?x <http://www.w3.org/1999/02/22-rdf-syntax-ns#predicate> ?y. }",model_in_train).execSelect() ;
        while (results.hasNext() )
        	{
        	// Look for subjects and objects that are linked through this predicate with statements given as true
        	String predicate_uri=results.nextSolution().get("?y").toString();
        	ResultSet results1 = QueryExecutionFactory.create("SELECT ?s ?o WHERE {?x <http://www.w3.org/1999/02/22-rdf-syntax-ns#predicate> <"+predicate_uri+">. ?x <http://www.w3.org/1999/02/22-rdf-syntax-ns#subject> ?s. ?x <http://www.w3.org/1999/02/22-rdf-syntax-ns#object> ?o. ?x <http://swc2017.aksw.org/hasTruthValue> \"1.0\"^^<http://www.w3.org/2001/XMLSchema#float>. }",model_in_train).execSelect() ;
        	LinkedList<String> existingRelations = new LinkedList<String>();
        	// Iterate over the found pairs of subjects and objects, look for other predicates that link them and store them in the HashMap with "predicate+true" as key
        	while (results1.hasNext() )
        		{ QuerySolution re = results1.next();
        		String subject=re.get("?s").toString(), object=re.get("?o").toString();	
        		ResultSet listOFRelations = QueryExecutionFactory.create("SELECT DISTINCT ?r WHERE {<"+subject+"> ?r <"+object+">. FILTER (?r!=<"+predicate_uri+">). }",dataset).execSelect() ;
        		while (listOFRelations.hasNext() )
        			{String foundRelation = listOFRelations.nextSolution().get("?r").toString();
        			if (!existingRelations.contains(foundRelation))
        			existingRelations.add(foundRelation);}
        		}
        	md.update((predicate_uri+true).getBytes());
        	trainedModel.put(DatatypeConverter.printHexBinary(md.digest()), existingRelations);
        	
        	// Same game again but with statements that are given as not true and with "predicate+false" as key
        	
        	ResultSet results0 = QueryExecutionFactory.create("SELECT ?s ?o WHERE {?x <http://www.w3.org/1999/02/22-rdf-syntax-ns#predicate> <"+predicate_uri+">. ?x <http://www.w3.org/1999/02/22-rdf-syntax-ns#subject> ?s. ?x <http://www.w3.org/1999/02/22-rdf-syntax-ns#object> ?o. ?x <http://swc2017.aksw.org/hasTruthValue> \"0.0\"^^<http://www.w3.org/2001/XMLSchema#float>. }",model_in_train).execSelect() ;
        	existingRelations = new LinkedList<String>();
        	while (results0.hasNext() )
        		{  QuerySolution re = results0.next();
        		String subject=re.get("?s").toString(), object=re.get("?o").toString();
        		ResultSet listOFRelations = QueryExecutionFactory.create("SELECT DISTINCT ?r WHERE {<"+subject+"> ?r <"+object+">. FILTER (?r!=<"+predicate_uri+">). }",dataset).execSelect() ;
        		while (listOFRelations.hasNext() )
        		{String foundRelation = listOFRelations.nextSolution().get("?r").toString();
    			if (!existingRelations.contains(foundRelation))
    			existingRelations.add(foundRelation);}
        		}
        	md.update((predicate_uri+false).getBytes());
        	trainedModel.put(DatatypeConverter.printHexBinary(md.digest()), existingRelations);
        	}
        
        
        // At this point training is finished.
        
        
        // Start iterating over the statements of the test data
        StmtIterator iter = model_in_test.listStatements();
        while (iter.hasNext())
        {
        	// Get the next statement and its subject
            Statement stmt      = iter.nextStatement();
            Resource  sub   = stmt.getSubject();
            
            
            // Make sure that it represents a fact in the format that we expect
            if (sub.toString().matches("^http:\\/\\/swc2017\\.aksw\\.org\\/task2\\/dataset\\/[0-9]+")
            		&&
            		stmt.getPredicate().toString().equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")
            		&&
            		stmt.getObject().toString().equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#Statement")
            	)
            {
            	// Get the subject, predicate and object of the fact
            	String subject=model_in_test.getProperty(sub,new PropertyImpl("http://www.w3.org/1999/02/22-rdf-syntax-ns#subject")).getObject().toString();
            	String predicate=model_in_test.getProperty(sub,new PropertyImpl("http://www.w3.org/1999/02/22-rdf-syntax-ns#predicate")).getObject().toString();
            	String object=model_in_test.getProperty(sub,new PropertyImpl("http://www.w3.org/1999/02/22-rdf-syntax-ns#object")).getObject().toString();
            	
            	// Start computing the truth value of the fact
            	float truthValue = 0f;
            	float countExpectedPos=0f,countExistingPos=0f,countExpectedNeg=0f,countExistingNeg=0f;
            	
            	

            	// If fact is in the knowledge graph, set truth value = 1
            	if (QueryExecutionFactory.create("ASK {<"+subject+"> <"+predicate+"> <"+object+">.}",dataset).execAsk())
            		truthValue=1f;
            	// If fact is not in the knowledge graph, then:
            	else
            	{
            	// Look at the predicates stored in the HashMap under "predicate+true" as key and check how many of them link also the subject and object of our statement being studied
            	md.update((predicate+true).getBytes());
            	LinkedList<String> expected = trainedModel.get(DatatypeConverter.printHexBinary(md.digest()));
            	String wherePart=null;
            	if (expected!=null && expected.size()>=1)
            		{ countExpectedPos=(float)expected.size();
            		wherePart="{ {<"+subject+"> <"+expected.get(0)+"> <"+object+">. } ";	
            		if (expected.size()>1)
            			{	Iterator<String> it = expected.iterator(); it.next();
            				while (it.hasNext())
            					wherePart+=" UNION { <"+subject+"> <"+it.next()+"> <"+object+">. } ";
            			}
            		wherePart+="}";
            		countExistingPos=(float)(QueryExecutionFactory.create("SELECT (COUNT (*) AS ?count) WHERE "+wherePart,dataset).execSelect().next().getLiteral("?count").getInt());
            
            		}
            
            	
            	
            	
            	// Same game again with but with "predicate+false" as key
            	md.update((predicate+false).getBytes());
            	expected = trainedModel.get(DatatypeConverter.printHexBinary(md.digest()));
            	wherePart=null;
            	if (expected!=null && expected.size()>=1)
            		{ countExpectedNeg=(float)expected.size();
            		wherePart="{ {<"+subject+"> <"+expected.get(0)+"> <"+object+">. } ";	
            		if (expected.size()>1)
            			{	Iterator<String> it = expected.iterator(); it.next();
            				while (it.hasNext())
            					wherePart+=" UNION { <"+subject+"> <"+it.next()+"> <"+object+">. } ";
            			}
            		wherePart+="}";
            		countExistingNeg=(float)(QueryExecutionFactory.create("SELECT (COUNT (*) AS ?count) WHERE "+wherePart,dataset).execSelect().next().getLiteral("?count").getInt());
            		}
            	
            	
            	// Compute the ratio of expected to expectedAndExisting for the positive and negative
            	float p=(countExpectedPos==0)?0:countExistingPos/countExpectedPos;
            	float n=(countExpectedNeg==0)?0:countExistingNeg/countExpectedNeg;
            	
            	// Compute the final truth value
            	truthValue=p-n;
            	
            	}
            	
            	// Write the result (i.e. the fact-Id with its corresponding computed truth value) to the in-memory output model
            	model_out.add(sub,new PropertyImpl("http://swc2017.aksw.org/hasTruthValue"),model_out.createTypedLiteral(String.valueOf(truthValue),XSDDatatype.XSDfloat));
            	
            }
        
        
        }
        
        
        // Write the in-memory output model to the result file
        model_out.write(out,"N3");
        out.close();
      
    }
}
