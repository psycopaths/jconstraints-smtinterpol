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

import gov.nasa.jpf.constraints.solvers.smtinterpol.SMTInterpolSolver;
import de.uni_freiburg.informatik.ultimate.logic.Annotation;
import de.uni_freiburg.informatik.ultimate.logic.Logics;
import de.uni_freiburg.informatik.ultimate.logic.SMTLIBException;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Script.LBool;
import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.smtinterpol.smtlib2.SMTInterpol;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.Constant;
import gov.nasa.jpf.constraints.expressions.LogicalOperator;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.expressions.PropositionalCompound;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.testng.annotations.Test;

public class SMTInterpolTest {

    @Test
    public void test1() {
        System.out.println("--- test 1");
        try {
            Script s = new SMTInterpol(Logger.getRootLogger(), true);
            s.setOption(":produce-interpolants", true);
            s.setLogic(Logics.QF_LIA);
            s.declareFun("x", new Sort[0], s.sort("Int"));
            s.declareFun("y", new Sort[0], s.sort("Int"));
            s.assertTerm(s.annotate(
                    s.term(">", s.term("x"), s.term("y")),
                    new Annotation(":named", "phi_1")));
            s.assertTerm(s.annotate(
                    s.term("=", s.term("x"), s.numeral("0")),
                    new Annotation(":named", "phi_2")));
            s.assertTerm(s.annotate(
                    s.term(">", s.term("y"), s.numeral("0")),
                    new Annotation(":named", "phi_3")));
            if (s.checkSat() == LBool.UNSAT) {
                Term[] interpolants;
                interpolants = s.getInterpolants(new Term[]{
                    s.term("phi_1"),
                    s.term("phi_2"),
                    s.term("phi_3")});
                System.out.println(Arrays.toString(interpolants));
                interpolants = s.getInterpolants(new Term[]{
                    s.term("phi_2"),
                    s.term("and", s.term("phi_1"), s.term("phi_3"))});
                System.err.println(Arrays.toString(interpolants));
            }
        } catch (SMTLIBException ex) {
            System.out.println("unknown");
            ex.printStackTrace(System.err);
        }
    }

    @Test
    public void test2() {
        
        System.out.println("--- test 2");
        
//        s.declareFun("x", new Sort[0], s.sort("Int"));
//        s.declareFun("y", new Sort[0], s.sort("Int"));
        Variable x = new Variable(BuiltinTypes.SINT32 , "x");
        Variable y = new Variable(BuiltinTypes.SINT32 , "y");

//        s.assertTerm(s.annotate(
//                s.term(">", s.term("x"), s.term("y")),
//                new Annotation(":named", "phi_1")));
        Expression<Boolean> phi_1 = new NumericBooleanExpression(
                x, NumericComparator.GT, y);

//        s.assertTerm(s.annotate(
//                s.term("=", s.term("x"), s.numeral("0")),
//                new Annotation(":named", "phi_2")));        
        Expression<Boolean> phi_2 = new NumericBooleanExpression(
                x, NumericComparator.EQ, Constant.createParsed(BuiltinTypes.SINT32, "0"));

//        s.assertTerm(s.annotate(
//                s.term(">", s.term("y"), s.numeral("0")),
//                new Annotation(":named", "phi_3")));
        Expression<Boolean> phi_3 = new NumericBooleanExpression(
                y, NumericComparator.GT, Constant.createParsed(BuiltinTypes.SINT32, "0"));
 
        ArrayList<Expression<Boolean>> terms = new ArrayList<>();
        terms.add(phi_1);
        terms.add(phi_2);
        terms.add(phi_3);
        
        SMTInterpolSolver solver = new SMTInterpolSolver();
        Collection<Expression<Boolean>> interpolants = 
                solver.getInterpolants(terms);
        
        System.out.println(Arrays.toString(interpolants.toArray()));

        terms.clear();
        terms.add(phi_2);
        terms.add(new PropositionalCompound(phi_1, LogicalOperator.AND, phi_3));
        interpolants = solver.getInterpolants(terms);
        
        System.out.println(Arrays.toString(interpolants.toArray()));
        
    }
    
    @Test
    public void test3() {
        
        System.out.println("--- test 3");
        ArrayList<Expression<Boolean>> terms = new ArrayList<>();
        Variable x1 = new Variable(BuiltinTypes.SINT32 , "this.sink");
        Variable x2 = new Variable(BuiltinTypes.SINT32 , "this.sinkConnected");
        Variable x3 = new Variable(BuiltinTypes.SINT32 , "snk");
        Variable x4 = new Variable(BuiltinTypes.SINT32 , "conn");
        
        //(true && (((('this.sink' == 0) && ('this.sinkConnected' == 0)) && ('snk' == 0)) && ('conn' == 0)))
       
        Expression<Boolean> phi_2 = ExpressionUtil.and(
                ExpressionUtil.and(
                        ExpressionUtil.and(
                            new NumericBooleanExpression(
                                    x1, NumericComparator.EQ, Constant.createParsed(BuiltinTypes.SINT32, "0")),
                            new NumericBooleanExpression(
                                    x2, NumericComparator.EQ, Constant.createParsed(BuiltinTypes.SINT32, "0"))),
                        new NumericBooleanExpression(
                                x3, NumericComparator.EQ, Constant.createParsed(BuiltinTypes.SINT32, "0"))),
                    new NumericBooleanExpression(
                            x4, NumericComparator.EQ, Constant.createParsed(BuiltinTypes.SINT32, "0")));
                        

        terms.add(ExpressionUtil.and(ExpressionUtil.TRUE, phi_2));
        terms.add(ExpressionUtil.FALSE);
        
        SMTInterpolSolver solver = new SMTInterpolSolver();
        Collection<Expression<Boolean>> interpolants = 
                solver.getInterpolants(terms);
        
        System.out.println(Arrays.toString(interpolants.toArray()));

    }
    
    @Test
    public void test4() {
        System.out.println("--- test 4");
        ArrayList<Expression<Boolean>> terms = new ArrayList<>();
        Variable x1 = new Variable(BuiltinTypes.BOOL , "x1");
        Variable x2 = new Variable(BuiltinTypes.BOOL , "x2");
        
        Expression<Boolean> phi_2 =new PropositionalCompound(x1, LogicalOperator.AND, x2);
        terms.add(phi_2);
        terms.add(ExpressionUtil.FALSE);
        
        SMTInterpolSolver solver = new SMTInterpolSolver();
        Collection<Expression<Boolean>> interpolants = 
                solver.getInterpolants(terms);
        
        System.out.println(Arrays.toString(interpolants.toArray()));

        
    }
}
