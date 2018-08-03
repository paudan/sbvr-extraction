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

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import net.tmine.opennlp.entities.Sentence;
import net.tmine.stanfordnlp.processing.MaxEntropyPOSTagger;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class CustomTaggerTest {
    
    @Test
    public void testModelURL() {
        URL url = getClass().getClassLoader().getResource("models/en-custom-maxent.bin");
        System.out.println(url);
        assertNotNull(url);
        assertTrue(Files.exists(Paths.get(url.getFile()))); 
    }
    
    @Test
    public void testTagPerceptron() {
        Sentence sent = new Sentence("Pierre Vinken, 61 years old, will join the board as a nonexecutive director November 29.");
        String[] tags = Taggers.getCustomPerceptronTagger().tagSentence(sent);
        System.out.println(Arrays.toString(tags));
    }
    
    @Test
    public void testTagPerceptron2() {
        Sentence sent = new Sentence("Worker load goods");
        String[] tags = Taggers.getCustomPerceptronTagger().tagSentence(sent);
        System.out.println(Arrays.toString(tags));
    }
    
    @Test
    public void testTagPerceptron3() {
        Sentence sent = new Sentence("Manager start company");
        String[] tags = Taggers.getCustomPerceptronTagger().tagSentence(sent);
        System.out.println(Arrays.toString(tags));
    }
    
    @Test
    public void testTagMaxent() {
        Sentence sent = new Sentence("Worker load goods");
        String[] tags = Taggers.getCustomMaxentTagger().tagSentence(sent);
        System.out.println(Arrays.toString(tags));
    }
    
    @Test
    public void testTagMaxent2() {
        Sentence sent = new Sentence("Manager start company");
        String[] tags = Taggers.getCustomMaxentTagger().tagSentence(sent);
        System.out.println(Arrays.toString(tags));
    }
    
    @Test
    public void testTagMaxentOriginal() {
        Sentence sent = new Sentence("Manager start company");
        String[] tags = MaxEntropyPOSTagger.getInstance() .tagSentence(sent);
        System.out.println(Arrays.toString(tags));
    }

}
