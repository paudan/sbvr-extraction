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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.tmine.entities.InitializationException;
import net.tmine.processing.POSTagger;
import net.tmine.stanfordnlp.entities.SentenceFactory;
import net.tmine.stanfordnlp.processing.NamedEntityFinder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;
import org.ktu.isd.extraction.SBVRExpressionModel.ExpressionType;
import org.ktu.isd.extraction.VocabularyExtractor.ConceptType;
import org.ktu.isd.tagging.Taggers;

public class SBVRExtractionTest {

    public static List<String> strings = Arrays.asList("rental manager", "rental manager’s assistant", "customer", "car rental system",
            "rental contract", "rental insurance", "car reservation", "car booking", "walk-in car booking",
            "additional personal data", "personal data", "rental contract", "additional personal data",
            "sales manager", "rental manager generalizes rental manager’s assistant", "rental manager create rental contract",
            "rental manager’s assistant create rental contract", "rental manager’s assistant manage rental insurance",
            "rental manager’s assistant confirm car reservation", "customer make car booking",
            "customer provide additional personal data", "customer confirm rental contract",
            "customer make walk-in car booking", "rental contract is confirmed",
            "additional personal data is required", "sales manager is absent",
            "It is obligatory that rental manager create rental contract if customer make car booking",
            "It is obligatory that rental manager’s assistant create rental contract if customer makes car booking",
            "It is obligatory that rental manager’s assistant manage car insurance if rental manager create rental contract",
            "It is obligatory that rental manager’s assistant manage car insurance if rental manager's assistant create rental contract",
            "It is obligatory that customer confirm rental contract if rental manager create rental contract",
            "It is obligatory that customer confirm rental contract if rental manager’s assistant create rental contract",
            "It is permitted that customer provide additional personal data if rental manager create rental contract",
            "It is permitted that customer was providing additional personal data if rental manager's assistant create rental contract",
            "It is permitted that rental manager’s assistant confirm car reservation if rental manager create rental contract",
            "It is permitted that rental manager’s assistant confirm car reservation if rental manager’s assistant create rental contract",
            "It is obligatory that customer provide additional personal data if additional personal data is required",
            "It is obligatory that rental manager’s assistant confirm car reservation if rental contract is confirmed",
            "It is obligatory that rental manager’s assistant create rental contract if sales manager is absent");

    public static Map<String, ConceptType> rumblingsMap = Collections.unmodifiableMap(Stream.of(new SimpleEntry<>("rental manager", ConceptType.GENERAL_CONCEPT),
            new SimpleEntry<>("rental manager's assistant", ConceptType.GENERAL_CONCEPT),
            new SimpleEntry<>("customer", ConceptType.GENERAL_CONCEPT),
            new SimpleEntry<>("consumer", ConceptType.GENERAL_CONCEPT),
            new SimpleEntry<>("car rental system", ConceptType.GENERAL_CONCEPT),
            new SimpleEntry<>("rental contracts", ConceptType.GENERAL_CONCEPT),
            new SimpleEntry<>("rental insurance", ConceptType.GENERAL_CONCEPT),
            new SimpleEntry<>("car reservation", ConceptType.GENERAL_CONCEPT),
            new SimpleEntry<>("John", ConceptType.GENERAL_CONCEPT),
            new SimpleEntry<>("car booking", ConceptType.GENERAL_CONCEPT),
            new SimpleEntry<>("walk-in car booking", ConceptType.GENERAL_CONCEPT),
            new SimpleEntry<>("personal data", ConceptType.GENERAL_CONCEPT),
            new SimpleEntry<>("additional personal data", ConceptType.GENERAL_CONCEPT),
            new SimpleEntry<>("sales manager", ConceptType.GENERAL_CONCEPT),
            new SimpleEntry<>("rental manager generalizes rental manager's assistant", ConceptType.VERB_CONCEPT),
            new SimpleEntry<>("rental managers create rental contract", ConceptType.VERB_CONCEPT),
            new SimpleEntry<>("rental manager's assistant created rental contract", ConceptType.VERB_CONCEPT),
            new SimpleEntry<>("rental manager's assistant manage rental insurance", ConceptType.VERB_CONCEPT),
            new SimpleEntry<>("rental manager's assistant confirm car reservation", ConceptType.VERB_CONCEPT),
            new SimpleEntry<>("customer make car booking", ConceptType.VERB_CONCEPT),
            new SimpleEntry<>("customer provide additional personal data", ConceptType.VERB_CONCEPT),
            new SimpleEntry<>("customer confirm rental contract", ConceptType.VERB_CONCEPT),
            new SimpleEntry<>("customer make walk-in car booking", ConceptType.VERB_CONCEPT),
            new SimpleEntry<>("rental contract is confirmed", ConceptType.VERB_CONCEPT),
            new SimpleEntry<>("additional personal data is required", ConceptType.VERB_CONCEPT),
            new SimpleEntry<>("sales manager is absent", ConceptType.VERB_CONCEPT),
            new SimpleEntry<>("It is obligatory that rental manager create rental contract if customer make car booking", ConceptType.BUSINESS_RULE),
            new SimpleEntry<>("It is obligatory that rental manager's assistant create a rental contract if customer makes car booking", ConceptType.BUSINESS_RULE),
            new SimpleEntry<>("It is obligatory that rental manager's assistant managed car insurance if rental manager create rental contract", ConceptType.BUSINESS_RULE),
            new SimpleEntry<>("It is obligatory that rental manager's assistant managed car insurance if rental manager's assistant creates rental contract", ConceptType.BUSINESS_RULE),
            new SimpleEntry<>("It is obligatory that customer confirm rental contract if rental managers create rental contract", ConceptType.BUSINESS_RULE),
            new SimpleEntry<>("It is obligatory that customer confirm rental contract if rental manager's assistant create rental contract", ConceptType.BUSINESS_RULE),
            new SimpleEntry<>("It is permitted that customer provide additional personal data if rental managers work with rental contract", ConceptType.BUSINESS_RULE),
            new SimpleEntry<>("It is permitted that customer provide additional personal data if rental manager's assistant create rental contract", ConceptType.BUSINESS_RULE),
            new SimpleEntry<>("It is permitted that rental manager's assistant confirm a car reservation if rental manager create rental contract", ConceptType.BUSINESS_RULE),
            new SimpleEntry<>("It is permitted that rental manager's assistant confirm car reservation if rental manager's assistant create rental contract", ConceptType.BUSINESS_RULE),
            new SimpleEntry<>("It is obligatory that consumer provide additional personal data if additional personal data is required", ConceptType.BUSINESS_RULE),
            new SimpleEntry<>("It is obligatory that rental manager's assistant confirm car reservation if rental contract is confirmed", ConceptType.BUSINESS_RULE),
            new SimpleEntry<>("It is obligatory that rental manager's assistant create rental contract if sales manager is absent", ConceptType.BUSINESS_RULE))
            .collect(Collectors.toMap((e) -> e.getKey(), (e) -> e.getValue())));

    private Collection<SBVRExpressionModel> executeExtractEntities(StepwiseCascadedExtractor extractor, Collection<String> strings) {
        try {
            Class clazz = extractor.getClass();
            Method extractEntities = clazz.getDeclaredMethod("extractEntities", Collection.class);
            extractEntities.setAccessible(true);
            Object res = extractEntities.invoke(extractor, strings);
            return (Collection<SBVRExpressionModel>) res;
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            Logger.getLogger(SBVRExtractionTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private Collection<SBVRExpressionModel> executeSynonymsFinder(StepwiseCascadedExtractor extractor, Collection<SBVRExpressionModel> sbvr) {
        try {
            Class clazz = extractor.getClass();
            Method extractEntities = clazz.getDeclaredMethod("identifySynonyms", Collection.class);
            extractEntities.setAccessible(true);
            Object res = extractEntities.invoke(extractor, sbvr);
            return (Collection<SBVRExpressionModel>) res;
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            Logger.getLogger(SBVRExtractionTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    @Test
    public void testActorsOnly() throws InitializationException {
        List<String> strings = Arrays.asList("rental manager", "rental manager’s assistant", "customer", "car rental system",
                "rental contract", "John", "car reservation", "car booking", "walk-in car booking",
                "Microsoft", "personal data", "rental contract", "additional personal data", "sales manager");
        StepwiseCascadedExtractor extractor = new StepwiseCascadedExtractor(null, NamedEntityFinder.getInstance(), SentenceFactory.getInstance());
        Collection<SBVRExpressionModel> sbvrEntities = executeExtractEntities(extractor, strings);
        assertNotNull(sbvrEntities);
        int count_gc = 0, count_ic = 0;
        ExpressionType john_ic = ExpressionType.GENERIC;
        for (SBVRExpressionModel sbvr : sbvrEntities) {
            if (sbvr.getExpressionType(0) == ExpressionType.GENERAL_CONCEPT)
                count_gc += 1;
            else if (sbvr.getExpressionType(0) == ExpressionType.INDIVIDUAL_CONCEPT)
                count_ic += 1;
            if (sbvr.getExpressionElement(0).compareTo("John") == 0)
                john_ic = sbvr.getExpressionType(0);
        }
        assertEquals(11, count_gc);
        assertEquals(2, count_ic);
        assertEquals(ExpressionType.INDIVIDUAL_CONCEPT, john_ic);
    }

    @Test
    public void testActorsOnlyWithSynonyms() throws InitializationException {
        List<String> strings = Arrays.asList("rental manager", "rental manager’s assistant", "customer", "consumer",
                "rental contract", "John", "car reservation", "car booking", "walk-in car booking",
                "Microsoft", "personal data", "rental contract", "additional personal data", "sales manager");
        StepwiseCascadedExtractor extractor = new StepwiseCascadedExtractor(null, NamedEntityFinder.getInstance(), SentenceFactory.getInstance());
        Collection<SBVRExpressionModel> sbvrEntities = executeExtractEntities(extractor, strings);
        sbvrEntities = executeSynonymsFinder(extractor, sbvrEntities);
        int count_gc = 0, count_ic = 0;
        SBVRExpressionModel customer_gc = null;
        for (SBVRExpressionModel sbvr : sbvrEntities) {
            if (sbvr.getExpressionType(0) == ExpressionType.GENERAL_CONCEPT)
                count_gc += 1;
            else if (sbvr.getExpressionType(0) == ExpressionType.INDIVIDUAL_CONCEPT)
                count_ic += 1;
            if (sbvr.toString().equals("customer") || sbvr.toString().equals("consumer"))
                customer_gc = sbvr;
        }
        assertEquals(10, count_gc);
        assertEquals(2, count_ic);
        assertNotNull(customer_gc);
        assertEquals(1, customer_gc.getSynonymousForms().size());
        String syn_name = customer_gc.toString().equals("customer") ? "consumer" : "customer";
        assertEquals(syn_name, customer_gc.getSynonymousForms().get(0).getExpressionElement(0));
    }

    @Test
    public void testExtractWithSynonyms() throws InitializationException {
        StepwiseCascadedExtractor extractor = new StepwiseCascadedExtractor(rumblingsMap,
                NamedEntityFinder.getInstance(), SentenceFactory.getInstance());
        extractor.setReplaceSynonyms(true);
        extractor.extract();
        Collection<SBVRExpressionModel> generalConcepts = extractor.getExtractedGeneralConcepts();
        Collection<SBVRExpressionModel> individualConcepts = extractor.getExtractedIndividualConcepts();
        Collection<SBVRExpressionModel> verbConcepts = extractor.getExtractedVerbConcepts();
        Collection<SBVRExpressionModel> businessRules = extractor.getExtractedBusinessRules();
        assertEquals(15, generalConcepts.size());
        assertEquals(1, individualConcepts.size());
        assertEquals(19, verbConcepts.size());
        assertEquals(13, businessRules.size());
    }

    @Test
    public void testExtractWithoutSynonyms() throws InitializationException {
        StepwiseCascadedExtractor extractor = new StepwiseCascadedExtractor(rumblingsMap,
                NamedEntityFinder.getInstance(), SentenceFactory.getInstance());
        extractor.setReplaceSynonyms(false);
        extractor.extract();
        Collection<SBVRExpressionModel> generalConcepts = extractor.getExtractedGeneralConcepts();
        Collection<SBVRExpressionModel> individualConcepts = extractor.getExtractedIndividualConcepts();
        Collection<SBVRExpressionModel> verbConcepts = extractor.getExtractedVerbConcepts();
        Collection<SBVRExpressionModel> businessRules = extractor.getExtractedBusinessRules();
        assertEquals(15, generalConcepts.size());
        assertEquals(1, individualConcepts.size());
        assertEquals(19, verbConcepts.size());
        assertEquals(13, businessRules.size());
    }

    @Test
    public void testExtractWithoutSynonymsNormalized() throws InitializationException {
        StepwiseCascadedExtractor extractor = new StepwiseCascadedExtractor(rumblingsMap,
                NamedEntityFinder.getInstance(), SentenceFactory.getInstance());
        extractor.setReplaceSynonyms(false);
        extractor.setUseNormalization(true);
        extractor.extract();
        Collection<SBVRExpressionModel> generalConcepts = extractor.getExtractedGeneralConcepts();
        Collection<SBVRExpressionModel> individualConcepts = extractor.getExtractedIndividualConcepts();
        Collection<SBVRExpressionModel> verbConcepts = extractor.getExtractedVerbConcepts();
        Collection<SBVRExpressionModel> businessRules = extractor.getExtractedBusinessRules();
        assertEquals(13, generalConcepts.size());
        assertEquals(1, individualConcepts.size());
        System.out.println(verbConcepts);
        assertEquals(16, verbConcepts.size());
        assertEquals(13, businessRules.size());
    }

    @Test
    public void testExtractSentence() throws InitializationException {
        Map<String, ConceptType> rumblings = Collections.unmodifiableMap(Stream.of(new SimpleEntry<>(
                "It is permitted that rental manager's assistant confirm a car reservation if rental manager create rental contract",
                        ConceptType.BUSINESS_RULE))
                .collect(Collectors.toMap((e) -> e.getKey(), (e) -> e.getValue())));
        StepwiseCascadedExtractor extractor = new StepwiseCascadedExtractor(rumblings,
                NamedEntityFinder.getInstance(), SentenceFactory.getInstance());
        extractor.setUseNormalization(true);
        extractor.extract();
        Collection<SBVRExpressionModel> businessRules = extractor.getExtractedBusinessRules();
        assertEquals(1, businessRules.size());
        assertEquals("It is permitted that rental manager's assistant confirms car reservation if rental manager creates rental contract", 
                businessRules.toArray()[0].toString());
    }

    @Test
    public void testExtractNumber() throws InitializationException {
        Map<String, ConceptType> rumblings = Collections.unmodifiableMap(Stream.of(new SimpleEntry<>("30 books", ConceptType.GENERAL_CONCEPT))
                .collect(Collectors.toMap((e) -> e.getKey(), (e) -> e.getValue())));
        StepwiseCascadedExtractor extractor = new StepwiseCascadedExtractor(rumblings,
                NamedEntityFinder.getInstance(), SentenceFactory.getInstance());
        extractor.extract();
        Collection<SBVRExpressionModel> general = extractor.getExtractedGeneralConcepts();
        assertEquals(1, general.size());
    }

    @Test
    public void testExtractNumber2() throws InitializationException {
        Map<String, ConceptType> rumblings = Collections.unmodifiableMap(Stream.of(new SimpleEntry<>("car no. 15", ConceptType.GENERAL_CONCEPT))
                .collect(Collectors.toMap((e) -> e.getKey(), (e) -> e.getValue())));
        StepwiseCascadedExtractor extractor = new StepwiseCascadedExtractor(rumblings,
                NamedEntityFinder.getInstance(), SentenceFactory.getInstance());
        extractor.extract();
        Collection<SBVRExpressionModel> general = extractor.getExtractedIndividualConcepts();
        // "No." is recognized as SYM! But 15 is recognized as CGC
        assertEquals(1, general.size());
    }

    @Test
    public void testExtractAbbreviation() throws InitializationException {
        Map<String, ConceptType> rumblings = Collections.unmodifiableMap(Stream.of(new SimpleEntry<>("USA", ConceptType.GENERAL_CONCEPT))
                .collect(Collectors.toMap((e) -> e.getKey(), (e) -> e.getValue())));
        StepwiseCascadedExtractor extractor = new StepwiseCascadedExtractor(rumblings,
                NamedEntityFinder.getInstance(), SentenceFactory.getInstance());
        extractor.extract();
        Collection<SBVRExpressionModel> general = extractor.getExtractedIndividualConcepts();
        assertEquals(1, general.size());
    }

    @Test
    public void testExtractAbbreviation2() throws InitializationException {
        Map<String, ConceptType> rumblings = Collections.unmodifiableMap(Stream.of(new SimpleEntry<>("U.S.A", ConceptType.GENERAL_CONCEPT))
                .collect(Collectors.toMap((e) -> e.getKey(), (e) -> e.getValue())));
        StepwiseCascadedExtractor extractor = new StepwiseCascadedExtractor(rumblings,
                NamedEntityFinder.getInstance(), SentenceFactory.getInstance());
        extractor.extract();
        // Surprisingly, POS of U.S.A is [NN] (general concept). However, it is recognized as location, thus is IC 
        Collection<SBVRExpressionModel> general = extractor.getExtractedIndividualConcepts();
        assertEquals(1, general.size());
    }

    @Test
    public void testExtractDate() throws InitializationException {
        Map<String, ConceptType> rumblings = Collections.unmodifiableMap(Stream.of(new SimpleEntry<>("March 15", ConceptType.GENERAL_CONCEPT))
                .collect(Collectors.toMap((e) -> e.getKey(), (e) -> e.getValue())));
        StepwiseCascadedExtractor extractor = new StepwiseCascadedExtractor(rumblings,
                NamedEntityFinder.getInstance(), SentenceFactory.getInstance());
        extractor.extract();
        Collection<SBVRExpressionModel> general = extractor.getExtractedIndividualConcepts();
        assertEquals(1, general.size());
    }

    @Test
    public void testExtractDate2() throws InitializationException {
        Map<String, ConceptType> rumblings = Collections.unmodifiableMap(Stream.of(new SimpleEntry<>("2016-09-25", ConceptType.GENERAL_CONCEPT))
                .collect(Collectors.toMap((e) -> e.getKey(), (e) -> e.getValue())));
        StepwiseCascadedExtractor extractor = new StepwiseCascadedExtractor(rumblings,
                NamedEntityFinder.getInstance(), SentenceFactory.getInstance());
        extractor.extract();
        Collection<SBVRExpressionModel> general = extractor.getExtractedIndividualConcepts();
        assertEquals(1, general.size());
    }

    @Test
    public void testExtractDate3() throws InitializationException {
        Map<String, ConceptType> rumblings = Collections.unmodifiableMap(Stream.of(new SimpleEntry<>("2016.09.25", ConceptType.GENERAL_CONCEPT))
                .collect(Collectors.toMap((e) -> e.getKey(), (e) -> e.getValue())));
        StepwiseCascadedExtractor extractor = new StepwiseCascadedExtractor(rumblings,
                NamedEntityFinder.getInstance(), SentenceFactory.getInstance());
        extractor.extract();
        Collection<SBVRExpressionModel> general = extractor.getExtractedIndividualConcepts();
        assertEquals(1, general.size());
    }

    @Test
    public void testExtractTime() throws InitializationException {
        Map<String, ConceptType> rumblings = Collections.unmodifiableMap(Stream.of(new SimpleEntry<>("23:25:15", ConceptType.GENERAL_CONCEPT))
                .collect(Collectors.toMap((e) -> e.getKey(), (e) -> e.getValue())));
        StepwiseCascadedExtractor extractor = new StepwiseCascadedExtractor(rumblings,
                NamedEntityFinder.getInstance(), SentenceFactory.getInstance());
        extractor.extract();
        Collection<SBVRExpressionModel> general = extractor.getExtractedIndividualConcepts();
        assertEquals(1, general.size());
    }

    @Test
    public void testTrinaryConcepts() throws InitializationException {
        Map<String, ConceptType> rumblings = Collections.unmodifiableMap(Stream.of(
                new SimpleEntry<>("John works at home using remote technologies", ConceptType.VERB_CONCEPT))
                .collect(Collectors.toMap((e) -> e.getKey(), (e) -> e.getValue())));
        StepwiseCascadedExtractor extractor = new StepwiseCascadedExtractor(rumblings,
                NamedEntityFinder.getInstance(), SentenceFactory.getInstance());
        extractor.extract();
        Collection<SBVRExpressionModel> general = extractor.getExtractedGeneralConcepts();
        assertEquals(2, general.size());
        Collection<SBVRExpressionModel> individual = extractor.getExtractedIndividualConcepts();
        assertEquals(1, individual.size());
        Collection<SBVRExpressionModel> verbs = extractor.getExtractedVerbConcepts();
        assertEquals(1, verbs.size());
    }

    @Test
    public void testTrinaryConcepts2() throws InitializationException {
        Map<String, ConceptType> rumblings = Collections.unmodifiableMap(Stream.of(
                new SimpleEntry<>("User preset Telescope to Catalogue Object", ConceptType.VERB_CONCEPT))
                .collect(Collectors.toMap((e) -> e.getKey(), (e) -> e.getValue())));
        StepwiseCascadedExtractor extractor = new StepwiseCascadedExtractor(rumblings,
                NamedEntityFinder.getInstance(), SentenceFactory.getInstance());
        extractor.extract();
        Collection<SBVRExpressionModel> general = extractor.getExtractedGeneralConcepts();
        assertEquals(1, general.size());
        assertEquals("User", general.iterator().next().toString());
        Collection<SBVRExpressionModel> individual = extractor.getExtractedIndividualConcepts();
        assertEquals(1, individual.size());
        assertEquals("Catalogue Object", individual.iterator().next().toString());
        Collection<SBVRExpressionModel> verbs = extractor.getExtractedVerbConcepts();
        assertEquals(0, verbs.size());  /// "preset" is tagged as adjective!
    }

    @Test
    public void testTrinaryConcepts3() throws InitializationException {
        Map<String, ConceptType> rumblings = Collections.unmodifiableMap(Stream.of(
                new SimpleEntry<>("User preset telescope to catalogue object", ConceptType.VERB_CONCEPT))
                .collect(Collectors.toMap((e) -> e.getKey(), (e) -> e.getValue())));
        StepwiseCascadedExtractor extractor = new StepwiseCascadedExtractor(rumblings,
                NamedEntityFinder.getInstance(), SentenceFactory.getInstance());
        extractor.extract();
        Collection<SBVRExpressionModel> general = extractor.getExtractedGeneralConcepts();
        assertEquals(2, general.size());
        TreeSet<String> genStr = general.stream().map(SBVRExpressionModel::toString).collect(Collectors.toCollection(TreeSet::new));
        Iterator iter = genStr.iterator();
        assertEquals("User", iter.next());
        assertEquals("catalogue object", iter.next());
        Collection<SBVRExpressionModel> individual = extractor.getExtractedIndividualConcepts();
        assertEquals(0, individual.size());
        Collection<SBVRExpressionModel> verbs = extractor.getExtractedVerbConcepts();
        assertEquals(0, verbs.size());  /// "preset" is tagged as adjective!
    }
    
    @Test
    public void testTrinaryConcepts4() throws InitializationException {
        Map<String, ConceptType> rumblings = Collections.unmodifiableMap(Stream.of(
                new SimpleEntry<>("User presets telescope to catalogue object", ConceptType.VERB_CONCEPT))
                .collect(Collectors.toMap((e) -> e.getKey(), (e) -> e.getValue())));
        StepwiseCascadedExtractor extractor = new StepwiseCascadedExtractor(rumblings,
                NamedEntityFinder.getInstance(), SentenceFactory.getInstance());
        extractor.extract();
        Collection<SBVRExpressionModel> general = extractor.getExtractedGeneralConcepts();
        assertEquals(3, general.size());
        Collection<SBVRExpressionModel> individual = extractor.getExtractedIndividualConcepts();
        assertEquals(0, individual.size());
        Collection<SBVRExpressionModel> verbs = extractor.getExtractedVerbConcepts();
        assertEquals(1, verbs.size());
    }
    
    @Test
    public void testNormalizeGeneralConcept() throws InitializationException {
        Map<String, ConceptType> rumblings = Collections.unmodifiableMap(Stream.of(new SimpleEntry<>("rental managers", ConceptType.GENERAL_CONCEPT))
                .collect(Collectors.toMap((e) -> e.getKey(), (e) -> e.getValue())));
        StepwiseCascadedExtractor extractor = new StepwiseCascadedExtractor(rumblings,
                NamedEntityFinder.getInstance(), SentenceFactory.getInstance());
        extractor.setUseNormalization(true);
        extractor.extract();
        Collection<SBVRExpressionModel> general = extractor.getExtractedGeneralConcepts();
        assertEquals(1, general.size());
        assertEquals("rental manager", general.toArray()[0].toString());
    }

    @Test
    public void testNormalizeVerbConcept() throws InitializationException {
        Map<String, ConceptType> rumblings = Collections.unmodifiableMap(Stream.of(new SimpleEntry<>("John worked at home", ConceptType.VERB_CONCEPT))
                .collect(Collectors.toMap((e) -> e.getKey(), (e) -> e.getValue())));
        StepwiseCascadedExtractor extractor = new StepwiseCascadedExtractor(rumblings,
                NamedEntityFinder.getInstance(), SentenceFactory.getInstance());
        extractor.setUseNormalization(true);
        extractor.extract();
        Collection<SBVRExpressionModel> general = extractor.getExtractedVerbConcepts();
        assertEquals(1, general.size());
        assertEquals("John works at home", general.toArray()[0].toString());
    }

    @Test
    public void testExtractGeneralConcept() throws InitializationException {
        Map<String, ConceptType> rumblings = Collections.unmodifiableMap(Stream.of(new SimpleEntry<>("car booking", ConceptType.GENERAL_CONCEPT))
                .collect(Collectors.toMap((e) -> e.getKey(), (e) -> e.getValue())));
        StepwiseCascadedExtractor extractor = new StepwiseCascadedExtractor(rumblings,
                NamedEntityFinder.getInstance(), SentenceFactory.getInstance());
        extractor.extract();
        Collection<SBVRExpressionModel> general = extractor.getExtractedGeneralConcepts();
        assertEquals(1, general.size());
    }
    
    @Test
    public void extractVerbConcept() throws InitializationException {
        Map<String, ConceptType> rumblings = Collections.unmodifiableMap(Stream.of(new SimpleEntry<>("customer buy on-sale tickets", ConceptType.VERB_CONCEPT))
                .collect(Collectors.toMap((e) -> e.getKey(), (e) -> e.getValue())));
        StepwiseCascadedExtractor extractor = new StepwiseCascadedExtractor(rumblings,
                NamedEntityFinder.getInstance(), SentenceFactory.getInstance());
        // extractor.setUseNormalization(true);
        extractor.extract();
        Collection<SBVRExpressionModel> general = extractor.getExtractedGeneralConcepts();
        assertEquals(2, general.size());
        Collection<SBVRExpressionModel> verbs = extractor.getExtractedVerbConcepts();
        assertEquals(1, verbs.size());
        System.out.println(general);
        System.out.println(verbs);
        assertEquals("customer buys one or more on-sale ticket", verbs.toArray()[0].toString());
    }
    
    @Test
    public void testExtractCase() throws InitializationException {
        Map<String, ConceptType> rumblingsMap = Collections.unmodifiableMap(Stream.of(
            new SimpleEntry<>("credit card validation system", ConceptType.GENERAL_CONCEPT),
            new SimpleEntry<>("customer", ConceptType.GENERAL_CONCEPT),
            new SimpleEntry<>("customer winning ticket entered", ConceptType.VERB_CONCEPT),
            new SimpleEntry<>("customer buy team merchandise", ConceptType.VERB_CONCEPT),
            new SimpleEntry<>("customer buy on-sale tickets", ConceptType.VERB_CONCEPT),
            new SimpleEntry<>("customer purchase using hockey team card", ConceptType.VERB_CONCEPT),
            new SimpleEntry<>("customer purchase with credit card", ConceptType.VERB_CONCEPT),
            new SimpleEntry<>("customer perform transaction", ConceptType.VERB_CONCEPT),
            new SimpleEntry<>("winning ticket was entered", ConceptType.VERB_CONCEPT),
            new SimpleEntry<>("credit card validation system purchase with credit card", ConceptType.VERB_CONCEPT),
            new SimpleEntry<>("It is possible that customer winning ticket entered if winning ticket was entered and customer buy team merchandise", ConceptType.BUSINESS_RULE),
            new SimpleEntry<>("It is obligatory that customer perform transaction if customer buy tickets", ConceptType.BUSINESS_RULE),
            new SimpleEntry<>("It is obligatory that customer perform transaction if customer buy on-sale tickets", ConceptType.BUSINESS_RULE),
            new SimpleEntry<>("It is obligatory that customer perform transaction if customer buy team merchandise", ConceptType.BUSINESS_RULE))
            .collect(Collectors.toMap((e) -> e.getKey(), (e) -> e.getValue())));
        System.out.println("Extraction using Stanford POS tagger");
        StepwiseCascadedExtractor extractor = new StepwiseCascadedExtractor(rumblingsMap, NamedEntityFinder.getInstance(), SentenceFactory.getInstance());
        extractor.setReplaceSynonyms(false);
        extractor.setUseNormalization(true);
        //extractor.extract();
        Collection<SBVRExpressionModel> generalConcepts = extractor.getExtractedGeneralConcepts();
        Collection<SBVRExpressionModel> verbConcepts = extractor.getExtractedVerbConcepts();
        Collection<SBVRExpressionModel> businessRules = extractor.getExtractedBusinessRules();
        System.out.println(generalConcepts);
        System.out.println(verbConcepts);
        System.out.println(businessRules);
        
        System.out.println("Extraction using custom POS tagger");
        POSTagger tagger = Taggers.getCustomStanfordTagger();
        extractor.setTagger(tagger);
        extractor.extract();
        generalConcepts = extractor.getExtractedGeneralConcepts();
        verbConcepts = extractor.getExtractedVerbConcepts();
        businessRules = extractor.getExtractedBusinessRules();
        System.out.println(generalConcepts);
        System.out.println(verbConcepts);
        System.out.println(businessRules);
    }

}
