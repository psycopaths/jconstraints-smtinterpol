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

import de.uni_freiburg.informatik.ultimate.logic.Annotation;
import de.uni_freiburg.informatik.ultimate.logic.Logics;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import gov.nasa.jpf.constraints.api.ConstraintSolver;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.InterpolationSolver;
import gov.nasa.jpf.constraints.api.Valuation;
import gov.nasa.jpf.constraints.api.Variable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import org.apache.log4j.Level;

public class SMTInterpolSolver extends ConstraintSolver implements InterpolationSolver {

    private static final Logger logger = Logger.getLogger("constraints");
    
    @Override
    public Result solve(Expression<Boolean> f, Valuation result) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Expression<Boolean>> getInterpolants(List<Expression<Boolean>> exprsn) {
        
        org.apache.log4j.Logger smtLogger = 
                org.apache.log4j.Logger.getLogger(SMTInterpolSolver.class.getName());
        
        //System.out.println(Arrays.toString(exprsn.toArray()));
        
        Script s = new de.uni_freiburg.informatik.ultimate.
                smtinterpol.smtlib2.SMTInterpol(smtLogger);
        
        SMTInterpolExpressionGenerator gen = 
                new SMTInterpolExpressionGenerator(s);
        ArrayList<String> names = new ArrayList<>();

        s.setOption(":produce-interpolants", true);
        s.setLogic(Logics.QF_LIA);
        int i = 1;
        for (Expression<Boolean> e : exprsn) {            
            Term t = gen.generateAssertion(e);
            //System.out.println("T: " + t);
            String name = "phi_" + (i++);
            names.add(name);
            s.assertTerm(s.annotate(
                    t, new Annotation(":named", name )));
        }
       
        if (s.checkSat() == Script.LBool.UNSAT) {
            Term[] terms = new Term[names.size()];
            i=0;
            for (String n : names) {
                terms[i++] = s.term(n);
            }
                    
            Term[] interpolants;
            interpolants = s.getInterpolants(terms);            
            //System.out.println("I: " + Arrays.toString(interpolants));
            
            // parse result
            ArrayList<Expression<Boolean>> ret = new ArrayList<>();
            for (Term t : interpolants) {
                //System.out.println(t);
                TermParser parser = new TermParser(t, gen.getVariables());
                Expression<Boolean> interpolant = parser.parse();
                ret.add(interpolant);
            }
            
            return ret;
        }

        return null;
    }
}
