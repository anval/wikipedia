package ner_dictionary;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import ner_dictionary.rules.RuleSet;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

/**
 * Application for creating a Named Entity Recognition dictionary
 *
 */
public class NERDictionary 
{
    public static void main( String[] args )
    {
    	NERDictionary app = new NERDictionary();
    	
    	Namespace ns = app.parseArguments(args);
    	
    	RuleSet ruleSet = app.parseRules(ns.getString("rules"));
    	
    	PrintStream outputFileStream = null;
    	try {
    		outputFileStream = new PrintStream(new FileOutputStream(ns.getString("output")));
    		
    		XMLReader reader = null;
    		try {
    			reader = XMLReaderFactory.createXMLReader();
    			WikiXMLParserHandler parserHandler = new WikiXMLParserHandler(outputFileStream, ruleSet);
    			reader.setContentHandler(parserHandler);
    			
    			for (String filePath : ns.<String> getList("source")) {
    				InputSource inputSource = null;
    				try {
    					inputSource = new InputSource(new FileInputStream(new File(filePath)));
    					try {
    						System.out.println("Parsing file " + filePath + " ...");
    						reader.parse(inputSource);
    						System.out.println("Parsing of file " + filePath + " finished");
    					} catch (SAXException e1) {
    						System.err.println("An error occured during parsing the file " + filePath);
    						e1.printStackTrace();
    					} catch (IOException e2) {
    						System.err.println("An error occured during parsing the file " + filePath);
    						e2.printStackTrace();
    					}
    				} catch (FileNotFoundException e) {
    					System.err.println("Can not open the file " + filePath);
    					e.printStackTrace();
    				}
    			}
    			System.out.println("Resolving redirects ...");
    			int redirectsRemained = parserHandler.processRedirects();
    			System.out.println("Redirects remained unresolved: " + redirectsRemained);
    		} catch (SAXException e) {
    			System.err.println("An error occured during inicialization of XML reader");
    			e.printStackTrace();
    			System.exit(1);
    		}
    		
    		outputFileStream.close();
    	} catch (FileNotFoundException e) {
    		System.err.println("Can not open or create the output file.");
    		e.printStackTrace();
    		System.exit(1);
    	}
    }
    
    private Namespace parseArguments(String[] args) {
    	ArgumentParser parser = ArgumentParsers.newArgumentParser("MainParser");
    	parser.defaultHelp(true);
    	parser.description("Create name entity recognition dictionary.");
    	parser.addArgument("-r", "--rules")
    		.required(true)
    		.help("Specify file in which are the rules for category recognition stored.");
    	
    	parser.addArgument("-o", "--output")
    		.setDefault("NER_dictionary.tsv")
    		.help("Specify file in which the dictionary will be created.");
    	
    	parser.addArgument("source")
    		.nargs("+")
    		.help("Source files to parse.");
    	
    	Namespace ns = null;
        try {
            ns = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }
        return ns;
    }
    
    private RuleSet parseRules(String rulesFilePath) {
    	try {
			JAXBContext jaxbContext = JAXBContext.newInstance(RuleSet.class);
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			return (RuleSet) jaxbUnmarshaller.unmarshal(new File(rulesFilePath));
		} catch (JAXBException e) {
			System.err.println("An error occured during parsing the rules file " + rulesFilePath);
			e.printStackTrace();
			System.exit(1);
			return null;
		}
    }
}