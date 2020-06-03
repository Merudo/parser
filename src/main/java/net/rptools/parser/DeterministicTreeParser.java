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
import org.antlr.runtime.tree.Tree;

public class DeterministicTreeParser {
  private static final Logger log = Logger.getLogger(EvaluationTreeParser.class.getName());

  private final MapToolParser parser;
  private final ExpressionParser xParser;

  public DeterministicTreeParser(MapToolParser parser, ExpressionParser xParser) {
    this.parser = parser;
    this.xParser = xParser;
  }

  public Tree evaluate(Tree node) throws ParserException {
    if (node == null) return null;

    switch (node.getType()) {
      case STRING:
      case NUMBER:
      case HEXNUMBER:
      case ASSIGNEE:
      case TRUE:
      case FALSE:
        setRightSibling(node, (evaluate(getRightSibling(node))));
        return node;
      case VARIABLE:
      case PROMPTVARIABLE:
        {
          String name = node.getText();
          if (!parser.containsVariable(name, VariableModifiers.None)) {
            throw new EvaluationException(String.format("Undefined variable: %s", name));
          }
          Object value = parser.getVariable(node.getText(), VariableModifiers.None);

          if (log.isLoggable(Level.FINEST))
            log.finest(String.format("VARIABLE: name=%s, value=%s\n", node.getText(), value));

          Tree newNode = createNode(value);
          node.getParent().setChild(GetNodeIndex(node), newNode);
          setRightSibling(newNode, (evaluate(getRightSibling(newNode))));

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

            Tree newNode = createNode(value);
            node.getParent().setChild(GetNodeIndex(node), newNode);
            setRightSibling(newNode, (evaluate(getRightSibling(newNode))));

            return newNode;
          } else {
            node.setChild(0, evaluate(node.getChild(0)));
            setRightSibling(node, (evaluate(getRightSibling(node))));
            return node;
          }
        }
      default:
        throw new EvaluationException(
            String.format("Unknown node type: name=%s, type=%d", node.getText(), node.getType()));
    }
  }

  private Tree createNode(Object value) {
    Tree newNode;

    if (value instanceof BigDecimal) {
      newNode = (Tree) xParser.getTreeAdaptor().create(NUMBER, value.toString());
    } else {
      newNode = (Tree) xParser.getTreeAdaptor().create(STRING, value.toString());
    }
    return newNode;
  }

  public static void setRightSibling(Tree node, Tree sibling) {
    int index = GetNodeIndex(node);

    if (index < 0) return;

    node.getParent().setChild(index + 1, sibling);
  }

  public static Tree getRightSibling(Tree node) {
    int index = GetNodeIndex(node);

    return index >= 0 && index < node.getParent().getChildCount() - 1
        ? node.getParent().getChild(index + 1)
        : null;
  }

  public static int GetNodeIndex(Tree node) {
    Tree parent = node.getParent();

    if (parent == null) return -1;

    for (int i = 0; i < parent.getChildCount(); i++) {
      if (parent.getChild(i).equals(node)) {
        return i;
      }
    }

    return -1;
  }
}
