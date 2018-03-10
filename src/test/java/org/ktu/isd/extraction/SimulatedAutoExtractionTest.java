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

import java.util.List;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.ktu.isd.extraction.SBVRExpressionModel.RuleType;

public class SimulatedAutoExtractionTest {
    
    @Test
    public void testProcessBRRumbling1() {
        SimulatedAutoExtraction extract = new SimulatedAutoExtraction(null);
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
        SimulatedAutoExtraction extract = new SimulatedAutoExtraction(null);
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
        SimulatedAutoExtraction extract = new SimulatedAutoExtraction(null);
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
        SimulatedAutoExtraction extract = new SimulatedAutoExtraction(null);
        String testString = "It is obligatory that customer winning ticket entered";
        Object [] processed = extract.processBRRumbling(testString);
        List<String> candidates = (List<String>) processed[0];
        assertEquals(1, candidates.size());
        assertEquals(candidates.get(0), "customer winning ticket entered");
        assertEquals(RuleType.OBLIGATION, (RuleType) processed[1]);
    }   
}
