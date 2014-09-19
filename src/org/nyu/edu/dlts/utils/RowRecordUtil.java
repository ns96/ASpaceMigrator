package org.nyu.edu.dlts.utils;

import org.nyu.edu.dlts.models.RowRecord;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: nathan
 * Date: 9/17/14
 * Time: 11:10 AM
 * Utility class for creating row records and perform other operation on them
 */
public class RowRecordUtil {

    /**
     * Method to get an array list of row records from an xml file
     *
     * @param recordType
     * @param elementName Used to get the correct node in the xml file
     * @param xmlFile
     * @return
     */
    public static ArrayList<RowRecord> getRowRecordFromXML(File xmlFile, String recordType, String elementName) {
        ArrayList<RowRecord> recordList = new ArrayList<RowRecord>();

        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
	        Document doc = dBuilder.parse(xmlFile);
	        doc.getDocumentElement().normalize();

	        System.out.println("Root element :" + doc.getDocumentElement().getNodeName());

	        NodeList nodeList = doc.getElementsByTagName(elementName);

	        for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);

		        if (node.getNodeType() == Node.ELEMENT_NODE) {
			        Element element = (Element) node;
                    NodeList childList  = element.getChildNodes();

                    HashMap<String, String> recordMap = new HashMap<String, String>();

                    for (int j = 0; j < childList.getLength(); j++) {
                        Node childNode = childList.item(j);
                        if(childNode.getNodeType() == Node.ELEMENT_NODE) {
                            String key = childNode.getNodeName();
                            String value =childNode.getTextContent();
                            recordMap.put(key, value);

                            System.out.println("Element Name " + key + ", value = " + value);
                        }
                    }

                    // now create the RowRecordObject
                    RowRecord record = new RowRecord(recordType, recordType +"_" + i, recordMap);
                    recordList.add(record);

                    System.out.println("\nRecord #" + i);
                }
            }
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return recordList;
    }
}
