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

package org.ktu.isd.extraction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import net.tmine.entities.InitializationException;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import org.ktu.isd.extraction.SBVRExpressionModel.RuleType;
import org.ktu.isd.extraction.VocabularyExtractor.ConceptType;

public class SimulatedAutoExtractionTest {
    
    @Test
    public void testProcessBRRumbling1() {
        SimulatedAutoExtraction extract = new SimulatedAutoExtraction();
        String testString1 = "It is possible that customer winning ticket entered if winning ticket was entered and customer buy team merchandise";
        Object [] processed = extract.processBRRumbling(testString1);
        List<String> candidates = (List<String>) processed[0];
        assertEquals(3, candidates.size());
        assertEquals(candidates.get(0), "customer winning ticket entered");
        assertEquals(candidates.get(1), "winning ticket was entered");
        assertEquals(candidates.get(2), "customer buy team merchandise");
        assertEquals(RuleType.POSSIBILITY, (RuleType) processed[1]);
    }

    @Test
    public void testProcessBRRumbling2() {
        SimulatedAutoExtraction extract = new SimulatedAutoExtraction();
        String testString = "It is obligatory that customer winning ticket entered if winning ticket was entered and customer buy team merchandise"
                + " and customer buys one or more ticket and customer buys one or more ticket and customer buys one or more ticket";
        Object [] processed = extract.processBRRumbling(testString);
        List<String> candidates = (List<String>) processed[0];
        assertEquals(6, candidates.size());
        assertEquals(candidates.get(0), "customer winning ticket entered");
        assertEquals(candidates.get(1), "winning ticket was entered");
        assertEquals(candidates.get(2), "customer buy team merchandise");
        for (int i = 3; i <= 5; i++)
            assertEquals(candidates.get(i), "customer buys one or more ticket");
        assertEquals(RuleType.OBLIGATION, (RuleType) processed[1]);
    }    
    
    @Test
    public void testProcessBRRumbling3() {
        SimulatedAutoExtraction extract = new SimulatedAutoExtraction();
        String testString = "It is obligatory that customer winning ticket entered if winning ticket was entered";
        Object [] processed = extract.processBRRumbling(testString);
        List<String> candidates = (List<String>) processed[0];
        assertEquals(2, candidates.size());
        assertEquals(candidates.get(0), "customer winning ticket entered");
        assertEquals(candidates.get(1), "winning ticket was entered");
        assertEquals(RuleType.OBLIGATION, (RuleType) processed[1]);
    }   
    
    @Test
    public void testProcessBRRumbling4() {
        SimulatedAutoExtraction extract = new SimulatedAutoExtraction();
        String testString = "It is obligatory that customer winning ticket entered";
        Object [] processed = extract.processBRRumbling(testString);
        List<String> candidates = (List<String>) processed[0];
        assertEquals(1, candidates.size());
        assertEquals(candidates.get(0), "customer winning ticket entered");
        assertEquals(RuleType.OBLIGATION, (RuleType) processed[1]);
    }   
    
    @Test
    public void testExtraction1() {
        Map<String, ConceptType> rumblingsMap = new HashMap<>();
        String brule1 = "It is possible that customer winning ticket entered if winning ticket was entered and customer buy team merchandise";
        rumblingsMap.put(brule1, ConceptType.BUSINESS_RULE);
        SimulatedAutoExtraction extraction = new SimulatedAutoExtraction(rumblingsMap);
        try {
            extraction.extract();
        } catch (InitializationException ex) {
            Logger.getLogger(SimulatedAutoExtractionTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        Map<String, Map<SBVRExpressionModel, ConceptType>> extracted = extraction.getExtractedConceptsAsMap();
        Map<SBVRExpressionModel, ConceptType> ruleExtracted = extracted.get(brule1);
        assertEquals(4, ruleExtracted.size());
        checkExistsInExtracted(ruleExtracted, ConceptType.BUSINESS_RULE, brule1);
        String vcExtracted[] = new String [] {
            "customer winning ticket entered", "winning ticket was entered", "customer buy team merchandise"
        };
        checkExistsInExtracted(ruleExtracted, ConceptType.VERB_CONCEPT, vcExtracted);
        
        String vc1 = "customer winning ticket entered";
        Map<SBVRExpressionModel, ConceptType> vcExtracted1 = extracted.get(vc1);
        assertEquals(3, vcExtracted1.size());
        checkExistsInExtracted(vcExtracted1, ConceptType.VERB_CONCEPT, vc1);
        checkExistsInExtracted(vcExtracted1, ConceptType.GENERAL_CONCEPT, "customer", "ticket entered");
        String vc2 = "winning ticket was entered";
        Map<SBVRExpressionModel, ConceptType> vcExtracted2 = extracted.get(vc2);
        assertEquals(3, vcExtracted2.size());
        checkExistsInExtracted(vcExtracted2, ConceptType.VERB_CONCEPT, vc2);
        checkExistsInExtracted(vcExtracted2, ConceptType.GENERAL_CONCEPT, "winning", "was entered");
        String vc3 = "customer buy team merchandise";
        Map<SBVRExpressionModel, ConceptType> vcExtracted3 = extracted.get(vc3);
        assertEquals(3, vcExtracted3.size());
        checkExistsInExtracted(vcExtracted3, ConceptType.VERB_CONCEPT, vc3);
        checkExistsInExtracted(vcExtracted3, ConceptType.GENERAL_CONCEPT, "customer", "team merchandise");
    }
    
    private void checkExistsInExtracted(Map<SBVRExpressionModel, ConceptType> extracted, ConceptType cType, String... items) {
        Set<String> extractedRules = extracted.entrySet().stream()
           .filter(map -> map.getValue().equals(cType))
           .collect(Collectors.toMap(p -> p.getKey().toString(), p -> p.getValue())).keySet();
        assertThat(extractedRules, hasItems(items));
    }
    
    @Test
    public void testSplit() {
        String rumbling = "test";
        String parts[] = rumbling.split(" ");
        assertEquals(1, parts.length);
        assertEquals("test", parts[0]);
    }
    
    @Test
    public void testSortedList() {
        Set<String> items = new HashSet<>(Arrays.asList("project manager", "project", "developer", "project developer", "project managing person"));
        List<String> list = new ArrayList<>(items);
        Collections.sort(list, AbstractVocabularyExtractor.getDefaultComparator());
        assertEquals(list.get(0), "project managing person");
        assertEquals(list.get(1), "project developer");
    }
}
