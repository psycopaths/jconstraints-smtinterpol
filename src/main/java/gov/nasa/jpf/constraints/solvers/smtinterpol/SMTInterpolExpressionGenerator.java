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

import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.AbstractExpressionVisitor;
import gov.nasa.jpf.constraints.expressions.BitvectorExpression;
import gov.nasa.jpf.constraints.expressions.BitvectorNegation;
import gov.nasa.jpf.constraints.expressions.CastExpression;
import gov.nasa.jpf.constraints.expressions.Constant;
import gov.nasa.jpf.constraints.expressions.Negation;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import static gov.nasa.jpf.constraints.expressions.NumericComparator.EQ;
import static gov.nasa.jpf.constraints.expressions.NumericComparator.GE;
import static gov.nasa.jpf.constraints.expressions.NumericComparator.GT;
import static gov.nasa.jpf.constraints.expressions.NumericComparator.LE;
import static gov.nasa.jpf.constraints.expressions.NumericComparator.LT;
import gov.nasa.jpf.constraints.expressions.NumericCompound;
import gov.nasa.jpf.constraints.expressions.NumericOperator;
import gov.nasa.jpf.constraints.expressions.PropositionalCompound;
import gov.nasa.jpf.constraints.expressions.QuantifierExpression;
import gov.nasa.jpf.constraints.expressions.UnaryMinus;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.types.IntegerType;
import gov.nasa.jpf.constraints.types.Type;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;

public class SMTInterpolExpressionGenerator extends AbstractExpressionVisitor<Term, Void> {

    private final Script script;
    private final Set<String> variables = new HashSet<>();
    private final Set<Variable<?>> vars = new HashSet<>();

    public SMTInterpolExpressionGenerator(Script script) {
        this.script = script;
    }

    public Term generateAssertion(Expression<Boolean> e) {
        return visit(e, null);
    }

    /* (non-Javadoc)
     * @see gov.nasa.jpf.constraints.expressions.AbstractExpressionVisitor#visit(gov.nasa.jpf.constraints.api.Variable, java.lang.Object)
     */
    @Override
    public <E> Term visit(Variable<E> v, Void data) {
        return getOrCreateVar(v);
    }

    /* (non-Javadoc)
     * @see gov.nasa.jpf.constraints.expressions.AbstractExpressionVisitor#visit(gov.nasa.jpf.constraints.expressions.Constant, java.lang.Object)
     */
    @Override
    public <E> Term visit(Constant<E> c, Void data) {
        Type<E> type = c.getType();
        if (type instanceof IntegerType) {
            return script.numeral(c.getValue().toString());
        }
        if (type instanceof BuiltinTypes.BoolType) {
            Constant zero = new Constant(BuiltinTypes.INTEGER, 0);
            Expression<Boolean> taut = new NumericBooleanExpression(
                    zero, NumericComparator.EQ, zero);
            
            return ((Boolean)c.getValue()) ? visit(taut) : visit(new Negation(taut));
        }
        throw new IllegalStateException("Cannot handle consts of type " + type);
    }

    /* (non-Javadoc)
     * @see gov.nasa.jpf.constraints.expressions.AbstractExpressionVisitor#visit(gov.nasa.jpf.constraints.expressions.Negation, java.lang.Object)
     */
    @Override
    public Term visit(Negation n, Void data) {
        Term negatedExpr = visit(n.getNegated());
        return script.term("not", negatedExpr);
    }

    /* (non-Javadoc)
     * @see gov.nasa.jpf.constraints.expressions.AbstractExpressionVisitor#visit(gov.nasa.jpf.constraints.expressions.NumericBooleanExpression, java.lang.Object)
     */
    @Override
    public Term visit(NumericBooleanExpression n, Void data) {

        NumericComparator cmp = n.getComparator();
        if (cmp == NumericComparator.NE) {
            return visit(new Negation(new NumericBooleanExpression(n.getLeft(), NumericComparator.EQ, n.getRight())));
        }

        Term left = null, right = null;
        left = visit(n.getLeft(), null);
        right = visit(n.getRight(), null);

        switch (cmp) {
            case EQ:
                return script.term("=", left, right);
            case GE:
                return script.term(">=", left, right);
            case GT:
                return script.term(">", left, right);
            case LE:
                return script.term("<=", left, right);
            case LT:
                return script.term("<", left, right);

            default:
                throw new UnsupportedOperationException("Comparator "
                        + cmp + " not supported");
        }
    }

    /* (non-Javadoc)
     * @see gov.nasa.jpf.constraints.expressions.AbstractExpressionVisitor#visit(gov.nasa.jpf.constraints.expressions.CastExpression, java.lang.Object)
     */
    @Override
    public <F, E> Term visit(CastExpression<F, E> cast, Void data) {
        Expression<F> casted = cast.getCasted();
        Type<F> ft = casted.getType();
        Type<E> tt = cast.getType();
        throw new IllegalStateException("Cannot handle cast from " + ft + " to " + tt);
    }

    /* (non-Javadoc)
     * @see gov.nasa.jpf.constraints.expressions.AbstractExpressionVisitor#visit(gov.nasa.jpf.constraints.expressions.NumericCompound, java.lang.Object)
     */
    @Override
    public <E> Term visit(NumericCompound<E> n, Void data) {
        Term left = null, right = null;
        left = visit(n.getLeft());
        right = visit(n.getRight());

        NumericOperator op = n.getOperator();
        switch (op) {
            case PLUS:
                return script.term("+", left, right);
            case MINUS:
                return script.term("-", left, right);
            case MUL:
                return script.term("*", left, right);
            case DIV:
                return script.term("/", left, right);
            case REM:
                return script.term("%", left, right);
            default:
                throw new IllegalArgumentException("Cannot handle numeric operator " + op);
        }
    }

    /* (non-Javadoc)
     * @see gov.nasa.jpf.constraints.expressions.AbstractExpressionVisitor#visit(gov.nasa.jpf.constraints.expressions.PropositionalCompound, java.lang.Object)
     */
    @Override
    public Term visit(PropositionalCompound n, Void data) {
        Term left = null, right = null;
        left = visit(n.getLeft(), null);
        right = visit(n.getRight(), null);

        switch (n.getOperator()) {
            case AND:
                return script.term("and", left, right);
            case OR:
                return script.term("or", left, right);
            case EQUIV:
              return script.term("=", left, right);
//      case IMPLY:
//        return script.term("+", left, right);
//      case XOR:
//        return script.term("+", left, right);
            default:
                throw new IllegalStateException("Cannot handle propositional operator " + n.getOperator());

        }
    }

    /* (non-Javadoc)
     * @see gov.nasa.jpf.constraints.expressions.AbstractExpressionVisitor#visit(gov.nasa.jpf.constraints.expressions.UnaryMinus, java.lang.Object)
     */
    @Override
    public <E> Term visit(UnaryMinus<E> n, Void data) {
        Term negated = visit(n.getNegated(), null);
        return script.term("-", negated);
    }

    /* (non-Javadoc)
     * @see gov.nasa.jpf.constraints.expressions.AbstractExpressionVisitor#visit(gov.nasa.jpf.constraints.expressions.QuantifierExpression, java.lang.Object)
     */
    @Override
    public Term visit(QuantifierExpression q, Void data) {
        throw new IllegalStateException("Cannot handle cast quantified expressions");
    }

    /* (non-Javadoc)
     * @see gov.nasa.jpf.constraints.expressions.AbstractExpressionVisitor#visit(gov.nasa.jpf.constraints.expressions.BitvectorExpression, java.lang.Object)
     */
    @Override
    public <E> Term visit(BitvectorExpression<E> bv, Void data) {
        throw new IllegalStateException("Cannot handle cast bitvector expressions");
    }

    /* (non-Javadoc)
     * @see gov.nasa.jpf.constraints.expressions.AbstractExpressionVisitor#visit(gov.nasa.jpf.constraints.expressions.BitvectorNegation, java.lang.Object)
     */
    @Override
    public <E> Term visit(BitvectorNegation<E> n, Void data) {
        throw new IllegalStateException("Cannot handle cast bitvector expressions");
    }

    private Term getOrCreateVar(Variable<?> v) {
        Type<?> type = v.getType();

        if (type instanceof IntegerType) {
            return getOrCreateIntVar((Variable<?>) v);
        }
        if (type instanceof BuiltinTypes.BoolType) {
            return getOrCreateBoolVar((Variable<?>) v);
        }        
        throw new IllegalArgumentException("Cannot handle variable type " + type);
    }

    private Term getOrCreateIntVar(Variable<?> v) {
        if (!this.variables.contains(v.getName())) {
            createIntVar(v);
            this.variables.add(v.getName());
            this.vars.add(v);
        }
        return script.term(v.getName());
    }
    
    private Term getOrCreateBoolVar(Variable<?> v) {
        if (!this.variables.contains(v.getName())) {
            createBoolVar(v);
            this.variables.add(v.getName());
            this.vars.add(v);
        }
        return script.term(v.getName());
    }    
    
    protected void createBoolVar(Variable<?> v) {
        script.declareFun(v.getName(), new Sort[0], script.sort("Bool"));
    }

    protected void createIntVar(Variable<?> v) {
        script.declareFun(v.getName(), new Sort[0], script.sort("Int"));

        IntegerType<?> type = (IntegerType<?>) v.getType();
        BigInteger min = type.getMinInt();
        BigInteger max = type.getMaxInt();

        // assert bounds
        if (min != null) {
            Term mint = script.term(">=", script.term(v.getName()), script.numeral(min));
            script.assertTerm(mint);
        }
        if (max != null) {
            Term maxt = script.term("<=", script.term(v.getName()), script.numeral(max));
            script.assertTerm(maxt);
        }
    }

    /**
     * @return the variables
     */
    public Set<Variable<?>> getVariables() {
        return vars;
    }
}
