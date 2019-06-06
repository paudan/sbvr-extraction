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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.tmine.entities.Entity;
import net.tmine.entities.Entity.EntityType;
import net.tmine.entities.InitializationException;
import net.tmine.entities.Sentence;
import net.tmine.entities.SentenceFactory;
import net.tmine.entities.VerbWord;
import net.tmine.entities.Word;
import org.ktu.isd.extraction.SBVRExpressionModel.ExpressionType;
import org.ktu.isd.extraction.SBVRExpressionModel.QuantifierType;
import net.tmine.processing.NamedEntityFinder;
import net.tmine.processing.POSTagger;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.lang3.StringUtils;
import simplenlg.features.Tense;

public class StepwiseCascadedExtractor extends AbstractVocabularyExtractor {

    protected static final String RECOGNIZED_GC = "CGC";
    protected static final String RECOGNIZED_IC = "CIC";
    protected static final String RECOGNIZED_VC = "CVC";
    protected static final String RECOGNIZED_BR = "CBR";

    protected static final String[] TAGS_VERB = new String[]{"VB", "VBD", "VBG", "VBN", "VBP", "VBZ"};

    protected static final Set<String> IGNORED = new HashSet<>(Arrays.asList("$", "(", ")", ",", ".", ":", "DT", "PDT"));

    protected static final String POS_GC_MULTIPLE = "NNS";
    protected static final String POS_IC_MULTIPLE = "NNPS";
    // Characteristics is represented as as adjective
    protected static final String CHAR_COND = "[JJ]|[JJR]|[JJS]";
    protected static final String PREP_COND = "[RB]|[RBR]|[RP]|[TO]|[IN]|[PREP]";
    protected static final String ADJ_COND = "[ADJ]|" + CHAR_COND;
    protected static final String GC_SINGLE_COND = "[NN]|[VBG]|[POS]|[RBS]|[FW]";
    protected static final String GC_COND = String.format("%s|[%s]", GC_SINGLE_COND, POS_GC_MULTIPLE);
    protected static final String IC_SINGLE_COND = "[NNP]|[POS]|[CD]";
    protected static final String IC_COND = String.format("%s|[%s]", IC_SINGLE_COND, POS_IC_MULTIPLE);
    protected static final String VERB_FORM = String.format("[%s]", String.join("]|[", TAGS_VERB));
    protected static final String VERB_COND = VERB_FORM + "|" + PREP_COND;

    protected static final String PATTERN_GC = String.format("(%s)*(%s)*(%s)+", ADJ_COND, IC_COND, GC_COND);
    protected static final String PATTERN_IC = String.format("(%s)*(%s)*(%s)+", ADJ_COND, GC_COND, IC_COND);
    protected static final String PATTERN_GC_IC = String.format("(%s)*(%s|%s)+", ADJ_COND, GC_COND, IC_COND);
    protected static String PATTERN_COMBINED_GC_IC = String.format("([%s]|[%s]){1}|%s",
            RECOGNIZED_GC, RECOGNIZED_IC, PATTERN_GC_IC);

    protected static String PATTERN_UNARY_VC = String.format("(%s){1}(%s)+(%s)*", PATTERN_COMBINED_GC_IC, VERB_COND, CHAR_COND);
    protected static String PATTERN_BINARY_VC = String.format("(%s){1}(%s){1}", PATTERN_UNARY_VC, PATTERN_COMBINED_GC_IC);
    protected static String PATTERN_MULTINARY_VC = String.format("(%s){2,}(%s){1}", PATTERN_UNARY_VC, PATTERN_COMBINED_GC_IC);

    protected static String PATTERN_COMBINED_VC = String.format("[%s]|(%s){1}(%s)*(%s)*",
            RECOGNIZED_VC, PATTERN_UNARY_VC, PATTERN_UNARY_VC, PATTERN_COMBINED_GC_IC);

    protected static String PATTERN_BR = String.format("[PRP][VBZ]([VBN]|[JJ]){1}[WDT](%s)([IF_TAG](%s)*)([CC]%s)*",
            PATTERN_COMBINED_VC, PATTERN_COMBINED_VC, PATTERN_COMBINED_VC);
    protected static String PATTERN_BR_2 = String.format("[CBR]([IF_TAG][%s])*([CC][%s])*", RECOGNIZED_VC, RECOGNIZED_VC);

    // IOB tags for BV and BR tagging
    public enum IOBTag {
        IOB_CGC_START("S_CGC"), IOB_CGC_MIDDLE("I_CGC"), IOB_CGC_END("F_CGC"),
        IOB_CIC_START("S_CIC"), IOB_CIC_MIDDLE("I_CIC"), IOB_CIC_END("F_CIC"),
        IOB_CVC_START("S_CVC"), IOB_CVC_MIDDLE("I_CVC"), IOB_CVC_END("F_CVC"),
        IF_COND("IF_COND"), CONJUNCTION("CONJ"), DISJUNCTION("DISJ");

        private String typeString;

        private IOBTag(String typeString) {
            this.typeString = typeString;
        }

        public String getTypeString() {
            return typeString;
        }

    }

    /**
     * Situations when POS identification output must be overridden in any of the algorithmic phases
     */
    private DualHashBidiMap<String, String> overrides;
    /**
     * Whether replacement with synonyms should be enabled 
     */
    private boolean replaceSynonyms = Boolean.FALSE;
    protected boolean searchForVerbs = Boolean.TRUE;

    private Map<String, Set<SBVRExpressionModel>> conceptsByRumbling = new HashMap<>();

    protected NamedEntityFinder finder;
    protected SentenceFactory sentFactory;
    protected POSTagger tagger;
    // Replacements which should be applied in case of wrong POS recognition (e.g., when POS tagger identified as NN, while it was actually VB) 
    protected Map<String, PosReplacementData> wrongPosReplacements;
    

    protected static class ProcessedStructure {

        private String rumbling;
        private Sentence sentence;
        private ConceptType type;
        private List<String> candPos = new ArrayList<>(), candToken = new ArrayList<>();
        private List<String> originalToken = new ArrayList<>();
        private Map<String, SBVRExpressionModel> identified = new HashMap<>();

        public ProcessedStructure(String rumbling, Sentence sentence, ConceptType type) {
            this.rumbling = rumbling;
            this.sentence = sentence;
            this.type = type;
        }

        public void process() {
            for (Word word : sentence) {
                candPos.add(word.getPOS());
                candToken.add(word.getToken());
                originalToken.add(word.getToken());
            }
        }

        public Sentence getSentence() {
            return sentence;
        }

        public void setSentence(Sentence sentence) {
            this.sentence = sentence;
        }

        public List<String> getPosList() {
            return candPos;
        }

        public void setPosList(List<String> candPos) {
            this.candPos = candPos;
        }

        public List<String> getTokenList() {
            return candToken;
        }

        public void setTokenList(List<String> candToken) {
            this.candToken = candToken;
        }

        public String toString() {
            return "Rumbling: " + rumbling + "\nSentence:" + sentence + "\n" + candPos + " => " + candToken;
        }

        public String getTagString() {
            StringBuilder builder = new StringBuilder();
            for (String tag : candPos)
                builder.append(String.format("[%s]", tag));
            return builder.toString();
        }

    }

    protected static class PosReplacementData {

        String rumbling;
        List<String> originalPOS, newPOS, originalToken, newToken;

        public PosReplacementData(String rumbling, List<String> originalPOS, List<String> newPOS,
                List<String> originalToken, List<String> newToken) {
            this.rumbling = rumbling;
            this.originalPOS = originalPOS;
            this.newPOS = newPOS;
            this.originalToken = originalToken;
            this.newToken = newToken;
        }

        @Override
        public String toString() {
            return "Rumbling :" + rumbling + "\nOriginal POS:" + originalPOS + "\nNew POS:" + newPOS
                    + "\nOriginal token:" + originalToken + "\nNew Token:" + newToken;
        }

    }

    public StepwiseCascadedExtractor(Map<String, ConceptType> rumblings) {
        this(rumblings, null, null);
    }

    public StepwiseCascadedExtractor(Map<String, ConceptType> rumblings,
            NamedEntityFinder finder, SentenceFactory factory) {
        super(rumblings);
        this.finder = finder;
        this.sentFactory = factory;
        // Must enforce to set longer phrases at the beginning of the list
        wrongPosReplacements = new TreeMap<>(StepwiseCascadedExtractor.getDefaultComparator());
        initOverrides();
    }
    
    public StepwiseCascadedExtractor(NamedEntityFinder finder, SentenceFactory factory) {
        this(null, finder, factory);
    }

    private void initOverrides() {
        overrides = new DualHashBidiMap<>();
        // These phrases may be recognized as verb concepts, thus they are ignored 
        // "that" can be tagged differently by original and custom taggers!
        overrides.put("It is obligatory that", "RULE_TAG1");
        overrides.put("It is possible that", "RULE_TAG2");
        overrides.put("It is permitted that", "RULE_TAG3");
        overrides.put("It is required that", "RULE_TAG4");
        overrides.put("if", "IF_TAG");
        // Original pattern is overriden to conform to these exclusions
        PATTERN_BR = String.format("([RULE_TAG1]|[RULE_TAG2]|[RULE_TAG3]|[RULE_TAG4]){1}(%s)([IF_TAG](%s))*([CC]%s)*",
                PATTERN_COMBINED_VC, PATTERN_COMBINED_VC, PATTERN_COMBINED_VC);
    }

    public NamedEntityFinder getNamedEntityFinder() {
        return finder;
    }

    public void setNamedEntityFinder(NamedEntityFinder finder) {
        this.finder = finder;
    }

    public SentenceFactory getSentenceFactory() {
        return sentFactory;
    }

    public void setSentenceFactor(SentenceFactory sentFactory) {
        this.sentFactory = sentFactory;
    }

    public POSTagger getTagger() {
        return tagger;
    }

    public void setTagger(POSTagger tagger) {
        this.tagger = tagger;
    }

    public boolean replaceSynonyms() {
        return replaceSynonyms;
    }

    public void setReplaceSynonyms(boolean aReplaceSynonyms) {
        replaceSynonyms = aReplaceSynonyms;
    }

    public boolean useNormalization() {
        return normalize;
    }

    @Override
    protected void clearResultStructures() {
        super.clearResultStructures();
        wrongPosReplacements.clear();
    }

    private Sentence preprocessSentence(Sentence sent) {
        sent.preprocess();
        Iterator<Word> it = sent.iterator();
        finder.findAllEntities(sent);
        while (it.hasNext()) {
            Word w = it.next();
            // Remove words which have tags that should be ignored
            if (IGNORED.contains(w.getPOS()))
                it.remove();
            else {
                List<Entity> entities = new ArrayList<>(w.getEntities());
                // Some concepts (like U.S.A) might still be recognized as GC, must use NER
                if (entities.isEmpty())
                    entities.addAll(finder.findAllEntities(sentFactory.createSentence(w.getToken(), tagger, false, null)));
                // If the word is part of any NER set its POS as [NNP]
                if (!entities.isEmpty() && !Stream.of(entities.toArray(new Entity[]{}))
                        .anyMatch(e -> e.getEntityType().equals(EntityType.GENERAL)))
                    w.setPOS("NNP");
            }
        }
        return sent;
    }

    private void modifySublist(List<String> source, List<String> modification, int index, int num) {
        List<String> subList = source.subList(index, index + num);
        List<String> sublist = new ArrayList<>();
        sublist.addAll(modification);
        subList.clear();
        subList.addAll(sublist);
    }

    protected void replaceExtracted(List<ProcessedStructure> updated) {
        for (ProcessedStructure cand : updated) {
            String rumbling = cand.sentence.toString();
            for (Entry<String, PosReplacementData> replacement : wrongPosReplacements.entrySet())
                if (rumbling.contains(replacement.getKey())) {
                    PosReplacementData changeData = replacement.getValue();
                    if (changeData != null) {
                        boolean modified = false;
                        int index = Collections.indexOfSubList(cand.candToken, changeData.originalToken);
                        if (index > -1) {
                            int origSize = changeData.originalToken.size();
                            modifySublist(cand.candToken, changeData.newToken, index, origSize);
                            modifySublist(cand.candPos, changeData.newPOS, index, origSize);
                            modified = true;
                        }
                        // It is possible that concatenation of original token is present (for BR case)
                        if (!modified) {
                            String origTokenString = String.join(" ", changeData.originalToken);
                            index = Collections.indexOfSubList(cand.candToken, Arrays.asList(origTokenString));
                            if (index > -1) {
                                modifySublist(cand.candToken, changeData.newToken, index, 1);
                                modifySublist(cand.candPos, changeData.newPOS, index, 1);
                            }
                        }
                    }
                    // Remove incorrectly identified concepts    
                    generalConcepts.remove(replacement.getKey());
                    individualConcepts.remove(replacement.getKey());
                }
            tagWithRecognizedGC(cand);
        }
    }

    protected List<ProcessedStructure> preprocessStructures() throws InitializationException {
        if (sentFactory == null)
            throw new InitializationException(SentenceFactory.class);
        if (finder == null)
            throw new InitializationException(NamedEntityFinder.class);
        // First, select general concepts which are extracted automatically, as general concept candidates
        Set<String> gcCandidates = rumblings.entrySet().stream()
                .filter(map -> map.getValue().equals(ConceptType.GENERAL_CONCEPT) && map.getKey().trim().length() > 0)
                .collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue())).keySet();
        clearResultStructures();
        Collection<SBVRExpressionModel> gcList = extractEntities(gcCandidates);
        gcList = identifySynonyms(gcList);

        List<ProcessedStructure> updated = new LinkedList<>();
        // Add general concepts
        for (String str : gcCandidates) {
            Sentence sent = sentFactory.createSentence(str, tagger, false, "NN");
            sent = preprocessSentence(sent);
            updated.add(new ProcessedStructure(str, sent, ConceptType.GENERAL_CONCEPT));
        }
        // Update remaining rumblings by replacing synonymous forms (beware: this should be optional!)
        Map<String, String> replacements = new HashMap<>();
        for (Entry<String, ConceptType> entry : rumblings.entrySet())
            if (!entry.getValue().equals(ConceptType.GENERAL_CONCEPT))
                updated.add(new ProcessedStructure(entry.getKey(), null, entry.getValue()));
        if (replaceSynonyms)
            replacements = createReplacementMap(gcList);
        for (ProcessedStructure other : updated) {
            String str = other.rumbling;
            if (replaceSynonyms)
                for (Entry<String, String> rep : replacements.entrySet())
                    str = str.replace(rep.getKey(), rep.getValue());
            // Replace overrides using their tags (notice the existence of separator!)
            String replacement = str;
            for (String substr : overrides.keySet()) {
                replacement = replacement.replaceAll(String.format(" (?i)%s", substr),
                        String.format(" 0%s0", overrides.get(substr)));
                replacement = replacement.replaceAll(String.format("(?i)%s ", substr),
                        String.format("0%s0 ", overrides.get(substr)));
            }
            Sentence sent = sentFactory.createSentence(replacement, tagger, false, null);
            sent = preprocessSentence(sent);
            // Replace overrides with their tags
            for (Word wrd : sent) {
                String wordPhrase = wrd.getToken().replaceFirst("0", "").replaceFirst("0", "");
                // Check if word represents the tag from override
                if (overrides.containsValue(wordPhrase)) {
                    wrd.setPOS(wordPhrase);
                    wrd.setToken(overrides.getKey(wordPhrase));
                }
            }
            other.sentence = sent;
        }

        // Process existing concepts: identify IC and GC, filter out invalid concepts
        for (Entry<String, Set<SBVRExpressionModel>> rumblingEntry : conceptsByRumbling.entrySet())
            for (SBVRExpressionModel concept : rumblingEntry.getValue()) {
                String conceptName = concept.toString();
                Sentence sent = sentFactory.createSentence(concept.getExpressionElement(0), tagger, false, null);
                sent = preprocessSentence(sent);
                String tagString = sent.getTagString();
                boolean matched = false;
                Pattern full_gc = getCompiledPattern(String.format("^%s$", PATTERN_GC));
                Matcher m = full_gc.matcher(tagString);
                if (m.matches()) {
                    matched = true;
                    if (!generalConcepts.containsKey(conceptName)) {
                        generalConcepts.put(conceptName, concept);
                        addConceptToExtractedMap(rumblingEntry.getKey(), concept, ConceptType.GENERAL_CONCEPT, true);
                    }
                }
                if (!matched) {
                    Pattern full_ic = getCompiledPattern(String.format("^%s$", PATTERN_IC));
                    m = full_ic.matcher(tagString);
                    if (m.matches() && !individualConcepts.containsKey(conceptName)) {
                        concept.setExpressionType(0, ExpressionType.INDIVIDUAL_CONCEPT);
                        individualConcepts.put(conceptName, concept);
                        addConceptToExtractedMap(rumblingEntry.getKey(), concept, ConceptType.INDIVIDUAL_CONCEPT, true);
                    }
                }
            }
        for (ProcessedStructure cand : updated)
            cand.process();
        return updated;
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

        // Tag all except business rules and extract general and individual concepts
        for (ProcessedStructure cand : updated)
            if (!cand.type.equals(ConceptType.BUSINESS_RULE))
                tagWithRecognizedGC(cand);
        // Replace rumbling taggings with their corrections
        replaceExtracted(updated);
        // Retag business rules
        for (ProcessedStructure cand : updated)
            if (cand.type.equals(ConceptType.BUSINESS_RULE))
                tagWithRecognizedGC(cand);
        // Extract verb concepts
        for (ProcessedStructure cand : updated) {
            tagWithRecognizedVC(cand, vc_nary);
            tagWithRecognizedVC(cand, vc_binary);
            tagWithRecognizedVC(cand, vc_unary);
        }
        // Finally, extract business rules
        for (ProcessedStructure cand : updated)
            if (cand.type.equals(ConceptType.BUSINESS_RULE))
                tagWithRecognizedBR(cand, br_pattern);

        // Retag business rule rumblings again to meet changes in verb concepts
        replaceExtracted(updated);

        // Rerun BR extraction to reflect changs after retagging 
        for (ProcessedStructure cand : updated)
            if (cand.type.equals(ConceptType.BUSINESS_RULE)) {
                tagWithRecognizedBR(cand, br_pattern);
                tagWithRecognizedBR_2(cand);
                logger.debug("{}", cand);
            }

        logger.debug("General concepts: {}", generalConcepts);
        logger.debug("Verb concepts: {}", verbConcepts);
        logger.debug("Business rules: {}", businessRules);
    }
    
    
    protected Collection<SBVRExpressionModel> extractEntities(Collection<String> rumblings) throws InitializationException {
        assert sentFactory != null;
        if (finder == null)
            throw new InitializationException(NamedEntityFinder.class);
        Collection<SBVRExpressionModel> sbvrList = new ArrayList<>();
        // Remove duplicates
        Set<String> rumblingSet = new HashSet<>(rumblings);
        for (String rumbling : rumblingSet) {
            Sentence sent = sentFactory.createSentence(rumbling, tagger, true, null);
            if (normalize) {
                // Normalize last noun
                Word last = sent.get(sent.size() - 1);
                last.setToken(last.getLemma());
            }
            List<Entity> entities = finder.findAllEntities(sent);
            if (entities.isEmpty()) {
                // Remove underscore before apostrophes (related to [POS] tag)
                String conceptName = sent.toString().replaceAll(" '", "'");
                // Should normalize the text as well, but some POS taggers (like StanfordNLP) do not recognize every proper case correctly
                // Therefore, it is currently disabled
                //conceptName = conceptName.toLowerCase();
                SBVRExpressionModel concept = new SBVRExpressionModel().addGeneralConcept(conceptName, Boolean.TRUE);
                sbvrList.add(concept);
                conceptsByRumbling.put(rumbling, new HashSet<>(Arrays.asList(concept)));
            } else {
                Set<SBVRExpressionModel> concepts = new HashSet<>();
                for (Entity entity : entities) {
                    // Remove underscore before apostrophes (related to [POS] tag)
                    String conceptName = entity.getExpression().replaceAll(" '", "'");
                    SBVRExpressionModel concept = new SBVRExpressionModel().addIndividualConcept(conceptName, Boolean.TRUE);
                    sbvrList.add(concept);
                    concepts.add(concept);
                }
                conceptsByRumbling.put(rumbling, concepts);
            }
        }
        return sbvrList;
    }

    protected Collection<SBVRExpressionModel> identifySynonyms(Collection<SBVRExpressionModel> candidates) {
        assert candidates != null;
        Set<SBVRExpressionModel> checked = new HashSet<>(candidates);
        Map<String, SBVRExpressionModel> searchMap = new HashMap<>();
        for (SBVRExpressionModel cand : candidates)
            searchMap.put(cand.getExpressionElement(0), cand);
        Iterator<SBVRExpressionModel> itChecked = checked.iterator();
        // Filter out only general concepts for synonymous forms
        while (itChecked.hasNext()) {
            SBVRExpressionModel cand = itChecked.next();
            if (cand.length() > 1 || cand.getExpressionType(0) != SBVRExpressionModel.ExpressionType.GENERAL_CONCEPT)
                itChecked.remove();
        }
        for (SBVRExpressionModel cand : checked) {
            Set<String> synonyms;
            try {
                synonyms = sentFactory.getWordFactory().createWord(cand.getExpressionElement(0), "NN").getHypernyms();
            } catch (Exception ex) {
                synonyms = new HashSet<>();
            }
            if (synonyms.isEmpty())
                continue;
            for (SBVRExpressionModel currCand : checked)
                if (synonyms.contains(currCand.toString()) && currCand != cand) {
                    SBVRExpressionModel candObj = searchMap.get(cand.toString());
                    candObj.addSynonymousForm(currCand.clone());
                    searchMap.remove(currCand.toString());
                    // Modify sbvrConceptsByRumbling respectively (here, it's rather a quick fix than efficient)
                    for (String rumbling : conceptsByRumbling.keySet()) {
                        Set<SBVRExpressionModel> conceptsByRumbl = conceptsByRumbling.get(rumbling);
                        for (SBVRExpressionModel c : conceptsByRumbl)
                            // Find concept with the checked candidate name
                            if (c.getExpressionElement(0).equals(cand.getExpressionElement(0))) {
                                for (String r2 : conceptsByRumbling.keySet())
                                    for (SBVRExpressionModel c2 : conceptsByRumbling.get(r2))
                                        if (c2.equals(currCand)) {
                                            c2.addSynonymousForm(candObj.clone());
                                            break;
                                        }
                                conceptsByRumbl.remove(c);
                                break;
                            }
                        if (conceptsByRumbl.isEmpty())
                            conceptsByRumbling.remove(rumbling);
                    }
                }
        }
        return searchMap.values();
    }

    protected Map<String, String> createReplacementMap(Collection<SBVRExpressionModel> candidates) {
        assert candidates != null;
        Map<String, String> replacements = new HashMap<>();
        for (SBVRExpressionModel candidate : candidates) {
            List<SBVRExpressionModel> synonyms = candidate.getSynonymousForms();
            if (!synonyms.isEmpty())
                for (SBVRExpressionModel synonym : synonyms) {
                    String symWord = synonym.toString();
                    int numSenses = 1;
                    // We must also check that synonym has not any homonymous forms itself!
                    try {
                        numSenses = sentFactory.getWordFactory().createWord(symWord).getAllSynonyms().size();
                    } catch (Exception ex) {
                    }
                    // Check for possible homonyms in synonyms
                    if (!replacements.containsKey(symWord) && numSenses == 1)
                        replacements.put(symWord, candidate.toString());
                    else
                        // If there are homonyms, it is safer to ignore them
                        replacements.remove(symWord);
                }
        }
        return replacements;
    }

    protected Pattern getCompiledPattern(String pattern) {
        return Pattern.compile(pattern.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]"));
    }

    protected List<Integer> getBracketPositions(String tagString) {
        List<Integer> posBracket = new ArrayList<>();
        int index = -1;
        index = tagString.indexOf("]", index + 1);
        if (index != -1)
            posBracket.add(index);
        while (index != -1) {
            index = tagString.indexOf("]", index + 1);
            if (index == -1)
                break;
            posBracket.add(index);
        }
        return posBracket;
    }

    private void addIdentifiedConcept(ProcessedStructure candEntry, SBVRExpressionModel concept) {
        Map<String, SBVRExpressionModel> candList = candEntry.identified;
        if (candList == null)
            candList = new HashMap<>();
        candList.put(concept.toString(), concept);
    }

    private QuantifierType getQuantifier(String tagSub) {
        if (tagSub.endsWith(String.format("[%s]", POS_GC_MULTIPLE)) || tagSub.endsWith(String.format("[%s]", POS_IC_MULTIPLE)))
            return QuantifierType.QUANTIFIER_ONE_MORE;
        else
            return QuantifierType.QUANTIFIER_SINGLE;
    }

    private void setIOBTags(ProcessedStructure cand, List<Integer> indices,
            String startTag, String middleTag, String endTag) {
        if (indices == null || indices.isEmpty())
            return;
        assert cand != null && cand.sentence != null;
        Sentence sent = cand.sentence;
        if (sent.isEmpty() || indices.size() > sent.size())
            return;
        int maxInd = Collections.max(indices);
        int minInd = Collections.min(indices);
        if (minInd < 0 || maxInd >= sent.size())
            return;
        if (sent.size() == 1) {
            sent.get(0).setIOB(middleTag);
            return;
        }
        for (Integer ind : indices)
            if (ind == minInd)
                sent.get(ind).setIOB(startTag);
            else if (ind == maxInd)
                sent.get(ind).setIOB(endTag);
            else
                sent.get(ind).setIOB(middleTag);
    }

    private String[] identifyGeneralOrIndividual(ProcessedStructure cand, String tagSub,
            List<String> nouns, List<Integer> indices, boolean replace, boolean setIOB) {
        Pattern only_gc = getCompiledPattern(String.format("%s", PATTERN_GC));
        Pattern only_ic = getCompiledPattern(String.format("%s", PATTERN_IC));
        // Preserve original tokens to identify quantifiers
        String origConceptStr = String.join(" ", nouns).trim();
        origConceptStr = origConceptStr.replaceAll(" '", "'");
        if (normalize && nouns.size() > 0) {
            // Normalize last noun
            String last = nouns.get(nouns.size() - 1);
            nouns.set(nouns.size() - 1, sentFactory.getWordFactory().createWord(last).getLemma());
        }
        String svbrCandStr = String.join(" ", nouns).trim();
        // Remove underscore before apostrophes (related to [POS] tag)
        svbrCandStr = svbrCandStr.replaceAll(" '", "'");
        Matcher g = only_gc.matcher(tagSub);
        SBVRExpressionModel concept = null;
        String result = null;
        String entry = cand.rumbling;
        if (g.matches()) {
            result = RECOGNIZED_GC;
            concept = generalConcepts.get(svbrCandStr);
            if (concept == null) {
                // Should normalize the text as well, but some POS taggers (like StanfordNLP) do not recognize every proper case correctly
                // Therefore, it is currently disabled
                // svbrCandStr = svbrCandStr.toLowerCase();
                concept = new SBVRExpressionModel().addGeneralConcept(svbrCandStr, Boolean.TRUE);
                generalConcepts.put(svbrCandStr, concept);
                addConceptToExtractedMap(entry, concept, ConceptType.GENERAL_CONCEPT, replace);
            }
            if (setIOB)
                setIOBTags(cand, indices, IOBTag.IOB_CGC_START.getTypeString(),
                        IOBTag.IOB_CGC_MIDDLE.getTypeString(), IOBTag.IOB_CGC_END.getTypeString());
        } else {
            g = only_ic.matcher(tagSub);
            if (g.matches()) {
                result = RECOGNIZED_IC;
                concept = individualConcepts.get(svbrCandStr);
                if (concept == null) {
                    concept = new SBVRExpressionModel().addIndividualConcept(svbrCandStr, Boolean.TRUE);
                    individualConcepts.put(svbrCandStr, concept);
                    addConceptToExtractedMap(entry, concept, ConceptType.INDIVIDUAL_CONCEPT, replace);
                }
            }
            if (setIOB)
                setIOBTags(cand, indices, IOBTag.IOB_CIC_START.getTypeString(),
                        IOBTag.IOB_CIC_MIDDLE.getTypeString(), IOBTag.IOB_CIC_END.getTypeString());
        }
        if (result != null)
            addIdentifiedConcept(cand, concept);
        return new String[]{result, svbrCandStr, origConceptStr};
    }

    private String getVerbPresentTense(String verb) {
        return sentFactory.getWordFactory().createVerbWord(verb).getTenseForm(Tense.PRESENT);
    }

    private String checkRumblingForConcepts(ProcessedStructure cand, String entry, String tagReplacement,
            Map<String, SBVRExpressionModel> sbvrConcepts, Map<String, String> tempReplace, ConceptType type) {
        for (Entry<String, SBVRExpressionModel> concept : sbvrConcepts.entrySet()) {
            String general = concept.getKey();
            String initialEntry = entry;
            String tagRepl = String.format(" [%s-%s] ", tagReplacement, general.replace(" ", "_"));
            tempReplace.put(tagRepl.trim(), general);
            entry = entry.replaceAll(String.format("( %s )|^(%s )|( %s)$", general, general, general), tagRepl);
            // If some concepts were identified in the rumbling (replacement was executed), add them to the extracted map
            if (!entry.equals(initialEntry)) {
                addConceptToExtractedMap(cand.rumbling, concept.getValue(), type, Boolean.FALSE);
                addIdentifiedConcept(cand, concept.getValue());
            }
        }
        return entry;
    }

    protected void tagWithRecognizedGC(ProcessedStructure cand) {
        List<String> tags = cand.getPosList();
        List<String> tokens = cand.getTokenList();
        assert tags.size() == tokens.size();
        Pattern both_gc_ic = getCompiledPattern(PATTERN_GC_IC);
        Pattern full_gc_ic = getCompiledPattern(String.format("^(%s)$", PATTERN_GC_IC));
        Pattern invalid_ic_gc = getCompiledPattern(String.format("^(%s)+(%s)$", PATTERN_GC_IC, PATTERN_GC_IC));
        String tagString = cand.getTagString();
        List<Integer> posBracket = getBracketPositions(tagString);
        List<String> newPos = new ArrayList<>(), newToken = new ArrayList<>(), origToken = new ArrayList<>();
        // First check, if the whole rumbling is a GC/IC
        Matcher f = full_gc_ic.matcher(tagString);
        Matcher im = invalid_ic_gc.matcher(tagString);
        if (searchForVerbs && (f.matches() || (!f.matches() && im.matches())
                || // Fail to find any verb tag
                (!f.matches() && !im.matches() && StringUtils.indexOfAny(tagString, TAGS_VERB) == -1)))
            // If it's a rumbling which matches this pattern but must be parsed as verb concept, try to identify verb in this phrase
            if (cand.type != ConceptType.GENERAL_CONCEPT) {
                Map<String, String> tempReplace = new HashMap<>();
                Map<String, String> rumblingPosMap = new HashMap<>();
                for (int i = 0; i < tokens.size(); i++)
                    rumblingPosMap.put(tokens.get(i), tags.get(i));
                String entry = String.join(" ", tokens).trim();
                // Identify existing general and individual concepts
                entry = checkRumblingForConcepts(cand, entry, RECOGNIZED_GC, generalConcepts, tempReplace, ConceptType.GENERAL_CONCEPT);
                entry = checkRumblingForConcepts(cand, entry, RECOGNIZED_IC, individualConcepts, tempReplace, ConceptType.INDIVIDUAL_CONCEPT);
                entry = entry.trim();
                String[] words = entry.split(" ");
                for (int i = 0; i < words.length; i++) {
                    String rumbling = words[i].trim();
                    String tag = rumblingPosMap.get(rumbling);
                    if (rumbling.startsWith(String.format("[%s-", RECOGNIZED_GC))) {
                        tag = RECOGNIZED_GC;
                        rumbling = tempReplace.get(rumbling);
                    } else if (rumbling.startsWith(String.format("[%s-", RECOGNIZED_IC))) {
                        tag = RECOGNIZED_IC;
                        rumbling = tempReplace.get(rumbling);
                    }
                    newPos.add(tag);
                    newToken.add(rumbling);
                    origToken.add(rumbling);
                }
                // Select first noun after first identified GC which is also recognized as verb
                boolean cgcSkipped = false;
                int startIndex = 0;  // The first word is assumed to be the subject
                for (int i = startIndex; i < newPos.size(); i++) {
                    String currNewPos = newPos.get(i);
                    if (currNewPos == null)
                        continue;
                    if (currNewPos.equals(RECOGNIZED_GC) || currNewPos.equals(RECOGNIZED_IC))
                        cgcSkipped = true;
                    else if (cgcSkipped && !(currNewPos.equals(RECOGNIZED_GC) || currNewPos.equals(RECOGNIZED_IC))) {
                        startIndex = i;
                        break;
                    }
                }
                boolean verbFound = false;
                startIndex = 1;   // Skip first word
                while (!verbFound && startIndex < newPos.size())
                    if (VerbWord.isVerb(newToken.get(startIndex))) {
                        // Set tag for this word as VB
                        newPos.set(startIndex, "VB");
                        verbFound = true;
                        break;
                    } else
                        startIndex++;
                // Get tags originally identified by POS tagger
                String[] found = identifyGeneralOrIndividual(cand, tagString, tokens, null, true, false);
                if (found[0] != null)
                    // Add replacement for misidentified POS
                    wrongPosReplacements.put(cand.sentence.toString(), new PosReplacementData(cand.sentence.toString(),
                            Arrays.asList(found[0]), newPos, Arrays.asList(found[1]), newToken));
                else
                    wrongPosReplacements.put(cand.sentence.toString(), new PosReplacementData(cand.sentence.toString(),
                            tags, newPos, tokens, newToken));

            } else {
                List<Integer> indices = new ArrayList<>();
                for (int i = 0; i < cand.sentence.size(); i++)
                    indices.add(i);
                String match = f.matches() ? f.group() : im.matches() ? im.group() : cand.rumbling;
                String[] found = identifyGeneralOrIndividual(cand, match, tokens, indices, true, true);
                if (found[0] != null) {
                    newPos.add(found[0]);
                    newToken.add(found[1]);
                    origToken.add(found[2]);
                }
            }
        else {
            Matcher m = both_gc_ic.matcher(tagString);
            int prev_to = 0;
            while (m.find()) {
                for (int i = 0; i < posBracket.size(); i++)
                    if (posBracket.get(i) > prev_to && posBracket.get(i) < m.start()) {
                        newPos.add(tags.get(i));
                        newToken.add(tokens.get(i));
                        origToken.add(tokens.get(i));
                    }
                List<String> nouns = new ArrayList<>();
                List<Integer> indices = new ArrayList<>();
                for (int i = 0; i < posBracket.size(); i++)
                    if (posBracket.get(i) > m.start() && posBracket.get(i) < m.end()) {
                        nouns.add(tokens.get(i));
                        indices.add(i);
                    }
                String[] found = identifyGeneralOrIndividual(cand, m.group(), nouns, indices, false, false);
                if (found[0] != null) {
                    newPos.add(found[0]);
                    newToken.add(found[1]);
                    origToken.add(found[2]);
                }
                prev_to = m.end();
            }
            // Add remaining parts
            for (int i = 0; i < posBracket.size(); i++)
                if (posBracket.get(i) > prev_to) {
                    newPos.add(tags.get(i));
                    newToken.add(tokens.get(i));
                    origToken.add(tokens.get(i));
                }
        }
        cand.candPos = newPos;
        cand.candToken = newToken;
        cand.originalToken = origToken;
    }

    protected void tagWithRecognizedVC(ProcessedStructure cand, Pattern pattern) {
        Pattern vc_tag = getCompiledPattern(String.format("^(%s)$", VERB_FORM));
        List<String> tags = cand.getPosList();
        List<String> rumblings = cand.getTokenList();
        if (tags == null || rumblings == null)
            return;
        String tagString = cand.getTagString();
        if (!pattern.asPredicate().test(tagString))
            return;
        List<Integer> posBracket = getBracketPositions(tagString);
        String entry = cand.rumbling;
        Map<String, SBVRExpressionModel> identifiedByEntry = cand.identified;
        if (identifiedByEntry == null)
            // Verb concepts cannot contain only verb phrases!
            return;
        int prev_to = 0;
        List<String> newPos = new ArrayList<>(), newToken = new ArrayList<>(),
                origToken = new ArrayList<>();
        Matcher m = pattern.matcher(tagString);
        while (m.find()) {
            for (int i = 0; i < posBracket.size(); i++)
                if (posBracket.get(i) > prev_to && posBracket.get(i) < m.start()) {
                    newPos.add(tags.get(i));
                    newToken.add(rumblings.get(i));
                    origToken.add(cand.originalToken.get(i));
                }
            List<String> candEntry = new ArrayList(), origEntry = new ArrayList<>();
            SBVRExpressionModel newConcept = new SBVRExpressionModel();
            LinkedList<String> verbPart = new LinkedList<>();
            List<String> oldTags = new ArrayList<>(), oldRumblings = new ArrayList<>();
            for (int i = 0; i < posBracket.size(); i++)
                if (posBracket.get(i) > m.start() && posBracket.get(i) < m.end()) {
                    List<Integer> indices = new ArrayList<>();
                    String candToAdd = rumblings.get(i);
                    oldTags.add(tags.get(i));
                    oldRumblings.add(rumblings.get(i));
                    SBVRExpressionModel sbvr = identifiedByEntry.get(candToAdd);
                    if (sbvr != null) {
                        if (!verbPart.isEmpty())
                            // TODO: if verbPart is smth like "generalizes/specializes" it can be turned to generalization
                            newConcept.addVerbConcept(String.join(" ", verbPart).trim(), Boolean.TRUE);
                        // Identify quantifiers, according to rumbling part, representing identified concept 
                        QuantifierType quantifier = QuantifierType.QUANTIFIER_SINGLE;
                        if (i < cand.originalToken.size()) {
                            String conceptAsOriginal = cand.originalToken.get(i);
                            String origTagString = sentFactory.createSentence(conceptAsOriginal, tagger, true, null).getTagString();
                            quantifier = getQuantifier(origTagString);
                        }
                        addConceptToExtractedMap(entry, sbvr, ConceptType.GENERAL_CONCEPT, false);
                        if (!quantifier.equals(QuantifierType.QUANTIFIER_SINGLE)) {
                            newConcept.addQuantifier(quantifier);
                            candEntry.add(quantifier.getTypeString());
                        }
                        newConcept.addIdentifiedExpression(sbvr);
                        setIOBTags(cand, indices, IOBTag.IOB_CVC_START.getTypeString(),
                                IOBTag.IOB_CVC_MIDDLE.getTypeString(), IOBTag.IOB_CVC_END.getTypeString());
                        // Verb phrase will start next
                        verbPart.clear();
                        indices.clear();
                    } else {
                        if (normalize) {
                            // Normalize verb by using present tense
                            Matcher vm = vc_tag.matcher(String.format("[%s]", tags.get(i)));
                            // Cases like "is confirmed" (past participle) should not be viewed, as they correspond to characteristics
                            boolean prev_tag_cond = i > 0 ? vc_tag.matcher(String.format("[%s]", tags.get(i - 1))).matches() : false;
                            if (vm.matches() && !prev_tag_cond)
                                candToAdd = getVerbPresentTense(candToAdd);
                            // Cases like "is working", "was working etc." - should be normalized
                            else if (vm.matches() && String.format("[%s]", tags.get(i)).equalsIgnoreCase("[VBG]")) {
                                verbPart.pollLast();
                                candToAdd = getVerbPresentTense(candToAdd);
                            }
                        }
                        verbPart.add(candToAdd);
                        indices.add(i);
                    }
                    candEntry.add(candToAdd);
                    if (i < cand.originalToken.size())  // Should not be necessary...
                        origEntry.add(cand.originalToken.get(i));
                }
            // Add last verb phrase
            if (!verbPart.isEmpty())
                newConcept.addVerbConcept(String.join(" ", verbPart).trim(), Boolean.TRUE);

            String conceptStr = String.join(" ", candEntry).trim();
            verbConcepts.put(conceptStr, newConcept);
            newToken.add(conceptStr);
            newPos.add(RECOGNIZED_VC);
            origToken.add(String.join(" ", origEntry).trim());
            addIdentifiedConcept(cand, newConcept);
            addConceptToExtractedMap(entry, newConcept, ConceptType.VERB_CONCEPT, false);
            if (!cand.type.equals(ConceptType.BUSINESS_RULE))
                wrongPosReplacements.put(cand.sentence.toString(),
                        new PosReplacementData(cand.sentence.toString(), tags, newPos, rumblings, newToken));
            prev_to = m.end();
        }
        // Add remaining parts
        for (int i = 0; i < posBracket.size(); i++)
            if (posBracket.get(i) > prev_to) {
                newPos.add(tags.get(i));
                newToken.add(rumblings.get(i));
                origToken.add(cand.originalToken.get(i));
            }
        cand.candPos = newPos;
        cand.candToken = newToken;
        cand.originalToken = origToken;
    }

    protected void tagWithRecognizedBR(ProcessedStructure cand, Pattern pattern) {
        List<String> tags = cand.getPosList();
        List<String> rumblings = cand.getTokenList();
        if (tags == null || rumblings == null)
            return;
        String tagString = cand.getTagString();
        if (!pattern.asPredicate().test(tagString))
            return;
        String entry = cand.rumbling;
        Map<String, SBVRExpressionModel> identifiedByEntry = cand.identified;
        if (identifiedByEntry == null)
            // Rules should consist only of entries recognized previously
            return;
        int prev_to = 0;
        List<Integer> posBracket = getBracketPositions(tagString);
        List<String> newPos = new ArrayList<>(), newToken = new ArrayList<>(),
                origToken = new ArrayList<>();
        Matcher m = pattern.matcher(tagString);
        while (m.find()) {
            for (int i = 0; i < posBracket.size(); i++)
                if (posBracket.get(i) > prev_to && posBracket.get(i) < m.start()) {
                    newPos.add(tags.get(i));
                    newToken.add(rumblings.get(i));
                    origToken.add(cand.originalToken.get(i));
                }
            StringBuilder candEntry = new StringBuilder();
            SBVRExpressionModel newConcept = new SBVRExpressionModel();
            boolean valid = true;
            List<String> brTags = new ArrayList<>(),
                    brRumblings = new ArrayList<>();  // Must keep if BR candidate would be invalid
            for (int i = 0; i < posBracket.size(); i++)
                if (posBracket.get(i) > m.start() && posBracket.get(i) < m.end()) {
                    brTags.add(tags.get(i));
                    String candToAdd = rumblings.get(i);
                    brRumblings.add(candToAdd);
                    if (tags.get(i).equals(RECOGNIZED_VC)) {
                        SBVRExpressionModel sbvr = identifiedByEntry.get(candToAdd);
                        if (sbvr != null) {
                            addConceptToExtractedMap(entry, sbvr, ConceptType.VERB_CONCEPT, false);
                            newConcept.addIdentifiedExpression(sbvr);
                        } else {
                            valid = false;
                            break;
                        }
                    } else if (candToAdd.equalsIgnoreCase("if")) {
                        newConcept.addIfExpression();
                        cand.getSentence().get(i).setIOB(IOBTag.IF_COND.getTypeString());
                    } else if (candToAdd.equalsIgnoreCase("and")) {
                        newConcept.addAndExpression();
                        cand.getSentence().get(i).setIOB(IOBTag.CONJUNCTION.getTypeString());
                    } else if (candToAdd.equalsIgnoreCase("or")) {
                        newConcept.addOrExpression();
                        cand.getSentence().get(i).setIOB(IOBTag.DISJUNCTION.getTypeString());
                    } else
                        // Other adjunctions/prepositions can also be compatible to specs, thus they are added as unrecognized 
                        newConcept.addUnidentifiedText(candToAdd);
                    candEntry.append(candToAdd).append(" ");
                }
            if (valid) {
                String conceptStr = candEntry.toString().trim();
                businessRules.put(conceptStr, newConcept);
                newToken.add(conceptStr);
                newPos.add(RECOGNIZED_BR);
                origToken.add(cand.rumbling);
                addIdentifiedConcept(cand, newConcept);
                addConceptToExtractedMap(entry, newConcept, ConceptType.BUSINESS_RULE, false);
            } else {
                newToken.addAll(brRumblings);
                newPos.addAll(brTags);
                origToken.addAll(brRumblings);
            }
            prev_to = m.end();
        }
        // Add remaining parts
        for (int i = 0; i < posBracket.size(); i++)
            if (posBracket.get(i) > prev_to) {
                newPos.add(tags.get(i));
                newToken.add(rumblings.get(i));
                origToken.add(cand.originalToken.get(i));
            }
        cand.candPos = newPos;
        cand.candToken = newToken;
        cand.originalToken = origToken;
    }

    protected void tagWithRecognizedBR_2(ProcessedStructure cand) {
        Pattern pattern = getCompiledPattern(PATTERN_BR_2);
        List<String> tags = cand.getPosList();
        List<String> rumblings_ = cand.getTokenList();
        if (tags == null || rumblings_ == null)
            return;
        String tagString = cand.getTagString();
        if (!pattern.asPredicate().test(tagString))
            return;
        String entry = cand.rumbling;
        Matcher m = pattern.matcher(tagString);
        Map<String, SBVRExpressionModel> identifiedByEntry = cand.identified;
        if (identifiedByEntry == null)
            // Rules should consist only of entries recognized previously
            return;
        int prev_to = 0;
        List<Integer> posBracket = getBracketPositions(tagString);
        List<String> newPos = new ArrayList<>(), newToken = new ArrayList<>(),
                origToken = new ArrayList<>();
        while (m.find()) {
            for (int i = 0; i < posBracket.size(); i++)
                if (posBracket.get(i) > prev_to && posBracket.get(i) < m.start()) {
                    newPos.add(tags.get(i));
                    newToken.add(rumblings_.get(i));
                    origToken.add(cand.originalToken.get(i));
                }
            StringBuilder candEntry = new StringBuilder();
            SBVRExpressionModel newConcept = new SBVRExpressionModel();
            boolean valid = true;
            List<String> brTags = new ArrayList<>(),
                    brRumblings = new ArrayList<>();  // Must keep if BR candidate would be invalid
            for (int i = 0; i < posBracket.size(); i++)
                if (posBracket.get(i) > m.start() && posBracket.get(i) < m.end()) {
                    brTags.add(tags.get(i));
                    String candToAdd = rumblings_.get(i);
                    brRumblings.add(candToAdd);
                    if (tags.get(i).equals(RECOGNIZED_BR)) {
                        SBVRExpressionModel sbvr = identifiedByEntry.get(candToAdd);
                        if (sbvr != null) {
                            // Remove the rule which was not recognized completely in previous stage
                            businessRules.remove(candToAdd);
                            mapExtracted.get(entry).remove(sbvr);
                            newConcept.addIdentifiedExpression(sbvr);
                        } else {
                            valid = false;
                            break;
                        }
                    } else if (candToAdd.equalsIgnoreCase(SBVRExpressionModel.IF_COND)) {
                        newConcept.addIfExpression();
                        cand.getSentence().get(i).setIOB("IF_COND");
                    } else if (candToAdd.equalsIgnoreCase(SBVRExpressionModel.CONJUNCTION)) {
                        newConcept.addAndExpression();
                        cand.getSentence().get(i).setIOB("CONJ");
                    } else if (candToAdd.equalsIgnoreCase(SBVRExpressionModel.DISJUNCTION)) {
                        newConcept.addOrExpression();
                        cand.getSentence().get(i).setIOB("DISJ");
                    } else
                        // Other adjunctions/prepositions can also be compatible to specs, thus they are added as unrecognized 
                        newConcept.addUnidentifiedText(candToAdd);
                    candEntry.append(candToAdd).append(" ");
                }
            if (valid) {
                String conceptStr = candEntry.toString().trim();
                businessRules.put(conceptStr, newConcept);
                newToken.add(conceptStr);
                newPos.add(RECOGNIZED_BR);
                origToken.add(conceptStr);
                addIdentifiedConcept(cand, newConcept);
                addConceptToExtractedMap(entry, newConcept, ConceptType.BUSINESS_RULE, false);
            } else {
                newToken.addAll(brRumblings);
                newPos.addAll(brTags);
                origToken.addAll(brRumblings);
            }
            prev_to = m.end();
        }
        // Add remaining parts
        for (int i = 0; i < posBracket.size(); i++)
            if (posBracket.get(i) > prev_to) {
                newPos.add(tags.get(i));
                newToken.add(rumblings_.get(i));
                origToken.add(cand.originalToken.get(i));
            }
        cand.candPos = newPos;
        cand.candToken = newToken;
        cand.originalToken = origToken;
    }

}
