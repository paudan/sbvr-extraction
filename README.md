Experimental code for the paper "Natural Language Processing-Enhanced Extraction of SBVR Business Vocabularies and Business Rules from UML Use Case Diagrams". Can be applied extract of business vocabulary and business rules from text phrases in various visual models, such as Use Case diagrams. Compared to the initial implementation, it introduces an enhancement to our previous development which provides more advanced information extraction capabilities (such as recognition of entities, entire noun and verb phrases, multinary associations) and better quality of the extraction results compared to our previous solution.  

The code is written in Java, using multiple NLP toolkits, such as Stanford CoreNLP, Apache OpenNLP. It also requires interface with Wordnet. Most of this functionality is implemented in separate [project](https://github.com/paudan/tmine) which is required to run the code.      

## Usage 

The code can be compiled with all the required dependencies using Maven. The experiments are run as Java unit test files (particularly `TestUseCaseExperiment`).  

## Citation

if you find the paper useful, please cite it as:

```
@article{Danenas2020nlp,  
	author = "Paulius Danenas, Tomas Skersys, Rimantas Butleris",  
	doi = "10.1016/j.datak.2020.101822",  
	journal = "Data & Knowledge Engineering",  
	pages = "101822",  
	title = "Natural language processing-enhanced extraction of SBVR business vocabularies and business rules from UML use case diagrams",  
	url = "http://www.sciencedirect.com/science/article/pii/S0169023X1930299X",  
	volume = "128",  
	year = "2020"  
}
```
