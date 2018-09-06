/*
 * Copyright 2017 Paulius.
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

import java.util.Collection;
import java.util.Map;
import net.tmine.entities.InitializationException;

public interface VocabularyExtractor {

    public enum ConceptType {
        GENERAL_CONCEPT, INDIVIDUAL_CONCEPT, VERB_CONCEPT, BUSINESS_RULE
    }

    void extract() throws InitializationException;

    Collection<SBVRExpressionModel> getExtractedBusinessRules();

    Map<String, Map<SBVRExpressionModel, ConceptType>> getExtractedConceptsAsMap();

    Collection<SBVRExpressionModel> getExtractedGeneralConcepts();

    Collection<SBVRExpressionModel> getExtractedIndividualConcepts();

    Collection<SBVRExpressionModel> getExtractedVerbConcepts();
    
    void setUseNormalization(boolean normalize);
    
    boolean useNormalization();

    Map<String, ConceptType> getRumblings();

    void setRumblings(Map<String, ConceptType> rumblings);
}
