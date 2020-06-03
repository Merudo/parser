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
package net.rptools.parser.function.impl;

import java.math.BigDecimal;
import java.util.List;
import net.rptools.parser.MapToolParser;
import net.rptools.parser.function.AbstractNumberFunction;
import net.rptools.parser.function.EvaluationException;
import net.rptools.parser.function.ParameterException;

public class SquareRoot extends AbstractNumberFunction {
  private static final int DEFAULT_SCALE = 10;
  private static final BigDecimal TWO = new BigDecimal(2);

  public SquareRoot() {
    super(1, 2, "sqrt", "squareroot");
  }

  @Override
  public Object childEvaluate(MapToolParser parser, String functionName, List<Object> parameters)
      throws EvaluationException, ParameterException {
    int scale = DEFAULT_SCALE;
    if (parameters.size() == 2) {
      scale = ((BigDecimal) parameters.get(1)).intValue();
    }

    BigDecimal value = (BigDecimal) parameters.get(0);
    return sqrt(value, scale);
  }

  //  the Babylonian square root method (Newton's method)
  private BigDecimal sqrt(BigDecimal value, final int scale) {
    BigDecimal x0 = new BigDecimal("0");
    BigDecimal x1 = BigDecimal.valueOf(Math.sqrt(value.doubleValue()));

    while (!x0.equals(x1)) {
      x0 = x1;
      x1 = value.divide(x0, scale, BigDecimal.ROUND_HALF_UP);
      x1 = x1.add(x0);
      x1 = x1.divide(TWO, scale, BigDecimal.ROUND_HALF_UP);
    }

    return x1;
  }
}
