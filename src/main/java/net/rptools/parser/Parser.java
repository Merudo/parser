/*
 * This software Copyright by the RPTools.net development team, and
 * licensed under the Affero GPL Version 3 or, at your option, any later
 * version.
 *
 * RPTools Source Code is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You should have received a copy of the GNU Affero General Public
 * License * along with this source Code.  If not, please visit
 * <http://www.gnu.org/licenses/> and specifically the Affero license
 * text at <http://www.gnu.org/licenses/agpl.html>.
 */
package net.rptools.parser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.rptools.CaseInsensitiveHashMap;
import net.rptools.parser.function.Function;
import net.rptools.parser.function.impl.AbsoluteValue;
import net.rptools.parser.function.impl.Addition;
import net.rptools.parser.function.impl.And;
import net.rptools.parser.function.impl.Assignment;
import net.rptools.parser.function.impl.BitwiseAnd;
import net.rptools.parser.function.impl.BitwiseNot;
import net.rptools.parser.function.impl.BitwiseOr;
import net.rptools.parser.function.impl.BitwiseXor;
import net.rptools.parser.function.impl.Ceiling;
import net.rptools.parser.function.impl.Division;
import net.rptools.parser.function.impl.Equals;
import net.rptools.parser.function.impl.Eval;
import net.rptools.parser.function.impl.Floor;
import net.rptools.parser.function.impl.Greater;
import net.rptools.parser.function.impl.GreaterOrEqual;
import net.rptools.parser.function.impl.Hex;
import net.rptools.parser.function.impl.Hypotenuse;
import net.rptools.parser.function.impl.Lesser;
import net.rptools.parser.function.impl.LesserEqual;
import net.rptools.parser.function.impl.Ln;
import net.rptools.parser.function.impl.Log;
import net.rptools.parser.function.impl.Max;
import net.rptools.parser.function.impl.Mean;
import net.rptools.parser.function.impl.Median;
import net.rptools.parser.function.impl.Min;
import net.rptools.parser.function.impl.Multiplication;
import net.rptools.parser.function.impl.Not;
import net.rptools.parser.function.impl.NotEquals;
import net.rptools.parser.function.impl.Or;
import net.rptools.parser.function.impl.Power;
import net.rptools.parser.function.impl.Round;
import net.rptools.parser.function.impl.SquareRoot;
import net.rptools.parser.function.impl.StrEquals;
import net.rptools.parser.function.impl.StrNotEquals;
import net.rptools.parser.function.impl.Subtraction;
import net.rptools.parser.transform.Transformer;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.tree.ParseTree;

public class MapToolParser implements VariableResolver {
  private final Map<String, Function> functions = new CaseInsensitiveHashMap<Function>();

  private final List<Transformer> transforms = new ArrayList<Transformer>();

  private final EvaluationTreeParser evaluationTreeParser;

  private final VariableResolver variableResolver;

  ///////////////////////////////////////////////////////////////////////////
  // Constructor(s)
  ///////////////////////////////////////////////////////////////////////////

  public MapToolParser() {
    this(true);
  }

  public MapToolParser(boolean addDefaultFunctions) {
    this(null, addDefaultFunctions);
  }

  public MapToolParser(VariableResolver variableResolver, boolean addDefaultFunctions) {

    if (addDefaultFunctions) {
      addStandardOperators();
      addStandardMathFunctions();
      addBitwiseLogicFunctions();
      addLogicalFunctions();
      addExtraFunctions();
    }

    this.evaluationTreeParser = new EvaluationTreeParser(this);

    if (variableResolver == null) this.variableResolver = new MapVariableResolver();
    else this.variableResolver = variableResolver;
  }

  ///////////////////////////////////////////////////////////////////////////
  // Functions
  ///////////////////////////////////////////////////////////////////////////

  public void addStandardOperators() {
    addFunction(new Assignment());

    addFunction(new Addition());
    addFunction(new Subtraction());

    addFunction(new Multiplication());
    addFunction(new Division());

    addFunction(new Power());
  }

  public void addStandardMathFunctions() {
    addFunction(new AbsoluteValue());
    addFunction(new Ceiling());
    addFunction(new Floor());
    addFunction(new Hypotenuse());
    addFunction(new Max());
    addFunction(new Min());
    addFunction(new Round());
    addFunction(new SquareRoot());
    addFunction(new Mean());
    addFunction(new Median());
    addFunction(new Log());
    addFunction(new Ln());
  }

  public void addBitwiseLogicFunctions() {
    addFunction(new BitwiseAnd());
    addFunction(new BitwiseOr());
    addFunction(new BitwiseNot());
    addFunction(new BitwiseXor());
    addFunction(new Hex());
  }

  public void addLogicalFunctions() {
    addFunction(new Not());
    addFunction(new Or());
    addFunction(new And());
    addFunction(new Equals());
    addFunction(new NotEquals());
    addFunction(new Greater());
    addFunction(new GreaterOrEqual());
    addFunction(new Lesser());
    addFunction(new LesserEqual());
    addFunction(new StrEquals());
    addFunction(new StrNotEquals());
  }

  public void addExtraFunctions() {
    addFunction(new Eval());
  }

  public void addFunction(Function function) {
    for (String alias : function.getAliases()) {
      functions.put(alias, function);
    }
  }

  public void addFunctions(Function[] functions) {
    for (Function f : functions) {
      addFunction(f);
    }
  }

  public void addFunctions(List<Function> functions) {
    for (Function f : functions) {
      addFunction(f);
    }
  }

  public Function getFunction(String functionName) {
    return functions.get(functionName);
  }

  public Collection<Function> getFunctions() {
    return functions.values();
  }

  ///////////////////////////////////////////////////////////////////////////
  // Transforms
  ///////////////////////////////////////////////////////////////////////////
  public void addTransformer(Transformer t) {
    transforms.add(t);
  }

  private String applyTransforms(String expression) {
    String s = expression;
    for (Transformer trans : transforms) {
      s = trans.transform(s);
    }

    return s;
  }

  ///////////////////////////////////////////////////////////////////////////
  // Variable
  ///////////////////////////////////////////////////////////////////////////

  public VariableResolver getVariableResolver() {
    return variableResolver;
  }

  public boolean containsVariable(String name) throws ParserException {
    return variableResolver.containsVariable(name, VariableModifiers.None);
  }

  public void setVariable(String name, Object value) throws ParserException {
    variableResolver.setVariable(name, VariableModifiers.None, value);
  }

  public Object getVariable(String variableName) throws ParserException {
    return variableResolver.getVariable(variableName, VariableModifiers.None);
  }

  public boolean containsVariable(String name, VariableModifiers vType) throws ParserException {
    return variableResolver.containsVariable(name, vType);
  }

  public void setVariable(String name, VariableModifiers vType, Object value)
      throws ParserException {
    variableResolver.setVariable(name, vType, value);
  }

  public Object getVariable(String variableName, VariableModifiers vType) throws ParserException {
    return variableResolver.getVariable(variableName, vType);
  }

  @Override
  public Set<String> getVariables() {
    return variableResolver.getVariables();
  }

  public EvaluationTreeParser getEvaluationTreeParser() throws ParserException {
    return evaluationTreeParser;
  }

  ///////////////////////////////////////////////////////////////////////////
  // parseExpression
  ///////////////////////////////////////////////////////////////////////////

  public Expression parseExpression(String expression) throws ParserException {
    try {
      String s = applyTransforms(expression);

      ExpressionLexer lexer = new ExpressionLexer(new ANTLRInputStream(new ByteArrayInputStream(s.getBytes())));
      ExpressionParser parser = new ExpressionParser(new CommonTokenStream(lexer));
      ExpressionParser.ExpressionContext tree = parser.expression();

      parser.expression();

      return new Expression(this, parser, tree);

    } catch (RecognitionException e) {
      throw new ParserException(e);
    } catch (IOException e) {
      throw new ParserException(e);
    }
  }
}
