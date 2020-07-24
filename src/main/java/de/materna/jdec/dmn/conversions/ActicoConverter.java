package de.materna.jdec.dmn.conversions;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.UUID;

public class ActicoConverter {
	public static ConversionResult convertModel(String model) {
		try {
			// Performance optimization, checks if model is exported by ACTICO and includes decision services.
			if (!model.contains("exporter=\"ACTICO Modeler\"") || !model.contains("<dmn:decisionService")) {
				return new ConversionResult(false);
			}

			// Parse decision model.
			Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(model)));
			Element rootElement = document.getDocumentElement();

			// Determine if model contains decision services.
			// Change nothing if model does not contain decision services.
			NodeList decisionServiceNodes = rootElement.getElementsByTagName("dmn:decisionService");
			if (decisionServiceNodes.getLength() == 0) {
				return new ConversionResult(false);
			}

			//Update DMN version
			String version = rootElement.getAttribute("xmlns:dmn");
			if (!version.equals("http://www.omg.org/spec/DMN/20151101/dmn.xsd")) {
				return new ConversionResult(false);
			}

			rootElement.setAttribute("xmlns:dmn", "http://www.omg.org/spec/DMN/20180521/MODEL/");

			//Add missing variable elements to decision services
			for (int i = 0; i < decisionServiceNodes.getLength(); i++) {
				Node decisionServiceNode = decisionServiceNodes.item(i);
				String decisionServiceName = decisionServiceNode.getAttributes().getNamedItem("name").getTextContent();

				NodeList decisionServiceNodeChildren = decisionServiceNode.getChildNodes();

				boolean hasDMNVariableElement = false;
				//Check if decision service is missing the variable element
				for (int j = 0; j < decisionServiceNodeChildren.getLength(); j++) {
					Node child = decisionServiceNodeChildren.item(j);

					try {
						if (child.getNodeName().equals("dmn:variable") && child.getAttributes().getNamedItem("name").getTextContent().equals(decisionServiceName)) {
							hasDMNVariableElement = true;
							break;
						}
					}
					catch (Exception e) {
						//Any exception indicates that the child in question has
						//a different structure than the variable child we are looking for
						//and is thus not applicable
						continue;
					}
				}

				//Add variable element if not already present
				if (!hasDMNVariableElement) {
					Element var = document.createElement("dmn:variable");
					var.setAttribute("id", "_" + UUID.randomUUID().toString().toUpperCase());
					var.setAttribute("name", decisionServiceName);

					decisionServiceNode.insertBefore(var, decisionServiceNode.getFirstChild());
				}
			}

			//Export document to string
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			StringWriter stringWriter = new StringWriter();
			transformer.transform(new DOMSource(document), new StreamResult(stringWriter));

			return new ConversionResult(true, stringWriter.toString());
		}
		catch (IOException | SAXException | ParserConfigurationException | TransformerException e) {
			e.printStackTrace();
			return new ConversionResult(false);
		}
	}
}
