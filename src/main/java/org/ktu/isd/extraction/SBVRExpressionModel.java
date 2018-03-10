package org.ktu.isd.extraction;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class SBVRExpressionModel implements Cloneable, Comparable<SBVRExpressionModel> {

    public final static String CGC_FORMAT = "<span style='text-decoration: underline; color: teal;'>%s</span>";
    public final static String CBLANK_FORMAT = "<span>%s</span>";
    public final static String CVC_FORMAT = "<span style='font-style: italic; color: blue;'>%s</span>";
    public final static String CRC_FORMAT = "<span style='color: orange;'>%s</span>";

    private static final ResourceBundle bundle = ResourceBundle.getBundle("net/tmine/entities/Messages");

    public static String IF_COND = "if";
    public static String CONJUNCTION = "and";
    public static String DISJUNCTION = "or";

    public enum QuantifierType {
        QUANTIFIER_ONE_MORE("one or more"), QUANTIFIER_MANY("many"), QUANTIFIER_SINGLE("single");

        private String typeRepr;

        private QuantifierType(String typeRepr) {
            this.typeRepr = typeRepr;
        }

        public String getTypeString() {
            return typeRepr;
        }

    }

    public enum ExpressionType {
        GENERAL_CONCEPT, INDIVIDUAL_CONCEPT, VERB_CONCEPT, RULE_TYPE, RULE_IF, RULE_AND, RULE_OR, QUANTIFIER, GENERIC
    }

    public enum RuleType {
        OBLIGATION("It is obligatory that"),
        PERMISSION("It is permitted that"),
        POSSIBILITY("It is possible that");

        private final String expr;

        RuleType(String expr) {
            this.expr = expr;
        }

        public String toString() {
            return expr;
        }
    }

    // Original data
    private List<ExpressionType> types;
    private List<String> expressions;
    private boolean auto;
    private SBVRExpressionModel general_concept;
    private List<SBVRExpressionModel> synonymous_forms;
    private boolean mmVocConcept;               // Is it a SBVR concept which belongs metamodel vocabulary?
    private List<Boolean> identified;   	// Identified by the user previously

    public SBVRExpressionModel() {
        types = new ArrayList<>();
        expressions = new ArrayList<>();
        identified = new ArrayList<>();
        synonymous_forms = new ArrayList<>();
        general_concept = null;
        // By default, this SBVRExpressionModel does not represent a model vocabulary concept
        mmVocConcept = false;
    }

    public SBVRExpressionModel addGeneralConcept(String expression, Boolean isIdentified) {
        types.add(ExpressionType.GENERAL_CONCEPT);
        expressions.add(expression);
        identified.add(isIdentified);
        return this;
    }

    public SBVRExpressionModel addIndividualConcept(String expression, Boolean isIdentified) {
        types.add(ExpressionType.INDIVIDUAL_CONCEPT);
        expressions.add(expression);
        identified.add(isIdentified);
        return this;
    }

    public SBVRExpressionModel addVerbConcept(String expression, Boolean isIdentified) {
        types.add(ExpressionType.VERB_CONCEPT);
        expressions.add(expression);
        identified.add(isIdentified);
        return this;
    }

    public SBVRExpressionModel addRuleExpression(RuleType type) {
        types.add(ExpressionType.RULE_TYPE);
        expressions.add(type.toString());
        identified.add(true);
        return this;
    }

    public SBVRExpressionModel addIfExpression() {
        types.add(ExpressionType.RULE_IF);
        expressions.add(IF_COND);
        identified.add(true);
        return this;
    }

    public SBVRExpressionModel addAndExpression() {
        types.add(ExpressionType.RULE_AND);
        expressions.add(CONJUNCTION);
        identified.add(true);
        return this;
    }

    public SBVRExpressionModel addOrExpression() {
        types.add(ExpressionType.RULE_OR);
        expressions.add(DISJUNCTION);
        identified.add(true);
        return this;
    }

    public SBVRExpressionModel addUnidentifiedText(String expression) {
        types.add(ExpressionType.GENERIC);
        expressions.add(expression);
        identified.add(false);
        return this;
    }

    public SBVRExpressionModel addReservedText(String expression) {
        types.add(ExpressionType.GENERIC);
        expressions.add(expression);
        identified.add(true);
        return this;
    }

    public SBVRExpressionModel addQuantifier(QuantifierType quantifier) {
        types.add(ExpressionType.QUANTIFIER);
        expressions.add(quantifier.getTypeString());
        identified.add(true);
        return this;
    }

    public SBVRExpressionModel addIdentifiedExpression(SBVRExpressionModel model) {
        types.addAll(model.types);
        expressions.addAll(model.expressions);
        identified.addAll(model.identified);
        return this;
    }

    public void replace(SBVRExpressionModel modification) {
        types = modification.types;
        expressions = modification.expressions;
        identified = modification.identified;
        auto = modification.auto;
        general_concept = modification.general_concept;
        mmVocConcept = modification.mmVocConcept;
        synonymous_forms = modification.synonymous_forms;
    }
    
    public SBVRExpressionModel remove(int i) {
        types.remove(i);
        expressions.remove(i);
        identified.remove(i);
        return this;
    }    

    public boolean isAuto() {
        return auto;
    }

    public void setAuto(boolean auto) {
        this.auto = auto;
    }

    public void setIdentified(boolean value) {
        for (int i = 0; i < identified.size(); i++)
            identified.set(i, value);
    }

    public boolean originalEqualsTo(SBVRExpressionModel model) {
        return expressions.equals(model.expressions) && types.equals(model.types);
    }

    public boolean equalsTo(String string) {
        return string.compareTo(toString()) == 0;
    }

    public String getExpressionElement(int index) {
        if (expressions.size() <= index)
            return null;
        return expressions.get(index);
    }

    public ExpressionType getExpressionType(int index) {
        if (types.size() <= index)
            return null;
        return types.get(index);
    }

    public void setExpressionType(int index, ExpressionType type) {
        if (types.size() <= index)
            return;
        types.set(index, type);
    }

    public int length() {
        return expressions.size();
    }

    private String getString(List<String> expressions) {
        if (expressions.isEmpty())
            return null;
        StringBuilder res = new StringBuilder();
        for (String expression : expressions)
            res.append(expression).append(" ");
        return res.toString().trim();
    }

    public String toString() {
        return getString(expressions);
    }

    public String toUnderscoreString() {
        if (expressions.isEmpty())
            return null;
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < expressions.size(); i++)
            res.append(types.get(i) == ExpressionType.RULE_TYPE ? expressions.get(i)
                    : expressions.get(i).replaceAll(" ", "_")).append(" ");
        // Remove underscore before "'" in model vocabulary concepts
        return res.toString().trim().replace("_'", " '");
    }

    public String toHTMLString(boolean addHtml, Boolean showIdentified) {
        if (expressions.isEmpty())
            return null;
        StringBuilder res = new StringBuilder();
        if (addHtml)
            res.append("<html>");
        for (int i = 0; i < expressions.size(); i++)
            res.append(String.format(getFormat(types.get(i),
                    showIdentified != null ? showIdentified : identified.get(i)), expressions.get(i))).append(" ");
        if (addHtml)
            res.append("</html>");
        return res.toString().trim();
    }

    private String getFormat(ExpressionType type, boolean identified) {
        if ((type == ExpressionType.GENERAL_CONCEPT || type == ExpressionType.VERB_CONCEPT) && !identified)
            return CBLANK_FORMAT;
        if (type == ExpressionType.GENERAL_CONCEPT && identified)
            return CGC_FORMAT;
        if (type == ExpressionType.VERB_CONCEPT && identified)
            return CVC_FORMAT;
        if (type == ExpressionType.RULE_TYPE || type == ExpressionType.RULE_IF || type == ExpressionType.QUANTIFIER)
            return CRC_FORMAT;
        return CBLANK_FORMAT;
    }

    public SBVRExpressionModel clone() {
        SBVRExpressionModel copy = new SBVRExpressionModel();
        copy.types = new ArrayList<>();
        for (ExpressionType type : types)
            copy.types.add(type);
        copy.expressions = new ArrayList<>();
        for (String str : expressions)
            copy.expressions.add(str);
        copy.identified = new ArrayList<>();
        for (Boolean ident : identified)
            copy.identified.add(ident);
        copy.auto = auto;
        return copy;
    }

    @Override
    public int compareTo(SBVRExpressionModel o) {
        return toString().compareToIgnoreCase(o.toString());
    }

    public List<ExpressionType> getTypes() {
        return types;
    }

    public List<String> getExpressions() {
        return expressions;
    }

    public SBVRExpressionModel getGeneralConcept() {
        return general_concept;
    }

    public void setGeneralConcept(SBVRExpressionModel general_concept) throws SBVRModelException {
        if (general_concept == null)
            return;
        if (general_concept.getTypes().size() == 1
                && general_concept.getTypes().get(0) == ExpressionType.GENERAL_CONCEPT)
            this.general_concept = general_concept;
        else
            throw new SBVRModelException(bundle.getString("SBVRExpressionModel.1"));
    }

    public boolean isModelVocabularyConcept() {
        return mmVocConcept;
    }

    public void setModelVocabularyConcept(boolean mmVocConcept) {
        this.mmVocConcept = mmVocConcept;
    }

    public List<SBVRExpressionModel> getSynonymousForms() {
        return synonymous_forms;
    }

    public void addSynonymousForm(SBVRExpressionModel model) {
        if (model != null)
            synonymous_forms.add(model);
    }

}
