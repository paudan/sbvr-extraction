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

import java.util.Arrays;
import net.tmine.opennlp.entities.Sentence;
import net.tmine.stanfordnlp.processing.MaxEntropyPOSTagger;
import org.junit.Assert;
import org.junit.Test;

public class CustomStanfordTaggerTest {
    
        
    @Test
    public void testTagMaxent() {
        Sentence sent = new Sentence("Worker load goods");
        String[] tags = Taggers.getCustomStanfordTagger().tagSentence(sent);
        Assert.assertArrayEquals(new String[] {"NNP", "VB", "NNS"}, tags);
    }
    
    @Test
    public void testTagMaxent2() {
        Sentence sent = new Sentence("Manager start company");
        String[] tags = Taggers.getCustomStanfordTagger().tagSentence(sent);
        Assert.assertArrayEquals(new String[] {"NNP", "VB", "NN"}, tags);
    }
    
    @Test
    public void testTagMaxent3() {
        Sentence sent = new Sentence("It is possible that manager start company");
        String[] tags = Taggers.getCustomStanfordTagger().tagSentence(sent);
        // System.out.println(Arrays.toString(tags));
        Assert.assertArrayEquals(new String[] {"PPS", "BEZ", "JJ", "CS", "NN", "VB", "NN"}, tags);
    }
    
    @Test
    public void testTagOriginal() {
        Sentence sent = new Sentence("Worker load goods");
        String[] tags = MaxEntropyPOSTagger.getInstance().tagSentence(sent);
        Assert.assertArrayEquals(new String[] {"NN", "NN", "NNS"}, tags);
    }
    
    @Test
    public void testTagOriginal2() {
        Sentence sent = new Sentence("Manager start company");
        String[] tags = MaxEntropyPOSTagger.getInstance().tagSentence(sent);
        Assert.assertArrayEquals(new String[] {"NN", "NN", "NN"}, tags);
    }
    
    @Test
    public void testTagOriginal3() {
        Sentence sent = new Sentence("It is possible that manager start company");
        String[] tags = MaxEntropyPOSTagger.getInstance().tagSentence(sent);
        Assert.assertArrayEquals(new String[] {"PRP", "VBZ", "JJ", "IN", "NN", "NN", "NN"}, tags);
    }
    
}
