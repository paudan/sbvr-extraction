/*
 * Copyright 2017 Paulius Danenas
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
package org.ktu.isd.extraction;

import com.google.common.collect.Sets;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import net.tmine.entities.InitializationException;
import org.ktu.isd.extraction.VocabularyExtractor.ConceptType;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ExtractionExperiment {

    public class ExperimentConfigException extends Exception {

        public ExperimentConfigException(String message) {
            super(message);
        }

    }

    private Map<String, ConceptType> rumblings;
    /** Results obtained manually */
    private Map<String, Map<String, ConceptType>> actualConcepts = new HashMap<>();
    private Map<String, EvaluationResult> allResults = new HashMap<>();
    private boolean normalize;
    private String caseName;
    private VocabularyExtractor extractor;

    public ExtractionExperiment(VocabularyExtractor extractor, Map<String, ConceptType> rumblings, boolean normalize) {
        this.rumblings = rumblings;
        this.extractor = extractor;
        this.normalize = normalize;
    }

    public ExtractionExperiment(VocabularyExtractor extractor, File xml) throws ExperimentConfigException {
        loadConfiguration(xml);
        this.extractor = extractor;
    }

    public static class EvaluationResult {

        public String rumbling;
        public int[][] counts;
        public double[][] ratios;

        public EvaluationResult(String rumbling) {
            this.rumbling = rumbling;
            counts = new int[4][5]; //4 types (GC, IC, VC, BR) x 5 ratios (total_actual, total_extracted, TP, FP and FN)
            ratios = new double[4][6]; //4 types (GC, IC, VC, BR) x 6 ratios (TPR, FPR, FNR, precision, recall, Fscore)
            for (int index = 0; index < 4; index++) {
                int[] countVals = new int[5];
                Arrays.fill(countVals, Integer.MIN_VALUE);
                counts[index] = countVals;
                double[] ratioVals = new double[6];
                Arrays.fill(ratioVals, Double.NaN);
                ratios[index] = ratioVals;
            }
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("Error counts matrix").append("\n");
            String[] names = new String[]{"#actual", "#output", "#TP", "#FP", "#FN"};
            String[] conceptNames = new String[]{"GC", "IC", "VC", "BR"};
            for (int j = 0; j < names.length; j++)
                builder.append("\t").append(names[j]);
            builder.append("\n");
            for (int i = 0; i < conceptNames.length; i++) {
                builder.append(conceptNames[i]).append("\t");
                for (int j = 0; j < names.length; j++)
                    builder.append(counts[i][j]).append("\t");
                builder.append("\n");
            }
            builder.append("\n").append("Evaluation score matrix").append("\n");
            names = new String[]{"TPR", "FPR", "FNR", "Prec", "Rec", "F-Score"};
            for (int j = 0; j < names.length; j++)
                builder.append("\t").append(names[j]);
            builder.append("\n");
            for (int i = 0; i < conceptNames.length; i++) {
                builder.append(conceptNames[i]).append("\t");
                for (int j = 0; j < names.length; j++)
                    builder.append(String.format("%1.4f", ratios[i][j])).append("\t");
                builder.append("\n");
            }
            return builder.toString();
        }

    }

    public EvaluationResult perform() throws InitializationException {
        if (extractor instanceof StepwiseCascadedExtractor)
            ((StepwiseCascadedExtractor) extractor).setReplaceSynonyms(false);
        extractor.setRumblings(rumblings);
        extractor.setUseNormalization(normalize);
        extractor.extract();
        Map<String, Map<SBVRExpressionModel, ConceptType>> extracted = extractor.getExtractedConceptsAsMap();

        for (String rumbling : rumblings.keySet()) {
            Map<SBVRExpressionModel, ConceptType> extractedByConcept = extracted.get(rumbling);
            Map<String, ConceptType> actual = actualConcepts.get(rumbling);
            if (actual == null)
                continue;
            EvaluationResult result = allResults.get(rumbling);
            if (result == null) {
                result = new EvaluationResult(rumbling);
                allResults.put(rumbling, result);
            }
            if (extractedByConcept == null)
                extractedByConcept = new HashMap<>();

            ConceptType[] types = ConceptType.values();
            Set<String> extractedStrings = new HashSet<>();
            Set<String> actualStrings = new HashSet<>();
            for (int i = 0; i < types.length; i++) {
                // Skip evaluation of GC/IC extracted from BR, as they must be part of extracted VC
                if (rumblings.get(rumbling).equals(ConceptType.BUSINESS_RULE)
                        && (types[i].equals(ConceptType.GENERAL_CONCEPT) || types[i].equals(ConceptType.INDIVIDUAL_CONCEPT)))
                    continue;
                extractedStrings.clear();
                for (Entry<SBVRExpressionModel, ConceptType> sbvr : extractedByConcept.entrySet())
                    if (sbvr.getValue().equals(types[i]))
                        extractedStrings.add(sbvr.getKey().toString());
                actualStrings.clear();
                for (Entry<String, ConceptType> sbvr : actual.entrySet())
                    if (sbvr.getValue().equals(types[i]))
                        actualStrings.add(sbvr.getKey());
                result = calculateMetrics(result, extractedStrings, actualStrings, i, rumbling);
            }
        }

        // Calculate macro-averaging results
        EvaluationResult macroResult = new EvaluationResult(null);
        double[][] macroResults = new double[4][6];
        int[][] macroCounts = new int[4][5];
        for (EvaluationResult result : allResults.values())
            for (int i = 0; i < 4; i++)
                for (int j = 0; j < 5; j++)
                    macroCounts[i][j] += result.counts[i][j] != Integer.MIN_VALUE ? result.counts[i][j] : 0;
        for (int i = 0; i < 4; i++) {
            macroResults[i][0] = macroCounts[i][2] / (double) macroCounts[i][0];
            macroResults[i][1] = macroCounts[i][3] / (double) macroCounts[i][0];
            macroResults[i][2] = macroCounts[i][4] / (double) macroCounts[i][0];
            macroResults[i][3] = macroCounts[i][2] / (double) (macroCounts[i][2] + macroCounts[i][3]);
            macroResults[i][4] = macroCounts[i][2] / (double) (macroCounts[i][2] + macroCounts[i][4]);
            macroResults[i][5] = 2. * macroResults[i][3] * macroResults[i][4] / (macroResults[i][3] + macroResults[i][4]);
        }
        macroResult.counts = macroCounts;
        macroResult.ratios = macroResults;
        System.out.println(macroResult);
        return macroResult;
    }

    public void setActualConcepts(Map<String, Map<String, ConceptType>> actualConcepts) {
        this.actualConcepts = actualConcepts;
    }

    private EvaluationResult calculateMetrics(EvaluationResult result, Set<String> extracted, Set<String> actual, int index, String rumbling) {
        // Get the concepts which were extracted correctly (true positives)
        Set<String> tp = Sets.intersection(actual, extracted);
        // Get the concepts which were not extracted from actual concepts (false negatives)
        Set<String> fn = Sets.difference(actual, extracted);
        // Get the concepts which were extracted incorrectly (false positives)
        Set<String> fp = Sets.difference(extracted, actual);
        result.counts[index] = new int[]{actual.size(), extracted.size(), tp.size(), fn.size(), fp.size()};
        double denom = (double) extracted.size();
        double prec = tp.size() / (double) (tp.size() + fp.size());
        double recall = tp.size() / (double) (tp.size() + fn.size());
        result.ratios[index] = new double[]{tp.size() / denom, fn.size() / denom, fp.size() / (double) actual.size(),
            prec, recall, 2 * prec * recall / (prec + recall)};
        return result;
    }

    private ConceptType getConceptType(String typeRepr) {
        if (typeRepr.equalsIgnoreCase("gc"))
            return ConceptType.GENERAL_CONCEPT;
        else if (typeRepr.equalsIgnoreCase("ic"))
            return ConceptType.INDIVIDUAL_CONCEPT;
        else if (typeRepr.equalsIgnoreCase("vc"))
            return ConceptType.VERB_CONCEPT;
        else if (typeRepr.equalsIgnoreCase("br"))
            return ConceptType.BUSINESS_RULE;
        return null;
    }

    private void loadConfiguration(File xml) throws ExperimentConfigException {
        try {
            rumblings = new HashMap<>();
            actualConcepts = new HashMap<>();
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc;
            doc = dBuilder.parse(xml);
            Node root = doc.getDocumentElement();
            root.normalize();
            if (root.hasChildNodes()) {
                NodeList rootNodes = root.getChildNodes();
                for (int count = 0; count < rootNodes.getLength(); count++) {
                    Node expConfigNode = rootNodes.item(count);
                    if (expConfigNode.getNodeType() == Node.ELEMENT_NODE && expConfigNode.getNodeName().equalsIgnoreCase("normalize"))
                        normalize = Boolean.parseBoolean(expConfigNode.getTextContent());
                    else if (expConfigNode.getNodeType() == Node.ELEMENT_NODE && expConfigNode.getNodeName().equalsIgnoreCase("case_name"))
                        caseName = expConfigNode.getTextContent();
                    else if (expConfigNode.getNodeType() == Node.ELEMENT_NODE && expConfigNode.getNodeName().equalsIgnoreCase("source")) {
                        NodeList rumblingNodes = expConfigNode.getChildNodes();
                        for (int i = 0; i < rumblingNodes.getLength(); i++) {
                            Node rNode = rumblingNodes.item(i);
                            if (rNode.getNodeType() == Node.ELEMENT_NODE && rNode.getNodeName().equalsIgnoreCase("rumbling")) {
                                NamedNodeMap rNodeAttr = rNode.getAttributes();
                                if (rNodeAttr == null)
                                    continue;
                                Node expected = rNodeAttr.getNamedItem("expected");
                                if (expected != null) {
                                    ConceptType type = getConceptType(expected.getTextContent());
                                    String rumblingVal = rNode.getTextContent();
                                    if (type != null && rumblingVal != null)
                                        rumblings.put(rumblingVal, type);
                                }
                            }
                        }
                    } else if (expConfigNode.getNodeType() == Node.ELEMENT_NODE && expConfigNode.getNodeName().equalsIgnoreCase("target")) {
                        NodeList expectedNodes = expConfigNode.getChildNodes();
                        for (int i = 0; i < expectedNodes.getLength(); i++) {
                            Node rNode = expectedNodes.item(i);
                            if (rNode.getNodeType() == Node.ELEMENT_NODE && rNode.getNodeName().equalsIgnoreCase("entry")) {
                                NodeList entryNodes = rNode.getChildNodes();
                                if (entryNodes == null)
                                    continue;
                                String rumbling = null;
                                Map<String, ConceptType> expectedConcepts = new HashMap<>();
                                for (int k = 0; k < entryNodes.getLength(); k++) {
                                    Node entryNode = entryNodes.item(k);
                                    if (entryNode.getNodeType() == Node.ELEMENT_NODE)
                                        if (entryNode.getNodeName().equalsIgnoreCase("rumbling"))
                                            rumbling = entryNode.getTextContent();
                                        else if (entryNode.getNodeName().equalsIgnoreCase("concepts")) {
                                            NodeList conceptsNodes = entryNode.getChildNodes();
                                            if (conceptsNodes == null)
                                                continue;
                                            for (int j = 0; j < conceptsNodes.getLength(); j++) {
                                                Node conceptNode = conceptsNodes.item(j);
                                                if (conceptNode.getNodeType() == Node.ELEMENT_NODE && conceptNode.getNodeName().equalsIgnoreCase("concept")) {
                                                    NamedNodeMap rNodeAttr = conceptNode.getAttributes();
                                                    if (rNodeAttr == null)
                                                        continue;
                                                    Node type = rNodeAttr.getNamedItem("type");
                                                    if (type != null) {
                                                        ConceptType expectedType = getConceptType(type.getTextContent());
                                                        if (expectedType != null && conceptNode.getTextContent() != null)
                                                            expectedConcepts.put(conceptNode.getTextContent(), expectedType);
                                                    }
                                                }
                                            }
                                        }
                                }
                                if (rumbling != null && !expectedConcepts.isEmpty())
                                    actualConcepts.put(rumbling, expectedConcepts);
                            }
                        }
                    }
                }
            }
            if (rumblings.isEmpty())
                throw new ExperimentConfigException("Source rumbling set must not be empty");
            if (actualConcepts.isEmpty())
                throw new ExperimentConfigException("Expected concepts set must not be empty");
        } catch (IOException | ParserConfigurationException | SAXException ex) {
            Logger.getLogger(ExtractionExperiment.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public Map<String, ConceptType> getRumblings() {
        return rumblings;
    }

    public Map<String, Map<String, ConceptType>> getActualConcepts() {
        return actualConcepts;
    }

    public String getCaseName() {
        return caseName;
    }

    public void setCaseName(String caseName) {
        this.caseName = caseName;
    }

    public boolean isNormalize() {
        return normalize;
    }

}
