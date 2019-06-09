# Fact Checker
### How to run
Given:
* Training data in the following format:<br/>
```
<http://swc2017.aksw.org/task2/dataset/Fact-ID> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/1999/02/22-rdf-syntax-ns#Statement> .
<http://swc2017.aksw.org/task2/dataset/Fact-ID> <http://swc2017.aksw.org/hasTruthValue> "value"^^<http://www.w3.org/2001/XMLSchema#float> .
<http://swc2017.aksw.org/task2/dataset/Fact-ID> <http://www.w3.org/1999/02/22-rdf-syntax-ns#object> <oubject-URI> .
<http://swc2017.aksw.org/task2/dataset/Fact-ID> <http://www.w3.org/1999/02/22-rdf-syntax-ns#predicate> <predicate-URI> .
<http://swc2017.aksw.org/task2/dataset/Fact-ID> <http://www.w3.org/1999/02/22-rdf-syntax-ns#subject> <oubject-URI> .
```
* Test data in the following format:<br/>
```
<http://swc2017.aksw.org/task2/dataset/Fact-ID> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/1999/02/22-rdf-syntax-ns#Statement> .
<http://swc2017.aksw.org/task2/dataset/Fact-ID> <http://www.w3.org/1999/02/22-rdf-syntax-ns#object> <oubject-URI> .
<http://swc2017.aksw.org/task2/dataset/Fact-ID> <http://www.w3.org/1999/02/22-rdf-syntax-ns#predicate> <predicate-URI> .
<http://swc2017.aksw.org/task2/dataset/Fact-ID> <http://www.w3.org/1999/02/22-rdf-syntax-ns#subject> <oubject-URI> .
```
* Knowledge base file in normal ttl format.

Then,
* Running `java TDBGenerator pathOfGraph pathOfOutput` converts the data in a given ttl file (`pathOfGraph`) into TDB-backed repository
stored in a directory at the given path (`pathOfOutput`).
* Running `java App pathOfTestData pathOfKnowledgeBase pathOfOutput` reads a file of test data in the format given above, a directory of
the knowledge base graph in TDB format (after converted from ttl to TDB using `TDBGenerator` as explained above), computes the truth value
for each of the statements in the test data and stores the final result in turtle format in a file at the given `pathOfOutput` using the
following format:
```
<http://swc2017.aksw.org/task2/dataset/Fact-ID> <http://swc2017.aksw.org/hasTruthValue> "value"^^<http://www.w3.org/2001/XMLSchema#float> .
```
* Running `java App2 pathOfTrainData pathOfTestData pathOfKnowledgeBase pathOfOutput` reads a file of training data, a file of test data
in the formats given above, a file of knowledge base graph (in ttl format without converting to TDB), computes the truth value
for each of the statements in the test data and stores the final result in turtle format in a file at the given `pathOfOutput` using the
following format:
```
<http://swc2017.aksw.org/task2/dataset/Fact-ID> <http://swc2017.aksw.org/hasTruthValue> "value"^^<http://www.w3.org/2001/XMLSchema#float> .
```
### Example:
Given training data in: `E:\Dataset_Train.nt`, test data in: `E:\Dataset_Test.nt` and a knowledge base in: `E:\mappingbased_objects_en.ttl`
* After running `java TDBGenerator E:\mappingbased_objects_en.ttl E:\MyKnowledgeGraph` the knowledge graph given in ttl file will be 
converted to TDB data structure and stored in the directory `E:\MyKnowledgeGraph`
* After running `java App E:\Dataset_Test.nt E:\MyKnowledgeGraph E:\Result.nt` the truth values of the statements in the test data
will be computed and stored in a file at `E:\Result.nt` using the formated explained above.
* After running `java App2 E:\Dataset_Train E:\Dataset_Test.nt E:\mappingbased_objects_en.ttl E:\Result.nt` the truth values of the statements in the test data
will be computed and stored in a file at `E:\Result.nt` using the formated explained above.
