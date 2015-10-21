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
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.Constant;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.expressions.NumericCompound;
import gov.nasa.jpf.constraints.expressions.NumericOperator;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import org.testng.annotations.Test;

public class StreamTest {

    private final Constant zero = new Constant(BuiltinTypes.SINT32, 0);
    private final Constant one = new Constant(BuiltinTypes.SINT32, 1);
    private final Constant two = new Constant(BuiltinTypes.SINT32, 2);

    private final Variable s0 = new Variable(BuiltinTypes.SINT32 , "s_0");
    private final Variable s1 = new Variable(BuiltinTypes.SINT32 , "s_1");
        
    private final Variable sink0 = new Variable(BuiltinTypes.SINT32 , "sink_0");
    private final Variable cap0 = new Variable(BuiltinTypes.SINT32 , "cap_0");

    private final Variable sink1 = new Variable(BuiltinTypes.SINT32 , "sink_1");
    private final Variable cap1 = new Variable(BuiltinTypes.SINT32 , "cap_1");
    
    private final Variable sink2 = new Variable(BuiltinTypes.SINT32 , "sink_2");
    private final Variable cap2 = new Variable(BuiltinTypes.SINT32 , "cap_2");

    private final Variable sink3 = new Variable(BuiltinTypes.SINT32 , "sink_3");
    private final Variable cap3 = new Variable(BuiltinTypes.SINT32 , "cap_3");

    
    private Expression<Boolean> init(Variable sink, Variable cap) {
       Expression<Boolean> initSink = new NumericBooleanExpression(
                sink, NumericComparator.EQ, zero);
        
        Expression<Boolean> initCap = new NumericBooleanExpression(
                cap, NumericComparator.EQ, two);
        
        return ExpressionUtil.and(initCap, initSink);         
    } 
    
    private Expression<Boolean> connectGT(Variable sink_0, Variable sink_1, 
            Variable cap_0, Variable cap_1, Variable s) {
        
        return ExpressionUtil.and(
                new NumericBooleanExpression(s, NumericComparator.GT, zero),
                new NumericBooleanExpression(sink_0, NumericComparator.LE, zero),
                new NumericBooleanExpression(s, NumericComparator.EQ, sink_1),
                new NumericBooleanExpression(cap_0, NumericComparator.EQ, cap_1)
        );
    }
    
    private Expression<Boolean> write(Variable sink_0, Variable sink_1, 
            Variable cap_0, Variable cap_1) {

        return ExpressionUtil.and(
                new NumericBooleanExpression(sink_0, NumericComparator.GT, zero),
                new NumericBooleanExpression(cap_0, NumericComparator.GT, zero),
                new NumericBooleanExpression(sink_0, NumericComparator.EQ, sink_1),
                new NumericBooleanExpression(cap_0, NumericComparator.EQ, 
                        new NumericCompound(cap_1, NumericOperator.PLUS, one))
        );        
    }
          
    private Expression<Boolean> connectGTErrorUnsat(Variable sink_0, Variable s) {
        
        return ExpressionUtil.and(
                new NumericBooleanExpression(s, NumericComparator.GT, zero),
                new NumericBooleanExpression(sink_0, NumericComparator.LE, zero)
        );        
    }
    
    private Expression<Boolean> writeErrorUnsat(Variable sink_0, Variable cap_0) {
        
        return ExpressionUtil.or(
                new NumericBooleanExpression(sink_0, NumericComparator.LE, zero),
                new NumericBooleanExpression(cap_0, NumericComparator.LE, zero)        
        );       
    }
    
    private Expression<Boolean> writeOkUnsat(Variable sink_0, Variable cap_0) {
        
        return ExpressionUtil.and(
                new NumericBooleanExpression(sink_0, NumericComparator.GT, zero),
                new NumericBooleanExpression(cap_0, NumericComparator.GT, zero)
        );        
    }

    private void interpolate(Expression<Boolean> prefix, Expression<Boolean> suffix) {
        
        System.out.println("Prefix: " + prefix);
        System.out.println("Suffix: " + suffix);
       
        ArrayList<Expression<Boolean>> terms = new ArrayList<>();
        terms.add(prefix);
        terms.add(suffix);
  
        SMTInterpolSolver solver = new SMTInterpolSolver();
        Collection<Expression<Boolean>> interpolants = 
                solver.getInterpolants(terms);
        
        System.out.println(Arrays.toString(interpolants.toArray()));         
    }
    
    @Test
    public void testConnectConnect() {        
        System.out.println("connect - connect");
        Expression<Boolean> prefix = ExpressionUtil.and(
                init(sink0, cap0), 
                connectGT(sink0, sink1, cap0, cap1, s0));

        Expression<Boolean> suffix = connectGTErrorUnsat(sink1, s1);
        interpolate(prefix, suffix);     
    }
    
    @Test
    public void testConnectWrite() {
        System.out.println("connect - write");
        Expression<Boolean> prefix = ExpressionUtil.and(
                init(sink0, cap0), 
                connectGT(sink0, sink1, cap0, cap1, s0));

        Expression<Boolean> suffix = writeErrorUnsat(sink1, cap1);
        interpolate(prefix, suffix);
    }

    @Test
    public void testConnectWriteWrite() {
        System.out.println("connect - write write");
        Expression<Boolean> prefix = ExpressionUtil.and(
                init(sink0, cap0), 
                connectGT(sink0, sink1, cap0, cap1, s0));

        Expression<Boolean> suffix = ExpressionUtil.and(
                write(sink1, sink2, cap1, cap2),
                writeErrorUnsat(sink2, cap2)
        ); 
        interpolate(prefix, suffix);
    }

    @Test
    public void testConnectWrite_Write() {
        System.out.println("connect write - write");
        Expression<Boolean> prefix = ExpressionUtil.and(
                init(sink0, cap0), 
                connectGT(sink0, sink1, cap0, cap1, s0),
                write(sink1, sink2, cap1, cap2));

        Expression<Boolean> suffix = writeErrorUnsat(sink2, cap2);
        interpolate(prefix, suffix);
    }    
    
    @Test
    public void testConnectWriteWriteWrite() {
        System.out.println("connect - write write write");
        Expression<Boolean> prefix = ExpressionUtil.and(
                init(sink0, cap0), 
                connectGT(sink0, sink1, cap0, cap1, s0));

        Expression<Boolean> suffix = ExpressionUtil.and(
                write(sink1, sink2, cap1, cap2),
                write(sink2, sink3, cap2, cap3),
                writeOkUnsat(sink3, cap3)
        );
        interpolate(prefix, suffix);
    }    
    
}
