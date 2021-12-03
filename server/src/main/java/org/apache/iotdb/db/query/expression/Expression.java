/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.iotdb.db.query.expression;

import org.apache.iotdb.db.exception.query.LogicalOptimizeException;
import org.apache.iotdb.db.exception.query.QueryProcessException;
import org.apache.iotdb.db.metadata.path.PartialPath;
import org.apache.iotdb.db.qp.physical.crud.UDTFPlan;
import org.apache.iotdb.db.qp.utils.WildcardsRemover;
import org.apache.iotdb.db.query.expression.unary.ConstantOperand;
import org.apache.iotdb.db.query.udf.core.executor.UDTFExecutor;
import org.apache.iotdb.db.query.udf.core.layer.IntermediateLayer;
import org.apache.iotdb.db.query.udf.core.layer.LayerMemoryAssigner;
import org.apache.iotdb.db.query.udf.core.layer.RawQueryInputLayer;
import org.apache.iotdb.tsfile.file.metadata.enums.TSDataType;

import java.io.IOException;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** A skeleton class for expression */
public abstract class Expression {

  private String expressionStringCache;
  protected Boolean isConstantOperandCache = null;

  public boolean isAggregationFunctionExpression() {
    return false;
  }

  public boolean isTimeSeriesGeneratingFunctionExpression() {
    return false;
  }

  public abstract void concat(List<PartialPath> prefixPaths, List<Expression> resultExpressions);

  public abstract void removeWildcards(
      WildcardsRemover wildcardsRemover, List<Expression> resultExpressions)
      throws LogicalOptimizeException;

  public abstract void collectPaths(Set<PartialPath> pathSet);

  public abstract void constructUdfExecutors(
      Map<String, UDTFExecutor> expressionName2Executor, ZoneId zoneId);

  public abstract void updateStatisticsForMemoryAssigner(LayerMemoryAssigner memoryAssigner);

  public abstract IntermediateLayer constructIntermediateLayer(
      long queryId,
      UDTFPlan udtfPlan,
      RawQueryInputLayer rawTimeSeriesInputLayer,
      Map<Expression, IntermediateLayer> expressionIntermediateLayerMap,
      Map<Expression, TSDataType> expressionDataTypeMap,
      LayerMemoryAssigner memoryAssigner)
      throws QueryProcessException, IOException;

  /** Sub-classes should override this method indicating if the expression is a constant operand */
  protected abstract boolean isConstantOperandInternal();

  /** If this expression and all of its sub-expressions are {@link ConstantOperand}. */
  public final boolean isConstantOperand() {
    if (isConstantOperandCache == null) {
      isConstantOperandCache = isConstantOperandInternal();
    }
    return isConstantOperandCache;
  }

  /**
   * Sub-classes should override this method to provide valid string representation of this object.
   * See {@link #getExpressionString()}
   */
  protected abstract String getExpressionStringInternal();

  /**
   * Get the representation of the expression in string. The hash code of the returned value will be
   * the hash code of this object. See {@link #hashCode()} and {@link #equals(Object)}. In other
   * words, same expressions should have exactly the same string representation, and different
   * expressions must have different string representations.
   */
  public final String getExpressionString() {
    if (expressionStringCache == null) {
      expressionStringCache = getExpressionStringInternal();
    }
    return expressionStringCache;
  }

  /** Sub-classes must not override this method. */
  @Override
  public final int hashCode() {
    return getExpressionString().hashCode();
  }

  /** Sub-classes must not override this method. */
  @Override
  public final boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof Expression)) {
      return false;
    }

    return getExpressionString().equals(((Expression) o).getExpressionString());
  }

  /** Sub-classes must not override this method. */
  @Override
  public final String toString() {
    return getExpressionString();
  }
}