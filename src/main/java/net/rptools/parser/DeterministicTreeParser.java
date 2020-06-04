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

import static net.rptools.parser.ExpressionParser.ASSIGNEE;
import static net.rptools.parser.ExpressionParser.FALSE;
import static net.rptools.parser.ExpressionParser.FUNCTION;
import static net.rptools.parser.ExpressionParser.HEXNUMBER;
import static net.rptools.parser.ExpressionParser.NUMBER;
import static net.rptools.parser.ExpressionParser.OPERATOR;
import static net.rptools.parser.ExpressionParser.PROMPTVARIABLE;
import static net.rptools.parser.ExpressionParser.STRING;
import static net.rptools.parser.ExpressionParser.TRUE;
import static net.rptools.parser.ExpressionParser.UNARY_OPERATOR;
import static net.rptools.parser.ExpressionParser.VARIABLE;

import java.math.BigDecimal;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.rptools.parser.function.EvaluationException;
import net.rptools.parser.function.Function;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

public class DeterministicTreeParser {
  private static final Logger log = Logger.getLogger(EvaluationTreeParser.class.getName());

  private final MapToolParser parser;
  private final ExpressionParser xParser;

  public DeterministicTreeParser(MapToolParser parser, ExpressionParser xParser) {
    this.parser = parser;
    this.xParser = xParser;
  }

  public ParseTree evaluate(ParseTree node) throws ParserException {
    Token token = null;
    if (node instanceof ParserRuleContext) {
      token = ((ParserRuleContext) node).getStart(); // or #getStop
    } else if (node instanceof TerminalNode) {        // TerminalNodeImpl or ErrorNode
      token = ((TerminalNode) node).getSymbol();
    }

    if (token == null) return null;

    switch (token.getType()) {
      case STRING:
      case NUMBER:
      case HEXNUMBER:
      case ASSIGNEE:
      case TRUE:
      case FALSE:
        node.setNextSibling(evaluate(node.getNextSibling()));
        return node;
      case VARIABLE:
        {
          String name = node.getText();
          if (!parser.containsVariable(name, VariableModifiers.None)) {
            throw new EvaluationException(String.format("Undefined variable: %s", name));
          }
          Object value = parser.getVariable(node.getText(), VariableModifiers.None);

          if (log.isLoggable(Level.FINEST))
            log.finest(String.format("VARIABLE: name=%s, value=%s\n", node.getText(), value));

          AST newNode = createNode(value);
          newNode.setNextSibling(evaluate(node.getNextSibling()));

          return newNode;
        }
      case PROMPTVARIABLE:
        {
          String name = node.getText();
          if (!parser.containsVariable(name, VariableModifiers.None)) {
            throw new EvaluationException(String.format("Undefined variable: %s", name));
          }
          Object value = parser.getVariable(node.getText(), VariableModifiers.None);

          if (log.isLoggable(Level.FINEST))
            log.finest(String.format("VARIABLE: name=%s, value=%s\n", node.getText(), value));

          AST newNode = createNode(value);
          newNode.setNextSibling(evaluate(node.getNextSibling()));

          return newNode;
        }
      case UNARY_OPERATOR:
      case OPERATOR:
      case FUNCTION:
        {
          String name = node.getText();
          Function function = parser.getFunction(node.getText());
          if (function == null) {
            throw new EvaluationException(String.format("Undefined function: %s", name));
          }

          if (!function.isDeterministic()) {
            Object value = parser.getEvaluationTreeParser().evaluate(node);

            AST newNode = createNode(value);
            newNode.setNextSibling(evaluate(node.getNextSibling()));

            return newNode;
          } else {
            node.setFirstChild(evaluate(node.getFirstChild()));
            node.setNextSibling(evaluate(node.getNextSibling()));
            return node;
          }
        }
      default:
        throw new EvaluationException(
            String.format("Unknown node type: name=%s, type=%d", node.getText(), node.getType()));
    }
  }

  private ParserRuleContext createNode(Object value) {
    AST newNode = xParser.getASTFactory().create();

    if (value instanceof BigDecimal) {
      newNode.setType(NUMBER);
      newNode.setText(value.toString());
    } else {
      newNode.setType(STRING);
      newNode.setText(value.toString());
    }

    return newNode;
  }

  public static void setRightSibling(ParserRuleContext context){
    int index = GetNodeIndex(context);

    if (index < 0) return;

    if (index < context.parent.getChildCount() - 1){
      context.parent.getChild(index+1) = context;
    }
  }

  public static ParseTree GetRightSibling(ParserRuleContext context)
  {
    int index = GetNodeIndex(context);

    return index >= 0 && index < context.parent.getChildCount() - 1
            ? context.parent.getChild(index + 1)
            : null;
  }

  public static int GetNodeIndex(ParserRuleContext context)
  {
    RuleContext parent = context.parent;

    if (parent == null)
      return -1;

    for (int i = 0; i < parent.getChildCount(); i++) {
      if (parent.getChild(i) == context) {
        return i;
      }
    }

    return -1;
  }
}
