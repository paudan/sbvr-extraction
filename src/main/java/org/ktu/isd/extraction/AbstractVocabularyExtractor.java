package org.ktu.isd.extraction;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract public class AbstractVocabularyExtractor implements VocabularyExtractor {
    
    protected Map<String, SBVRExpressionModel> generalConcepts, individualConcepts,
        verbConcepts, businessRules;
    protected Map<String, ConceptType> rumblings;
    protected Map<String, Map<SBVRExpressionModel, ConceptType>> mapExtracted;
    /**
     * Whether normalization (use of lemmas) should be enabled
     * Note, that while this is helpful to extract more valid concepts, it would also result in some information loss, e.g.:
     * - verb forms or tense information, such as {@code creates};
     * - nouns forms, which must be multiple by nature, e.g., {@code United States}
     */
    protected boolean normalize = Boolean.FALSE;
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    public AbstractVocabularyExtractor(Map<String, ConceptType> rumblings) {
        this.rumblings = rumblings;
        initialize();    
    }

    public AbstractVocabularyExtractor() {
        this.rumblings = null;
        initialize();
    }
    
    protected void initialize() {
        // Must enforce to set longer phrases at the beginning of the list
        Comparator<String> lenComparator = this.getDefaultComparator();
        generalConcepts = new TreeMap<>(lenComparator);
        individualConcepts = new TreeMap<>(lenComparator);
        verbConcepts = new TreeMap<>(lenComparator);
        businessRules = new TreeMap<>(lenComparator);
        mapExtracted = new HashMap<>();
    }
    
    protected Comparator<String> getDefaultComparator() {
        return (String s1, String s2) -> {
            if (s1.length() > s2.length())
                return -1;
            else if (s1.length() < s2.length())
                return 1;
            else
                return s1.compareTo(s2);
        };
    }

    @Override
    public Collection<SBVRExpressionModel> getExtractedGeneralConcepts() {
        return Collections.unmodifiableCollection(generalConcepts.values());
    }

    @Override
    public Collection<SBVRExpressionModel> getExtractedIndividualConcepts() {
        return Collections.unmodifiableCollection(individualConcepts.values());
    }

    @Override
    public Collection<SBVRExpressionModel> getExtractedVerbConcepts() {
        return Collections.unmodifiableCollection(verbConcepts.values());
    }

    @Override
    public Collection<SBVRExpressionModel> getExtractedBusinessRules() {
        return Collections.unmodifiableCollection(businessRules.values());
    }

    @Override
    public Map<String, Map<SBVRExpressionModel, ConceptType>> getExtractedConceptsAsMap() {
        return Collections.unmodifiableMap(mapExtracted);
    }
    
    @Override
    public void setUseNormalization(boolean useNorm) {
        this.normalize = useNorm;
    }
    
    @Override
    public Map<String, ConceptType> getRumblings() {
        return rumblings;
    }

    @Override
    public void setRumblings(Map<String, ConceptType> rumblings) {
        this.rumblings = rumblings;
    }
    
    protected void clearResultStructures() {
        generalConcepts.clear();
        individualConcepts.clear();
        verbConcepts.clear();
        businessRules.clear();
        for (String key : mapExtracted.keySet())
            mapExtracted.get(key).clear();
        mapExtracted.clear();
    }
    
    protected void addConceptToExtractedMap(String key, SBVRExpressionModel concept, ConceptType conceptType, boolean replace) {
        Map<SBVRExpressionModel, ConceptType> conceptSet = mapExtracted.get(key);
        if (replace) {
            conceptSet = new HashMap<>();
            conceptSet.put(concept, conceptType);
            mapExtracted.put(key, conceptSet);
        } else {
            if (conceptSet == null) {
                conceptSet = new HashMap<>();
                mapExtracted.put(key, conceptSet);
            }
            conceptSet.put(concept, conceptType);
        }
    }
    
    protected Set<String> filterRumblingsByType(ConceptType conceptType) {
        return rumblings.entrySet().stream()
           .filter(map -> map.getValue().equals(conceptType) && map.getKey().trim().length() > 0)
           .collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue())).keySet();
    }
    
}
