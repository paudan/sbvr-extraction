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

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.tmine.entities.InitializationException;
import org.junit.Test;
import org.ktu.isd.extraction.ExtractionExperiment.EvaluationResult;
import org.ktu.isd.extraction.ExtractionExperiment.ExperimentConfigException;
import org.ktu.isd.tagging.Taggers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestUseCaseExperiment {

    private class ExtractorOutput {

        String model;
        Double[] scores;

        public ExtractorOutput(String model, Double[] scores) {
            this.model = model;
            this.scores = scores;
        }
    }

    private ExtractorOutput runExperimentWithModel(String xmlPath, VocabularyExtractor extractor) {
        ClassLoader classLoader = getClass().getClassLoader();
        URL urlScores = classLoader.getResource("usecase/normalized/" + xmlPath);
        Logger logger = LoggerFactory.getLogger(getClass().getName());
        try {
            ExtractionExperiment experiment = new ExtractionExperiment(extractor, new File(urlScores.getFile()));
            logger.info("Running extractor " + extractor.getClass().getSimpleName() + " for " + xmlPath);
            logger.info(experiment.getCaseName() + ", normalization: " + experiment.isNormalize());
            EvaluationResult result = experiment.perform();
            return new ExtractorOutput(experiment.getCaseName(),
                    new Double[]{result.ratios[0][3], result.ratios[0][5], result.ratios[2][3], result.ratios[2][5],
                        result.ratios[3][3], result.ratios[3][5]});
        } catch (ExperimentConfigException | InitializationException ex) {
            logger.error(ex.toString());
        }
        return null;
    }

    private void runUseCaseExtractionExperiment(VocabularyExtractor[] extractors) {
        Map<String, List<ExtractorOutput>> fullResults = new HashMap<>();
        for (VocabularyExtractor extractor : extractors) {
            List<ExtractorOutput> extractorResults = new ArrayList<>();
            extractorResults.add(runExperimentWithModel("vepsem.xml", extractor));
            extractorResults.add(runExperimentWithModel("elements_of_style_1.xml", extractor));
            extractorResults.add(runExperimentWithModel("uml_bible_1.xml", extractor));
            extractorResults.add(runExperimentWithModel("uml_bible_2.xml", extractor));
            extractorResults.add(runExperimentWithModel("uml_specification.xml", extractor));
            extractorResults.add(runExperimentWithModel("uml_specification_2.xml", extractor));
            extractorResults.add(runExperimentWithModel("uml_distilled.xml", extractor));
            extractorResults.add(runExperimentWithModel("learning_uml.xml", extractor));
            extractorResults.add(runExperimentWithModel("el-attar-2007.xml", extractor));
            extractorResults.add(runExperimentWithModel("el-attar-2009.xml", extractor));
            extractorResults.add(runExperimentWithModel("el-attar-2012.xml", extractor));
            fullResults.put(extractor.getClass().getSimpleName(), extractorResults);
        }
        StringBuilder builder = new StringBuilder();
        for (Entry<String, List<ExtractorOutput>> extractorPerf : fullResults.entrySet()) {
            builder.append(extractorPerf.getKey()).append("\n");
            builder.append("Model\tGC\t\tVC\t\tBR\t\t\n");
            builder.append("\tPrec\tF-Score\tPrec\tF-Score\tPrec\tF-Score\t\n");
            for (ExtractorOutput output : extractorPerf.getValue()) {
                builder.append(output.model).append("\t");
                for (int i = 0; i < 6; i++)
                    builder.append(String.format("%.3f\t", output.scores[i]));
                builder.append("\n");
            }
            builder.append("\n");
        }
        System.out.println(builder.toString());
    }
/*
    @Test
    public void testUseCaseModelsOpenNLP() {
        Logger logger = LoggerFactory.getLogger(getClass().getName());
        logger.info("Testing performance using default taggers trained with OpenNLP");
        net.tmine.opennlp.processing.NamedEntityFinder finder = new net.tmine.opennlp.processing.NamedEntityFinder();
        net.tmine.opennlp.entities.SentenceFactory sentFactory = net.tmine.opennlp.entities.SentenceFactory.getInstance();
        net.tmine.opennlp.processing.MaxEntropyPOSTagger tagger = net.tmine.opennlp.processing.MaxEntropyPOSTagger.getInstance();
        StepwiseCascadedExtractor stepwise = new StepwiseCascadedExtractor(finder, sentFactory);
        stepwise.setTagger(tagger);
        SimpleCascadedExtractor simple = new SimpleCascadedExtractor(finder, sentFactory);
        simple.setTagger(tagger);
        runUseCaseExtractionExperiment(new VocabularyExtractor[]{stepwise, simple});
        System.gc();
    }

    @Test
    public void testUseCaseModelsWithCustomTagger() {
        Logger logger = LoggerFactory.getLogger(getClass().getName());
        logger.info("Testing performance using custom taggers trained with OpenNLP");
        net.tmine.opennlp.processing.NamedEntityFinder finder = new net.tmine.opennlp.processing.NamedEntityFinder();
        net.tmine.opennlp.entities.SentenceFactory sentFactory = net.tmine.opennlp.entities.SentenceFactory.getInstance();
        StepwiseCascadedExtractor stepwise = new StepwiseCascadedExtractor(finder, sentFactory);
        stepwise.setTagger(Taggers.getCustomMaxentTagger());
        SimpleCascadedExtractor simple = new SimpleCascadedExtractor(finder, sentFactory);
        simple.setTagger(Taggers.getCustomMaxentTagger());
        runUseCaseExtractionExperiment(new VocabularyExtractor[]{stepwise, simple});
        System.gc();
    }
*/

    @Test
    public void testUseCaseModelsStanford() {
        Logger logger = LoggerFactory.getLogger(getClass().getName());
        logger.info("Testing performance using Stanford CoreNLP tools");
        VocabularyExtractor[] extractors = {
            new StepwiseCascadedExtractor(net.tmine.stanfordnlp.processing.NamedEntityFinder.getInstance(),
            net.tmine.stanfordnlp.entities.SentenceFactory.getInstance()),
            new SimpleCascadedExtractor(net.tmine.stanfordnlp.processing.NamedEntityFinder.getInstance(),
            net.tmine.stanfordnlp.entities.SentenceFactory.getInstance()),
            new SimulatedAutoExtraction()
        };
        runUseCaseExtractionExperiment(extractors);
        System.gc();
    }

}
