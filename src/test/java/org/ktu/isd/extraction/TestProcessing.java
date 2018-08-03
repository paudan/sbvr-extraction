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
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class TestProcessing {

    @Test
    public void testGeneralPatternFormation() {
        try {
            Field field = StepwiseCascadedExtractor.class.getDeclaredField("PATTERN_GC");
            field.setAccessible(true);
            assertEquals("([ADJ]|[JJ]|[JJR]|[JJS])*([NNP]|[POS]|[CD]|[NNPS])*([NN]|[VBG]|[POS]|[RBS]|[FW]|[NNS])+", field.get(null));
        } catch (SecurityException | IllegalArgumentException ex) {
            Logger.getLogger(SBVRExtractionTest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            Logger.getLogger(TestProcessing.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Test
    public void testUnaryPatternFormation() {
        try {
            Field field = StepwiseCascadedExtractor.class.getDeclaredField("PATTERN_UNARY_VC");
            field.setAccessible(true);
            assertEquals("(([CGC]|[CIC]){1}|([ADJ]|[JJ]|[JJR]|[JJS])*([NN]|[VBG]|[POS]|[RBS]|[FW]|[NNS]|[NNP]|[POS]|[CD]|[NNPS])+){1}([VB]|[VBD]|[VBG]|[VBN]|[VBP]|[VBZ]|[RB]|[RBR]|[RP]|[TO]|[IN]|[PREP])+([JJ]|[JJR]|[JJS])*", field.get(null));
        } catch (SecurityException | IllegalArgumentException ex) {
            Logger.getLogger(SBVRExtractionTest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            Logger.getLogger(TestProcessing.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Test
    public void testBinaryPatternFormation() {
        try {
            Field field = StepwiseCascadedExtractor.class.getDeclaredField("PATTERN_BINARY_VC");
            field.setAccessible(true);
            assertEquals("((([CGC]|[CIC]){1}|([ADJ]|[JJ]|[JJR]|[JJS])*([NN]|[VBG]|[POS]|[RBS]|[FW]|[NNS]|[NNP]|[POS]|[CD]|[NNPS])+){1}"
                    + "([VB]|[VBD]|[VBG]|[VBN]|[VBP]|[VBZ]|[RB]|[RBR]|[RP]|[TO]|[IN]|[PREP])+([JJ]|[JJR]|[JJS])*){1}"
                    + "(([CGC]|[CIC]){1}|([ADJ]|[JJ]|[JJR]|[JJS])*([NN]|[VBG]|[POS]|[RBS]|[FW]|[NNS]|[NNP]|[POS]|[CD]|[NNPS])+){1}", field.get(null));
        } catch (SecurityException | IllegalArgumentException ex) {
            Logger.getLogger(SBVRExtractionTest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            Logger.getLogger(TestProcessing.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Test
    public void testMultinaryPatternFormation() {
        try {
            Field field = StepwiseCascadedExtractor.class.getDeclaredField("PATTERN_MULTINARY_VC");
            field.setAccessible(true);
            System.out.println(field.get(null));
            assertEquals("((([CGC]|[CIC]){1}|([ADJ]|[JJ]|[JJR]|[JJS])*([NN]|[VBG]|[POS]|[RBS]|[FW]|[NNS]|[NNP]|[POS]|[CD]|[NNPS])+){1}"
                    + "([VB]|[VBD]|[VBG]|[VBN]|[VBP]|[VBZ]|[RB]|[RBR]|[RP]|[TO]|[IN]|[PREP])+([JJ]|[JJR]|[JJS])*){2,}"
                    + "(([CGC]|[CIC]){1}|([ADJ]|[JJ]|[JJR]|[JJS])*([NN]|[VBG]|[POS]|[RBS]|[FW]|[NNS]|[NNP]|[POS]|[CD]|[NNPS])+){1}", field.get(null));
        } catch (SecurityException | IllegalArgumentException ex) {
            Logger.getLogger(SBVRExtractionTest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            Logger.getLogger(TestProcessing.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
