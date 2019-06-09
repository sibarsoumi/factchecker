package de.upb.cs.dice.factchecker;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.util.FileManager;
/*
import org.eclipse.rdf4j.model.impl.SimpleLiteral;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
*/
import java.io.*;

/**
 * This class takes the test data {@link args[0]} and the knowledge base {@link args[1]} as inputs, computes the truth values of the given data and stores the results in turtle format {@link args[2]}
 * @author Sibar
 *
 */
public class App
{
	  /**
	   * 
	   * @param args[0] path of the input file of test data.
	   * @param args[1] path of the TDB directory of the knowledge base graph (the output of {@link TDBGenerator})
	   * @param args[2] path of the output file of results.
	   * @throws IOException
	   * @throws InterruptedException
	   */
    public static void main( String[] args ) throws IOException
    {
    	// Create in-memory models for the input and output	
        Model model_in = ModelFactory.createDefaultModel();
        Model model_out = ModelFactory.createDefaultModel();
       
        // Initializing input and output streams to read and write the models
        InputStream in = FileManager.get().open( args[0] );
        FileOutputStream out = new FileOutputStream(args[2]);   
        
        // Read model from file
        model_in.read(in, "", "N-TRIPLE");
        
        // Begin reading transaction with the knowledge base
        Dataset	dataset = TDBFactory.createDataset(args[1]) ;
        dataset.begin(ReadWrite.READ) ;
        
        // Initialize an iterator to iterate over the statements of the read model
        StmtIterator iter = model_in.listStatements();

       // Start iterating over the statements of the read model
        while (iter.hasNext())
        {
        	// Get the next statement and its subject
            Statement stmt  = iter.nextStatement();
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
            	String subject=model_in.getProperty(sub,new PropertyImpl("http://www.w3.org/1999/02/22-rdf-syntax-ns#subject")).getObject().toString();
            	String predicate=model_in.getProperty(sub,new PropertyImpl("http://www.w3.org/1999/02/22-rdf-syntax-ns#predicate")).getObject().toString();
            	String object=model_in.getProperty(sub,new PropertyImpl("http://www.w3.org/1999/02/22-rdf-syntax-ns#object")).getObject().toString();
         
       	
            	// Start computing the truth value of the fact
            	float truthValue = 0f;
 			
    			QueryExecution qexec = QueryExecutionFactory.create("ASK {<"+subject+"> <"+predicate+"> <"+object+">.}",dataset);
    			
    			// If fact is in the knowledge graph, set truth value = 1
            	if (qexec.execAsk())
            	{	truthValue=1f; 	qexec.close();	}
            	// If fact is not in the knowledge graph, then:
            	else
            	{
            	// Look in the knowledge base for other subjects and objects that are linked via the given predicate, look for predicates other than the given one that link them and check how many of these predicates link also the subject and object of our statement being studied.
            	float expected=0f, expectedAndExisting=0f;
      	       
            			// Checking in direction: subject predicate object
            	String queryCountOfOtherExpectedPredicates ="SELECT (COUNT (DISTINCT ?otherExpectedPredicate) AS ?countOfExpected) \r\n" +
            			"WHERE { \r\n" +
            			"?anotherSubject <"+predicate+"> ?anotherObject .\r\n"+
            			"?anotherSubject ?otherExpectedPredicate ?anotherObject.\r\n"+
            			"FILTER(?otherExpectedPredicate != <"+predicate+">).\r\n"+
               			"} ";
            	
               	String queryCountOfExpectedAndExistingPredicates ="SELECT (COUNT (DISTINCT ?otherExpectedPredicate) AS ?countOfExpectedAndExisting) \r\n" +
            			"WHERE { \r\n" +
            			"?anotherSubject <"+predicate+"> ?anotherObject .\r\n"+
            			"?anotherSubject ?otherExpectedPredicate ?anotherObject.\r\n"+
            			"FILTER(?otherExpectedPredicate != <"+predicate+">).\r\n"+
            			"<"+subject+"> ?otherExpectedPredicate <"+object+">.\r\n"+
               			"} ";
            	 			
    			qexec = QueryExecutionFactory.create(queryCountOfExpectedAndExistingPredicates,dataset);
                     	
    			ResultSet results = qexec.execSelect() ;
    			while (results.hasNext() )
            	  expectedAndExisting=results.nextSolution().getLiteral("?countOfExpectedAndExisting").getInt();
    			qexec.close();
    						
    			qexec = QueryExecutionFactory.create(queryCountOfOtherExpectedPredicates,dataset);
    			      	
    			results = qexec.execSelect() ;
    			while (results.hasNext() )
            		expected=results.nextSolution().getLiteral("?countOfExpected").getInt();
    			qexec.close();
    			
    			
            		// Checking the other direction (object predicate subject)
            	queryCountOfOtherExpectedPredicates ="SELECT (COUNT (DISTINCT ?otherExpectedPredicate) AS ?countOfExpected) \r\n" +
            			"WHERE { \r\n" +
            			"?anotherSubject <"+predicate+"> ?anotherObject .\r\n"+
            			"?anotherObject ?otherExpectedPredicate ?anotherSubject.\r\n"+
               			"} ";
            	
               	queryCountOfExpectedAndExistingPredicates ="SELECT (COUNT (DISTINCT ?otherExpectedPredicate) AS ?countOfExpectedAndExisting) \r\n" +
            			"WHERE { \r\n" +
            			"?anotherSubject <"+predicate+"> ?anotherObject .\r\n"+
            			"?anotherObject ?otherExpectedPredicate ?anotherSubject.\r\n"+
            			"<"+object+"> ?otherExpectedPredicate <"+subject+">.\r\n"+
               			"} ";
            	
      
               	qexec = QueryExecutionFactory.create(queryCountOfExpectedAndExistingPredicates,dataset);
               	 
    			results = qexec.execSelect() ;
    			while (results.hasNext() )
            	  expectedAndExisting+=results.nextSolution().getLiteral("?countOfExpectedAndExisting").getInt();
    			qexec.close();
    			
    			qexec = QueryExecutionFactory.create(queryCountOfOtherExpectedPredicates,dataset);
            		
    			results = qexec.execSelect() ;
    			while (results.hasNext() )
            		expected+=results.nextSolution().getLiteral("?countOfExpected").getInt();
    			qexec.close();
            	
            	
    			// Compute the ratio and normalize it to the range [-1,+1] with some tolerance
    			truthValue=(expected==0)?-1f:2.7f*(expectedAndExisting/expected-1f)+1.7f;	
    			truthValue=(truthValue>1)?1.0f:truthValue;
    			truthValue=(truthValue<-1)?-1.0f:truthValue;
            	}
            	
            	// Write the result (i.e. the fact-Id with its corresponding computed truth value) to the in-memory output model
        		model_out.add(sub,new PropertyImpl("http://swc2017.aksw.org/hasTruthValue"),model_out.createTypedLiteral(String.valueOf(truthValue),XSDDatatype.XSDfloat));  	
            }
        }
       
        // Write the in-memory output model to the result file and close
        model_out.write(out,"N3");
        out.close();
        dataset.end() ;
    }
}
