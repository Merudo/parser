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

import java.util.List;
import net.rptools.parser.MapToolParser;
import net.rptools.parser.function.AbstractLogicalOperatorFunction;
import net.rptools.parser.function.EvaluationException;
import net.rptools.parser.function.ParameterException;

public class Not extends AbstractLogicalOperatorFunction {
  public Not() {
    super(1, 1, "not", "!");
  }

  @Override
  public Object childEvaluate(MapToolParser parser, String functionName, List<Object> parameters)
      throws EvaluationException, ParameterException {
    return BooleanAsBigDecimal(!ConvertToBoolean(parameters.get(0)));
  }
}
