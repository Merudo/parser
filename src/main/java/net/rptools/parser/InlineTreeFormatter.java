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
import static net.rptools.parser.ExpressionParser.FUNCTION;
import static net.rptools.parser.ExpressionParser.HEXNUMBER;
import static net.rptools.parser.ExpressionParser.NUMBER;
import static net.rptools.parser.ExpressionParser.OPERATOR;
import static net.rptools.parser.ExpressionParser.STRING;
import static net.rptools.parser.ExpressionParser.UNARY_OPERATOR;
import static net.rptools.parser.ExpressionParser.VARIABLE;

import java.util.HashMap;
import java.util.Map;
import net.rptools.parser.function.EvaluationException;
import org.antlr.runtime.tree.Tree;

public class InlineTreeFormatter {

  private static final Map<String, Integer> ORDER_OF_OPERATIONS = new HashMap<>();

  static {
    // P(1) E(2) MD(3) AS(4):
    ORDER_OF_OPERATIONS.put("=", 0);
    ORDER_OF_OPERATIONS.put("^", 2);
    ORDER_OF_OPERATIONS.put("*", 3);
    ORDER_OF_OPERATIONS.put("/", 3);
    ORDER_OF_OPERATIONS.put("+", 4);
    ORDER_OF_OPERATIONS.put("-", 4);
  }

  public String format(Tree node) throws EvaluationException {
    StringBuilder sb = new StringBuilder();
    format(node, sb);

    return sb.toString();
  }

  private void format(Tree node, StringBuilder sb) throws EvaluationException {
    if (node == null) return;

    switch (node.getType()) {
      case ASSIGNEE:
      case STRING:
      case VARIABLE:
      case NUMBER:
      case HEXNUMBER:
        {
          sb.append(node.getText());
          return;
        }
      case UNARY_OPERATOR:
        {
          if (!"+".equals(node.getText())) {
            sb.append(node.getText());
          }
          format(node.getChild(0), sb);
          return;
        }
      case OPERATOR:
        {
          int currentLevel = ORDER_OF_OPERATIONS.get(node.getText());

          for (int i = 0; i < node.getChildCount(); i++) {
            Tree child = node.getChild(i);
            if (child.getType() == OPERATOR) {
              int childLevel = ORDER_OF_OPERATIONS.get(child.getText());
              if (currentLevel < childLevel) sb.append("(");
              format(child, sb);
              if (currentLevel < childLevel) sb.append(")");
            } else {
              format(child, sb);
            }
            if (i < node.getChildCount() - 1) {
              sb.append(' ').append(node.getText()).append(' ');
            }
          }

          return;
        }
      case FUNCTION:
        {
          sb.append(node.getText()).append("(");
          for (int i = 0; i < node.getChildCount(); i++) {
            Tree child = node.getChild(i);
            format(child, sb);
            if (i < node.getChildCount() - 1) {
              sb.append(", ");
            }
          }
          sb.append(")");
          return;
        }
      default:
        throw new EvaluationException(
            String.format("Unknown node type: name=%s, type=%d", node.getText(), node.getType()));
    }
  }
}
