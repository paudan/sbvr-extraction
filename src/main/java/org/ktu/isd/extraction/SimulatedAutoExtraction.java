package org.ktu.isd.extraction;

import java.util.Map;
import java.util.Set;
import net.tmine.entities.InitializationException;

public class SimulatedAutoExtraction extends AbstractVocabularyExtractor {

    public SimulatedAutoExtraction(Map<String, ConceptType> rumblings) {
        super(rumblings);
    }

    @Override
    public void extract() throws InitializationException {
        if (rumblings == null || rumblings.isEmpty())
            return;
        clearResultStructures();
        // Expected general concept entries are automatically mapped to SBVR general concepts
        Set<String> gcCandidates = filterRumblingsByType(ConceptType.GENERAL_CONCEPT);
        for (String rumbling : gcCandidates) {
           SBVRExpressionModel concept = new SBVRExpressionModel().addGeneralConcept(rumbling.replaceAll(" '", "'"), Boolean.TRUE);
           addConceptToExtractedMap(rumbling, concept, ConceptType.GENERAL_CONCEPT, true);
        }
        // Expected verb concept entries are searched for existing general verb concepts at the beginning or end
        // As NLP techniques are not applied in automatic extraction, it is virtually impossible to extract n-ary associations, 
        // thus simplified processing is applied
        Set<String> vcCandidates = filterRumblingsByType(ConceptType.VERB_CONCEPT);
        for (String rumbling : vcCandidates) {
            for (String gc: gcCandidates) {
               if (rumbling.startsWith(gc)) {
                   String last_part = rumbling.substring(gc.length()).trim();
                   if (last_part.length() == 0)
                       continue;
                   String verb_phrase = last_part.split(" ")[0].trim();
                   if (verb_phrase.length() == 0)
                       continue;
                   String end_phrase = last_part.substring(verb_phrase.length()).trim();
                   SBVRExpressionModel concept = new SBVRExpressionModel().addGeneralConcept(gc, true)
                           .addVerbConcept(verb_phrase, true);
                   if (end_phrase.length() > 0)
                       concept = concept.addGeneralConcept(end_phrase, true);
                   addConceptToExtractedMap(rumbling, concept, ConceptType.VERB_CONCEPT, true);
               } else if (rumbling.endsWith(gc)) {
                   String first_part = rumbling.substring(0, rumbling.length() - gc.length()).trim();
                   if (first_part.length() == 0)
                       continue;
                   String start_phrase = first_part.split(" ")[0].trim();
                   if (start_phrase.length() == 0)
                       continue;
                   // Remaining part id treated as verb phrase
                   String verb_phrase = first_part.substring(start_phrase.length()).trim();
                   SBVRExpressionModel concept = new SBVRExpressionModel().addGeneralConcept(start_phrase, true);
                   if (verb_phrase.length() > 0)
                        concept = concept.addVerbConcept(verb_phrase, true);
                   concept = concept.addGeneralConcept(gc, true);
                   addConceptToExtractedMap(rumbling, concept, ConceptType.VERB_CONCEPT, true);
               }
            }
        }
    }

}
