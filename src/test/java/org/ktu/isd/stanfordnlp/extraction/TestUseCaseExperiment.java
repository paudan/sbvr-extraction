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
package org.ktu.isd.stanfordnlp.extraction;

import java.io.File;
import java.net.URL;
import net.tmine.entities.InitializationException;
import net.tmine.stanfordnlp.entities.SentenceFactory;
import net.tmine.stanfordnlp.processing.NamedEntityFinder;
import org.junit.Test;
import org.ktu.isd.extraction.ExtractionExperiment;
import org.ktu.isd.extraction.ExtractionExperiment.ExperimentConfigException;
import org.ktu.isd.extraction.SimpleCascadedExtractor;
import org.ktu.isd.extraction.SimulatedAutoExtraction;
import org.ktu.isd.extraction.StepwiseCascadedExtractor;
import org.ktu.isd.extraction.VocabularyExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestUseCaseExperiment {
    
    private VocabularyExtractor[] extractors = {
        new StepwiseCascadedExtractor(NamedEntityFinder.getInstance(), SentenceFactory.getInstance()),
        new SimpleCascadedExtractor(NamedEntityFinder.getInstance(), SentenceFactory.getInstance()),
        new SimulatedAutoExtraction()
    };
    
    private void runExperimentWithModel(String xmlPath, VocabularyExtractor extractor) {
        ClassLoader classLoader = TestUseCaseExperiment.class.getClassLoader();
        URL urlScores = classLoader.getResource("usecase/normalized/" + xmlPath);
        Logger logger = LoggerFactory.getLogger(TestUseCaseExperiment.class.getName());
        try {
            ExtractionExperiment experiment = new ExtractionExperiment(extractor, new File(urlScores.getFile()));
            logger.info("Running extractor " + extractor.getClass().getSimpleName());
            logger.info(experiment.getCaseName() + ", normalization: " + experiment.isNormalize());
            experiment.perform();
        } catch (ExperimentConfigException | InitializationException ex) {
            logger.error(ex.toString());
        }
    }

    @Test
    public void testUseCaseModels() {
        for (VocabularyExtractor extractor: extractors) {
            runExperimentWithModel("vepsem.xml", extractor);
            runExperimentWithModel("elements_of_style_1.xml", extractor);
            runExperimentWithModel("uml_bible_1.xml", extractor);
            runExperimentWithModel("uml_bible_2.xml", extractor);
            runExperimentWithModel("uml_specification.xml", extractor);
            runExperimentWithModel("uml_specification_2.xml", extractor);
            runExperimentWithModel("uml_distilled.xml", extractor);
            runExperimentWithModel("learning_uml.xml", extractor);
            runExperimentWithModel("el-attar-2007.xml", extractor);
            runExperimentWithModel("el-attar-2009.xml", extractor);
            runExperimentWithModel("el-attar-2012.xml", extractor);
        }
    }
}
