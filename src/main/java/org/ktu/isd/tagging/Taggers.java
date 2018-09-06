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
package org.ktu.isd.tagging;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import java.net.URL;
import net.tmine.opennlp.processing.CustomOpenNLPTagger;
import net.tmine.processing.POSTagger;
import net.tmine.stanfordnlp.processing.StanfordPOSTagger;


public class Taggers {
    
    public static POSTagger getCustomMaxentTagger() {
        return new CustomOpenNLPTagger("models/en-custom-maxent.bin");
    }
    
    public static POSTagger getCustomPerceptronTagger() {
        return new CustomOpenNLPTagger("models/en-custom-perceptron.bin");
    }
    
    public static POSTagger getCustomStanfordTagger() {
        String path = "models/stanford-maxent-model";
        URL modelFile = Taggers.class.getClassLoader().getResource(path);
        return new StanfordPOSTagger(new MaxentTagger(modelFile.getPath()));
    }
    
}
