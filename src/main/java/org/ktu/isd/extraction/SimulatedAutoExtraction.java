package org.ktu.isd.extraction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.tmine.entities.InitializationException;
import org.ktu.isd.extraction.SBVRExpressionModel.ExpressionType;
import org.ktu.isd.extraction.SBVRExpressionModel.RuleType;

public class SimulatedAutoExtraction extends AbstractVocabularyExtractor {

    public static final String BR_PATTERN = "^It is (possible|obligatory|permitted) that (.*?[^(\\ if\\ )])( if (.*))*$";

    public SimulatedAutoExtraction(Map<String, ConceptType> rumblings) {
        super(rumblings);
    }

    public SimulatedAutoExtraction() {
        super();
    }
    
    public boolean isIndividualConcept(String rumbling) {
        String[] words = rumbling.split(" ");
        for (String word: words)
            if (!Character.isUpperCase(word.charAt(0)))
                return false;
        return true;
    }

    public Object[] processBRRumbling(String rumbling) {
        Pattern pattern = Pattern.compile(BR_PATTERN);
        Matcher matcher1 = pattern.matcher(rumbling);
        List<String> result = new ArrayList<>();
        RuleType ruleType = null;
        if (!matcher1.matches())
            return new Object[]{result, ruleType};
        int matcherGroups = matcher1.groupCount() + 1;
        String type = matcher1.group(1);
        if (type == null)
            return new Object[]{result, ruleType};
        type = type.trim();
        if (type.compareToIgnoreCase("obligatory") == 0)
            ruleType = RuleType.OBLIGATION;
        else if (type.compareToIgnoreCase("permitted") == 0)
            ruleType = RuleType.PERMISSION;
        else if (type.compareToIgnoreCase("possible") == 0)
            ruleType = RuleType.POSSIBILITY;
        if (matcherGroups < 3)
            return new Object[]{result, ruleType};
        String firstVC = matcher1.group(2);
        if (firstVC != null)
            result.add(firstVC.trim());
        if (matcherGroups < 5)
            return new Object[]{result, ruleType};
        String postAnd = matcher1.group(4);
        if (postAnd != null) {
            postAnd = postAnd.trim();
            String[] splits = postAnd.split(" and");
            for (int i = 0; i < splits.length; i++)
                result.add(splits[i].trim());
        }
        return new Object[]{result, ruleType};
    }
    
    private void addGeneralConcept(String candString, String rumbling) {
        SBVRExpressionModel gc2 = new SBVRExpressionModel();
        ConceptType type = ConceptType.GENERAL_CONCEPT;
        if (isIndividualConcept(candString)) {
            gc2 = gc2.addIndividualConcept(candString, true);
            type = ConceptType.INDIVIDUAL_CONCEPT;
        } else 
            gc2 = gc2.addGeneralConcept(candString, true);
        addConceptToExtractedMap(rumbling, gc2, type, false);
    }

    private SBVRExpressionModel processVCRumbling(String rumbling) {
        rumbling = rumbling.trim();
        if (rumbling.trim().length() == 0)
            return null;
        List<String> gcCandidates = filterRumblingsByType(ConceptType.GENERAL_CONCEPT);
        for (String gc : gcCandidates)
            if (rumbling.startsWith(gc)) {
                String last_part = rumbling.substring(gc.length()).trim();
                if (last_part.length() == 0)
                    continue;
                String verb_phrase = last_part.split(" ")[0].trim();
                if (verb_phrase.length() == 0)
                    continue;
                String end_phrase = last_part.substring(verb_phrase.length()).trim();
                addGeneralConcept(gc, rumbling);
                SBVRExpressionModel concept = new SBVRExpressionModel();
                concept = isIndividualConcept(gc) ? concept.addGeneralConcept(gc, true) : concept.addIndividualConcept(gc, true);
                concept = concept.addVerbConcept(verb_phrase, true);
                if (end_phrase.length() > 0) {
                    addGeneralConcept(end_phrase, rumbling);
                    concept = isIndividualConcept(end_phrase) ? concept.addGeneralConcept(end_phrase, true) : concept.addIndividualConcept(end_phrase, true);
                }
                addConceptToExtractedMap(rumbling, concept, ConceptType.VERB_CONCEPT, false);
                return concept;
            } else if (rumbling.endsWith(gc)) {
                String first_part = rumbling.substring(0, rumbling.length() - gc.length()).trim();
                if (first_part.length() == 0)
                    continue;
                String start_phrase = first_part.split(" ")[0].trim();
                if (start_phrase.length() == 0)
                    continue;
                // Remaining part id treated as verb phrase
                String verb_phrase = first_part.substring(start_phrase.length()).trim();
                addGeneralConcept(start_phrase, rumbling);
                SBVRExpressionModel concept = new SBVRExpressionModel();
                concept = isIndividualConcept(start_phrase) ? concept.addGeneralConcept(start_phrase, true) : concept.addIndividualConcept(start_phrase, true);
                if (verb_phrase.length() > 0)
                    concept = concept.addVerbConcept(verb_phrase, true);
                addGeneralConcept(gc, rumbling);
                concept = isIndividualConcept(gc) ? concept.addGeneralConcept(gc, true) : concept.addIndividualConcept(gc, true);
                addConceptToExtractedMap(rumbling, concept, ConceptType.VERB_CONCEPT, false);
                return concept;
            }
        // NO general concept candidate was found, run typical processing
        String[] parts = rumbling.split(" ");
        if (parts.length == 0)
            return null;
        String start_phrase = parts[0].trim();
        SBVRExpressionModel concept = new SBVRExpressionModel();
        addGeneralConcept(start_phrase, rumbling);
        concept = isIndividualConcept(start_phrase) ? concept.addGeneralConcept(start_phrase, true) : concept.addIndividualConcept(start_phrase, true);
        if (parts.length > 1)
            concept = concept.addVerbConcept(parts[1], false);
        if (parts.length > 2) {
            String right_gc = String.join(" ", Arrays.copyOfRange(parts, 2, parts.length));
            addGeneralConcept(right_gc, rumbling);
            concept = isIndividualConcept(right_gc) ? concept.addGeneralConcept(right_gc, true) : concept.addIndividualConcept(right_gc, true);
        }
        addConceptToExtractedMap(rumbling, concept, ConceptType.VERB_CONCEPT, false);
        return concept;
    }

    @Override
    public void extract() throws InitializationException {
        if (rumblings == null || rumblings.isEmpty())
            return;
        clearResultStructures();
        // Expected general concept entries are automatically mapped to SBVR general concepts
        List<String> gcCandidates = filterRumblingsByType(ConceptType.GENERAL_CONCEPT);
        for (String rumbling : gcCandidates) {
            SBVRExpressionModel concept = new SBVRExpressionModel().addGeneralConcept(rumbling.replaceAll(" '", "'"), Boolean.TRUE);
            addConceptToExtractedMap(rumbling, concept, ConceptType.GENERAL_CONCEPT, false);
        }
        // Expected verb concept entries are searched for existing general verb concepts at the beginning or end
        // As NLP techniques are not applied in automatic extraction, it is virtually impossible to extract n-ary associations, 
        // thus simplified processing is applied
        List<String> vcCandidates = filterRumblingsByType(ConceptType.VERB_CONCEPT);
        for (String rumbling : vcCandidates)
            processVCRumbling(rumbling);
        // Finally, business rules parsing is performed
        // They are relatively structurized during the auto extraction step, thus parsing is more relevant only for "verb concept" parts
        List<String> brCandidates = filterRumblingsByType(ConceptType.BUSINESS_RULE);
        Collection<SBVRExpressionModel> vcConcepts = getExtractedVerbConcepts();
        for (String rumbling : brCandidates) {
            Object[] processed = processBRRumbling(rumbling);
            List<String> vcParts = (List<String>) processed[0];
            RuleType ruleType = (RuleType) processed[1];
            if (ruleType == null)
                continue;
            SBVRExpressionModel brModel = new SBVRExpressionModel().addRuleExpression(ruleType);
            for (int i = 0; i < vcParts.size(); i++) {
                String vc = vcParts.get(i);
                SBVRExpressionModel vcModel = null;
                search:
                for (SBVRExpressionModel model : vcConcepts)
                    if (vc.compareToIgnoreCase(model.toString()) == 0) {
                        vcModel = model;
                        break search;
                    }
                if (vcModel == null)
                    vcModel = processVCRumbling(vc);
                if (vcModel != null)
                    addConceptToExtractedMap(rumbling, vcModel, ConceptType.VERB_CONCEPT, false);
                brModel.addVerbConcept(vc, true);
                if (i == 0)
                    brModel.addIfExpression();
                else
                    brModel.addAndExpression(); // Should be improved to account for or expressions as well
            }
            // Remove last entry from the model, if it is an ExpressionType
            if (brModel.getExpressionType(brModel.length() - 1) == ExpressionType.RULE_AND)
                brModel.remove(brModel.length() - 1);
            addConceptToExtractedMap(rumbling, brModel, ConceptType.BUSINESS_RULE, false);
        }
    }

}
