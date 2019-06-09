package de.upb.cs.dice.sw9000_factchecker;

import java.io.InputStream;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.util.FileManager;
/**
 * This class takes a graph as a file in ttl format {@link args[0]} and converts it to a TDB-backed dataset stored in the wished directory {@link args[1]} so that it can be used by {@link App}
 * @author Sibar
 *
 */

public class TDBGenerator{
	/**
	 * 
	 * @param args[0] path of the input file in ttl format.
	 * @param args[1] path of the output TDB directory.
	 */
public static void main (String [] args)
{
	 Model model_in = ModelFactory.createDefaultModel();
	 InputStream in = FileManager.get().open( args[0] );
	 model_in.read(in, "", "TTL");

	 String directory = args[1] ;
	 Dataset dataset = TDBFactory.createDataset(directory) ;

	 dataset.begin(ReadWrite.WRITE) ;
	 Model model = dataset.getDefaultModel() ;
	 model.add(model_in);
	 dataset.commit() ;
	 dataset.end() ;

}
}
