/*
 * Copyright (C) 2015, United States Government, as represented by the 
 * Administrator of the National Aeronautics and Space Administration.
 * All rights reserved.
 *
 * The PSYCO: A Predicate-based Symbolic Compositional Reasoning environment 
 * platform is licensed under the Apache License, Version 2.0 (the "License"); you 
 * may not use this file except in compliance with the License. You may obtain a 
 * copy of the License at http://www.apache.org/licenses/LICENSE-2.0. 
 *
 * Unless required by applicable law or agreed to in writing, software distributed 
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR 
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the 
 * specific language governing permissions and limitations under the License.
 */
package gov.nasa.jpf.constraints.solvers.smtinterpol;

import de.uni_freiburg.informatik.ultimate.logic.Term;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.Constant;
import gov.nasa.jpf.constraints.expressions.Negation;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.expressions.NumericCompound;
import gov.nasa.jpf.constraints.expressions.NumericOperator;
import gov.nasa.jpf.constraints.expressions.UnaryMinus;
import gov.nasa.jpf.constraints.solvers.smtinterpol.exception.TermParserException;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TermParser {

    private String input;

    private Map<String, Expression> let = new HashMap<>();

    private final Map<String, Variable> vars = new HashMap<>();

    public TermParser(Term t, Set<Variable<?>> vars) {
        this.input = t.toString().trim();
        for (Variable v : vars) {
            this.vars.put(v.getName(), v);
        }
    }

    public Expression parse() throws TermParserException {
        parseLet();
        Expression result = parseTerm();
        return result;
    }

    private Expression parseTerm() {
        if (isComplexTerm()) {
            return parseComplexTerm();
        } else {
            return parseSimpleTerm();
        }
    }

    private Expression parseSimpleTerm() {
        String token = nextToken().trim();
        if (token.equals("true")) {
            return ExpressionUtil.TRUE;
        } else if (token.equals("false")) {
            return ExpressionUtil.FALSE;
        } else if (token.matches("\\d+")) {
            return new Constant(BuiltinTypes.SINT32, Integer.parseInt(token));
        } else {
            if (let.containsKey(token)) {
                return let.get(token);
            }
            //return new Variable(BuiltinTypes.SINT32, token);
            Variable var = this.vars.get(token);
            assert var != null;
            return var;
        }
    }

    private Expression parseComplexTerm() {
        removeParen();
        String op = nextToken();
        ArrayList<Expression> sub = new ArrayList<>();
        while (hasNextTerm()) {
          sub.add(parseTerm());
        }
        removeParen();
        switch (op) {
            case "=":
                return new NumericBooleanExpression(
                        sub.get(0), NumericComparator.EQ, sub.get(1));
            case ">=":
                return new NumericBooleanExpression(
                        sub.get(0), NumericComparator.GE, sub.get(1));
            case "<=":
                return new NumericBooleanExpression(
                        sub.get(0), NumericComparator.LE, sub.get(1));

            case "-":
                return (sub.size() == 1 ? new UnaryMinus<>(sub.get(0))
                        : new NumericCompound(sub.get(0), NumericOperator.MINUS, sub.get(1)));
            case "+":
                return new NumericCompound(
                        sub.get(0), NumericOperator.PLUS, sub.get(1));
            case "and":
                return ExpressionUtil.and(sub.toArray(new Expression[]{}));
            case "or":
                return ExpressionUtil.or(sub.toArray(new Expression[]{}));
            case "not":
                return new Negation(sub.get(0));
        }
        throw new IllegalArgumentException("unknown operator: " + op);
    }

    private boolean isComplexTerm() {
        return input.startsWith("(");
    }

    private String nextToken() {

        int idx1 = input.indexOf(" ");
        int idx2 = input.indexOf(")");
        int idx = idx1;
        if (idx < 1 || (idx2 >= 0 && idx2 < idx1)) {
            idx = idx2;
        }

        if (idx < 0) {
            idx = input.length();
        }

        String ret = input.substring(0, idx);
        input = input.substring(idx);
        trim();
        return ret;
    }

    private void removeParen() {
        if (input.startsWith("(") || input.startsWith(")")) {
            input = input.substring(1);
            trim();
        }
    }

    private boolean hasNextTerm() {
        trim();
        return !input.startsWith(")");
    }

    private void trim() {
        input = input.trim();
    }

    private void parseLet() throws TermParserException {
      while (hasMoreLet()) {
        getFirstLet();
      }
    }

    private void parseOneLet() throws TermParserException {
        trim();
        this.input = this.input.replaceFirst("\\(let", "");
        trim();
        removeParen();
        trim();
        if(hasMoreLet()){
          parseLet();
          trim();
          removeParen();
          trim();
        }
        while (hasNextTerm()) {
            removeParen();
            String name = nextToken().trim();
            Expression value = parseTerm();
            let.put(name, value);
            removeParen();
        }
        removeParen();
        trim();
    }

    private boolean hasMoreLet() {
        return this.input.contains("(let");
    }

    private void getFirstLet() throws TermParserException {
        int idx = this.input.indexOf("(let");
        String prefix = this.input.substring(0, idx);
        this.input = this.input.substring(idx);
        String postfix = preparePostfix();
        parseOneLet();
        this.input = prefix + this.input + postfix;
    }

    private String preparePostfix() throws TermParserException{
        int endIdx = getEndOfLetTerm(this.input, 5,0)+1;
        String postfix = this.input.substring(endIdx);
        postfix = postfix.trim();
        this.input = this.input.substring(0,endIdx);
        //In case this was not the last let in the term,
        //we need to remove the closing bracket.
        endIdx = getEndOfLetTerm(postfix, 0,-1);
        postfix = postfix.substring(0,endIdx) + postfix.substring(endIdx+1);
        return postfix;
    }

    private int getEndOfLetTerm(String analysisPart, 
            int start, int breakCondition) throws TermParserException{
        int open = 0;
        int closed = 0;
        char[] inputs = analysisPart.toCharArray();
        //index 5 is the first opening break after (let 
        for(int index = start ; index < inputs.length; index++){
          char c = inputs[index];
          if(c == '('){
            ++open;
          }
          if(c == ')'){
            ++closed;
          }
          if(open-closed == breakCondition){
            return index;
          }
        }
        throw new TermParserException("Cannot find the end of the let term");
    }
}
