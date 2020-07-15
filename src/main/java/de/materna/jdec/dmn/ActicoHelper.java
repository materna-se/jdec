package de.materna.jdec.dmn;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.UUID;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class ActicoHelper {
	public static String acticoFixDSWrapper(String model) {
		try {
			return acticoFixDS(model);
		} catch (IOException | SAXException | ParserConfigurationException | TransformerException e) {
			return model;
		}
	}
	
	public static String acticoFixDS(String model) throws IOException, SAXException, ParserConfigurationException, TransformerException {
		//Performance optimization
		if(!model.contains("decisionService")) return model;
		
		//Parse source XML
		InputSource is = new InputSource(new StringReader(model));
		Document d = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
		Element root = d.getDocumentElement();
		
		//Determine if model contains decision services
		//Change nothing if model does not contain decision services
		NodeList dsNodes = root.getElementsByTagName("dmn:decisionService");
		
		if(dsNodes.getLength() == 0) return model;
		
		String namespaceAttr = "xmlns:dmn";
		
		//Update DMN version
		String version = root.getAttribute(namespaceAttr);
		version = version.replaceFirst("^.*?DMN/(.*?)/.*?$", "$1");
		int versionInt = Integer.parseInt(version);
		
		if(versionInt < 20180521) {
			root.setAttribute(namespaceAttr, "http://www.omg.org/spec/DMN/20180521/MODEL/");
		}
		
		//Add missing variable elements to decision services
		for(int i = 0; i < dsNodes.getLength(); i++) {
			Node ds = dsNodes.item(i);
			String dsName = ds.getAttributes().getNamedItem("name").getTextContent();

			NodeList dsChildren = ds.getChildNodes();
			boolean hasDMNVariableElement = false;
			
			//Check if decision service is missing the variable element
			for(int j = 0; j < dsChildren.getLength(); j++) {
				Node child = dsChildren.item(j);

				try {
					if(child.getNodeName() == "dmn:variable" && 
							child.getAttributes().getNamedItem("name").getTextContent().equals(dsName)) {
						hasDMNVariableElement = true;
						break;
					}
				} catch(Exception e) {
					//Any exception indicates that the child in question has
					//a different structure than the variable child we are looking for
					//and is thus not applicable
					continue;
				}
			}
			
			//Add variable element if not already present
			if(!hasDMNVariableElement) {
				Element var = d.createElement("dmn:variable");
				var.setAttribute("id", "_" + UUID.randomUUID().toString().toUpperCase());
				var.setAttribute("name", dsName);
				
				ds.insertBefore(var, ds.getFirstChild());
			}
		}
		
		//Export document to string
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer tr = tf.newTransformer();
		StringWriter sw = new StringWriter();
		tr.transform(new DOMSource(d), new StreamResult(sw));
		String document = sw.toString();
		
		//Remove FEEL namespace for variable types
		//TODO: This doesn't seem to be necessary?
		//document = document.replaceAll(">feel:(.*?)<", ">$1<");
		//document = document.replaceAll("\"feel:(.*?)\"", "\"$1\"");
		
		return document;
	}
}
