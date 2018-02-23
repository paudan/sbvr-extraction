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

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import net.tmine.entities.InitializationException;
import net.tmine.entities.SentenceFactory;
import org.ktu.isd.extraction.StepwiseCascadedExtractor.ProcessedStructure;
import net.tmine.processing.NamedEntityFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple three stage cascaded extractor for vocabulary extraction
 * @author Paulius
 */
public class SimpleCascadedExtractor extends StepwiseCascadedExtractor {
    
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    public SimpleCascadedExtractor(Map<String, ConceptType> rumblings) {
        super(rumblings);
        searchForVerbs = false;
    }

    public SimpleCascadedExtractor(Map<String, ConceptType> rumblings, NamedEntityFinder finder, SentenceFactory factory) {
        super(rumblings, finder, factory);
        searchForVerbs = false;
    }
    
    public SimpleCascadedExtractor(NamedEntityFinder finder, SentenceFactory factory) {
        this(null, finder, factory);
    }    
    
    @Override
    public void extract() throws InitializationException {
        if (rumblings == null || rumblings.isEmpty())
            return;
        List<ProcessedStructure> updated = preprocessStructures();
        Pattern vc_nary = getCompiledPattern(PATTERN_MULTINARY_VC);
        Pattern vc_binary = getCompiledPattern(PATTERN_BINARY_VC);
        Pattern vc_unary = getCompiledPattern(PATTERN_UNARY_VC);
        Pattern br_pattern = getCompiledPattern(PATTERN_BR);
        
        for (ProcessedStructure cand : updated) {
            // Extract general and individual concepts
            tagWithRecognizedGC(cand);
            // Extract verb concepts
            tagWithRecognizedVC(cand, vc_nary);
            tagWithRecognizedVC(cand, vc_binary);
            tagWithRecognizedVC(cand, vc_unary);
            // Extract business rules
            tagWithRecognizedBR(cand, br_pattern);
            logger.debug("{}", cand);
        }
        logger.debug("General concepts: {}", generalConcepts);
        logger.debug("Verb concepts: {}", verbConcepts);
        logger.debug("Business rules: {}", businessRules);
    }
    
}
