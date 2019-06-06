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
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import net.tmine.entities.InitializationException;
import net.tmine.processing.POSTagger;
import net.tmine.stanfordnlp.processing.MaxEntropyPOSTagger;
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

    private ExtractorOutput runExperimentWithModel(Path xmlPath, VocabularyExtractor extractor, String path) {
        /*ClassLoader classLoader = TestUseCaseExperiment.class.getClassLoader();
        URL urlScores = classLoader.getResource(path + xmlPath);*/
        Logger logger = LoggerFactory.getLogger(getClass().getName());
        try {
            ExtractionExperiment experiment = new ExtractionExperiment(extractor, new File(xmlPath.toUri()));
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

    public Set<Path> getFileList(String dir) {
        Path path = Paths.get("..", "..", "extraction-dataset", dir);
        Set<Path> experimentFiles = new HashSet<>();
        if (Files.isDirectory(path))
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path, "*.xml")) {
                for (Path p : stream)                 
                    experimentFiles.add(p);
            } catch (IOException e) {
                LoggerFactory.getLogger(getClass().getName()).info(e.getMessage());
            }
        return experimentFiles;
    }

    private void runUseCaseExtractionExperiment(VocabularyExtractor[] extractors, String path) {
        Map<String, List<ExtractorOutput>> fullResults = new HashMap<>();
        Set<Path> experimentFiles = getFileList(path);
        for (VocabularyExtractor extractor : extractors) {
            List<ExtractorOutput> extractorResults = new ArrayList<>();
            for (Path filePath: experimentFiles)
                extractorResults.add(runExperimentWithModel(filePath, extractor, path));
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
        runUseCaseExtractionExperiment(new VocabularyExtractor[]{stepwise, simple}, "usecase/normalized");
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
        runUseCaseExtractionExperiment(new VocabularyExtractor[]{stepwise, simple}, "usecase/normalized");
        System.gc();
    }

    @Test
    public void testUseCaseModelsStanford() {
        Logger logger = LoggerFactory.getLogger(getClass().getName());
        logger.info("Testing performance using Stanford CoreNLP tools");
        net.tmine.stanfordnlp.processing.NamedEntityFinder finder = net.tmine.stanfordnlp.processing.NamedEntityFinder.getInstance();
        net.tmine.stanfordnlp.entities.SentenceFactory sentFactory = net.tmine.stanfordnlp.entities.SentenceFactory.getInstance();
        POSTagger tagger = MaxEntropyPOSTagger.getInstance();
        StepwiseCascadedExtractor stepwise = new StepwiseCascadedExtractor(finder, sentFactory);
        stepwise.setTagger(tagger);
        SimpleCascadedExtractor simple = new SimpleCascadedExtractor(finder, sentFactory);
        simple.setTagger(tagger);
        VocabularyExtractor[] extractors = {stepwise, simple, new SimulatedAutoExtraction()};
        runUseCaseExtractionExperiment(extractors, "usecase/normalized");
        System.gc();
    }

    @Test
    public void testUseCaseModelsStanfordCustom() {
        Logger logger = LoggerFactory.getLogger(getClass().getName());
        logger.info("Testing performance using Stanford CoreNLP tools");
        net.tmine.stanfordnlp.processing.NamedEntityFinder finder = net.tmine.stanfordnlp.processing.NamedEntityFinder.getInstance();
        net.tmine.stanfordnlp.entities.SentenceFactory sentFactory = net.tmine.stanfordnlp.entities.SentenceFactory.getInstance();
        POSTagger tagger = Taggers.getCustomStanfordTagger();
        StepwiseCascadedExtractor stepwise = new StepwiseCascadedExtractor(finder, sentFactory);
        stepwise.setTagger(tagger);
        SimpleCascadedExtractor simple = new SimpleCascadedExtractor(finder, sentFactory);
        simple.setTagger(tagger);
        VocabularyExtractor[] extractors = {stepwise, simple};
        runUseCaseExtractionExperiment(extractors, "usecase/normalized");
        System.gc();
    }

    @Test
    public void testUseCaseSimulatedAutoExtraction() {
        VocabularyExtractor[] extractors = {new SimulatedAutoExtraction()};
        runUseCaseExtractionExperiment(extractors, "usecase/non-normalized");
        System.gc();
    }

}
