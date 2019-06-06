/*
 * Copyright 2018 Paulius Danenas
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ktu.isd.testing;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class CaseFileParser {

    private Path testFile;
    public List<String> targetGC = new ArrayList<>();
    public List<String> targetVC = new ArrayList<>();
    public List<String> targetBR = new ArrayList<>();
    public Set<String> distinctTargetGC = new HashSet<>();
    public Set<String> distinctTargetVC = new HashSet<>();
    public Set<String> distinctTargetBR = new HashSet<>();

    public CaseFileParser(Path testFile) {
        this.testFile = testFile;
        calculateOutputStatistics();
    }

    private void calculateOutputStatistics() {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(new File(testFile.toUri()));
            doc.getDocumentElement().normalize();
            Element root = doc.getDocumentElement();
            if (!root.getNodeName().equalsIgnoreCase("experiment_case"))
                return;
            NodeList nList = doc.getElementsByTagName("target");
            if (nList.getLength() != 1)
                return;
            Node nNode = nList.item(0);
            if (nNode.getNodeType() != Node.ELEMENT_NODE)
                return;
            NodeList entries = ((Element) nNode).getElementsByTagName("entry");
            for (int j = 0; j < entries.getLength(); j++) {
                Node nEntry = entries.item(j);
                if (nEntry.getNodeType() != Node.ELEMENT_NODE)
                    continue;
                NodeList concepts = ((Element) nEntry).getElementsByTagName("concepts");
                if (nList.getLength() != 1)
                    return;
                Node nConcepts = concepts.item(0);
                if (nConcepts.getNodeType() != Node.ELEMENT_NODE)
                    return;
                NodeList conceptList = ((Element) nConcepts).getElementsByTagName("concept");
                for (int l = 0; l < conceptList.getLength(); l++) {
                    Node nConcept = conceptList.item(l);
                    if (nConcept.getNodeType() != Node.ELEMENT_NODE) 
                        continue;
                    Element nConceptEl = (Element) nConcept;
                    if (nConceptEl.hasAttribute("type")) {
                        String type = nConceptEl.getAttribute("type");
                        if (type.equals("gc")) {
                            targetGC.add(nConceptEl.getTextContent());
                            distinctTargetGC.add(nConceptEl.getTextContent());
                        } else if (type.equals("vc")) {
                            targetVC.add(nConceptEl.getTextContent());
                            distinctTargetVC.add(nConceptEl.getTextContent());
                        } else if (type.equals("br")) {
                            targetBR.add(nConceptEl.getTextContent());
                            distinctTargetBR.add(nConceptEl.getTextContent());
                        }
                    }
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            Logger.getLogger(CaseFileParser.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
