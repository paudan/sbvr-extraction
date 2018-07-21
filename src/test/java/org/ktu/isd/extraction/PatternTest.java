/*
 * Copyright 2016 Paulius Danenas
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

import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import static org.ktu.isd.extraction.StepwiseCascadedExtractor.PATTERN_COMBINED_VC;

public class PatternTest {

    @Test
    public void testGCPattern() {
        String pattern = "^(\\[ADJ\\])*(\\[NNP\\])*(\\[NN\\]|\\[VBG\\]|\\[NNS\\])+$";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher("[NN][NN]");
        assertTrue(m.matches());
        m = r.matcher("[NNP][NN][NN]");
        assertTrue(m.matches());
        m = r.matcher("[NNP][NNP][NN][NN]");
        assertTrue(m.matches());
        m = r.matcher("[ADJ][ADJ][NNP][NNP][NNP][NN][NN]");
        assertTrue(m.matches());
        m = r.matcher("[NNP][NN][VBG]");
        assertTrue(m.matches());
        m = r.matcher("[NNP][VBG][NNS]");
        assertTrue(m.matches());
        m = r.matcher("[NNP][VBG][NNS][NN]");
        assertTrue(m.matches());

        m = r.matcher("[NN][NN][VB]");
        assertFalse(m.matches());
        m = r.matcher("[NNP][NN][NN][VB]");
        assertFalse(m.matches());
        m = r.matcher("[NN][NN][VB][NNP]");
        assertFalse(m.matches());
        m = r.matcher("[NN][NN][NNP]");
        assertFalse(m.matches());
        m = r.matcher("[ADJ][NN][NN][NNP]");
        assertFalse(m.matches());
        m = r.matcher("[ADJ][NN][ADJ][NN][NNP]");
        assertFalse(m.matches());
    }

    @Test
    public void testGCPattern2() {
        // Example: "Data mining taking over world". "Taking" would be recognized as noun
        String pattern = "^(\\[ADJ\\])*(\\[NNP\\])*(\\[NN\\]|\\[VBG\\]|\\[NNS\\])+$";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher("[NNP][NN][VBG][VBG][NN][NN]");
        assertTrue(m.matches());
    }

    @Test
    public void testVCPattern() {
        String gc_pattern = "(\\[ADJ\\])*(\\[NNP\\])*(\\[NN\\]|\\[VBG\\]|\\[NNS\\])+";
        String pattern = String.format("^%s\\[VBG\\]%s$", gc_pattern, gc_pattern);
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher("[NNP][NN][VBG][VBG][NN][NN]");
        assertTrue(m.matches());
    }

    @Test
    public void testICPattern() {
        String pattern = "(\\[ADJ\\])*(\\[NN\\]|\\[VBG\\]|\\[NNS\\])*(\\[NNP\\])+";
        System.out.println(pattern);
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher("[NN][NN][NNP][NNP]");
        assertTrue(m.matches());
        m = r.matcher("[NNP][NNP][NN][VBG]");
        assertFalse(m.matches());
        m = r.matcher("[NNP][NNP][NN]");
        assertFalse(m.matches());
    }

    @Test
    public void testCombinedPattern() {
        String pattern = "(\\[ADJ\\])*(\\[NN\\]|\\[VBG\\]|\\[NNS\\]|\\[NNP\\])+";
        System.out.println(pattern);
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher("[NN][NN][NNP][NNP]");
        assertTrue(m.matches());
        m = r.matcher("[NNP][NNP][NN][VBG]");
        assertTrue(m.matches());
        m = r.matcher("[NNP][NNP][NN]");
        assertTrue(m.matches());
    }

    private Pattern getPattern(String name) {
        try {
            Field field = StepwiseCascadedExtractor.class.getDeclaredField(name);
            field.setAccessible(true);
            String value = (String) field.get(null);
            return Pattern.compile(String.format("^(%s)$", value).replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]"));
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
            Logger.getLogger(PatternTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    @Test
    public void testGetPATTERN_GC() {
        Pattern r = getPattern("PATTERN_COMBINED_GC_IC");
        System.out.println(r.pattern());
        Matcher m = r.matcher("[NN][NN]");
        assertTrue(m.matches());

        m = r.matcher("[CGC][NN][NN]");
        assertFalse(m.matches());

        m = r.matcher("[CGC]");
        assertTrue(m.matches());
        
        m = r.matcher("[NNP]");
        assertTrue(m.matches());
        
        m = r.matcher("[NNP][NN]");
        assertTrue(m.matches());
    }

    @Test
    public void testGetPATTERN_VC() {
        Pattern r = getPattern("PATTERN_UNARY_VC");
        System.out.println(r.pattern());
        Matcher m = r.matcher("[CGC][VB]");
        assertTrue(m.matches());

        m = r.matcher("[CGC][NN][NN]");
        assertFalse(m.matches());

        m = r.matcher("[CIC][VB]");
        assertTrue(m.matches());
    }
    
    @Test
    public void testGetPATTERN_UNARY_VC() {
        Pattern r = getPattern("PATTERN_UNARY_VC");
        System.out.println(r.pattern());
        Matcher m = r.matcher("[CGC][VB]");
        assertTrue(m.matches());

        m = r.matcher("[CGC][NN][NN]");
        assertFalse(m.matches());

        m = r.matcher("[CIC][VB]");
        assertTrue(m.matches());
    }

    @Test
    public void testGetPATTERN_BINARY_VC() {
        Pattern r = getPattern("PATTERN_BINARY_VC");
        Matcher m = r.matcher("[CGC][VB][NN]");
        assertTrue(m.matches());

        m = r.matcher("[CGC][NN][NN]");
        assertFalse(m.matches());

        m = r.matcher("[CIC][VB][CGC]");
        assertTrue(m.matches());
        
        m = r.matcher("[CIC][VB][NN][NN]");
        assertTrue(m.matches());
        
        m = r.matcher("[NNP][VBD][IN][CGC]");
        assertTrue(m.matches());
        assertTrue(r.asPredicate().test("[NNP][VBD][IN][CGC]"));
    }
    
    @Test
    public void testGetPATTERN_BR() {
        // Taken from initOverrides() method
        String br_recognized = String.format("([RULE_TAG1]|[RULE_TAG2]|[RULE_TAG3]|[RULE_TAG4]){1}[WDT](%s)([IF_TAG](%s))*([CC]%s)*",
                PATTERN_COMBINED_VC, PATTERN_COMBINED_VC, PATTERN_COMBINED_VC);
        Pattern r = Pattern.compile(String.format("^(%s)$", br_recognized).replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]"));
//        Pattern r = getPattern("PATTERN_BR");
        Matcher m = r.matcher("[RULE_TAG1][WDT][CIC][VBP][CGC][IF_TAG][CGC][VBP][CGC]");
        assertTrue(m.matches());
        m = r.matcher("[RULE_TAG1][WDT][CGC][VB][CGC][IF_TAG][CGC][VBP][CGC]");
        assertTrue(m.matches());
    }
    
    @Test
    public void testGetPATTERN_COMBINED_VC() {
        Pattern r = getPattern("PATTERN_COMBINED_VC");
        System.out.println(r.pattern());
        Matcher m = r.matcher("[CGC][VBP]");
        assertTrue(m.matches());
        m = r.matcher("[CGC][VBP][CGC]");
        assertTrue(m.matches());
    }
    
}
