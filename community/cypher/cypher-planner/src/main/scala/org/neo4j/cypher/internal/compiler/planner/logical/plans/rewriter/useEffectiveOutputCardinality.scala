/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.cypher.internal.compiler.planner.logical.plans.rewriter

import org.neo4j.cypher.internal.compiler.planner.logical.CardinalityCostModel
import org.neo4j.cypher.internal.logical.plans.LogicalPlan
import org.neo4j.cypher.internal.planner.spi.PlanningAttributes.Cardinalities
import org.neo4j.cypher.internal.util.Rewriter
import org.neo4j.cypher.internal.util.Selectivity
import org.neo4j.cypher.internal.util.WorkReduction
import org.neo4j.cypher.internal.util.attribution.Attributes
import org.neo4j.cypher.internal.util.attribution.Id
import org.neo4j.cypher.internal.util.topDown

import scala.collection.mutable


/**
 * Traverse the LogicalPlan and update cardinalities to "effective cardinalities". The update is meant to represent how we, in certain cases, "push down" a
 * LIMIT to earlier operations.
 *
 */
case class useEffectiveOutputCardinality(cardinalities: Cardinalities, attributes: Attributes[LogicalPlan]) extends Rewriter {

  override def apply(input: AnyRef): AnyRef = {
    val workReductions: mutable.Map[Id, WorkReduction] = mutable.Map().withDefaultValue(WorkReduction.NoReduction)

    val rewriter: Rewriter = {
      topDown(Rewriter.lift {
        case p: LogicalPlan =>
          val reduction = workReductions(p.id)
          val effectiveCardinalities = CardinalityCostModel.effectiveCardinalities(p, reduction, cardinalities)

          p.lhs.foreach { lhs => workReductions += (lhs.id -> effectiveCardinalities.lhsReduction) }
          p.rhs.foreach { rhs => workReductions += (rhs.id -> effectiveCardinalities.rhsReduction) }

          // No need to create a new plan if we do not have a work reduction
          if (reduction == WorkReduction.NoReduction) {
            p
          } else {
            val newP = p.copyPlanWithIdGen(attributes.copy(p.id))
            cardinalities.set(newP.id, effectiveCardinalities.outputCardinality)
            newP
          }
      })
    }

    rewriter.apply(input)
  }
}
