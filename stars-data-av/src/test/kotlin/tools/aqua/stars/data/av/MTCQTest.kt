/*
 * Copyright 2023-2025 The STARS Project Authors
 * SPDX-License-Identifier: Apache-2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tools.aqua.stars.data.av

import kotlin.test.Test
import tools.aqua.stars.core.evaluation.BinaryPredicate.Companion.predicate
import tools.aqua.stars.core.evaluation.*
import tools.aqua.stars.core.metric.metrics.evaluation.*
import tools.aqua.stars.core.evaluation.UnaryPredicate.Companion.predicate
import tools.aqua.stars.core.tsc.builder.tsc
import tools.aqua.stars.data.av.dataclasses.*
import tools.aqua.stars.logic.mtcq.DLConverter
import tools.aqua.stars.logic.mtcq.mtcq


val vehicle1 = emptyVehicle(id = 0, egoVehicle = true)
val vehicle2 = emptyVehicle(id = 1, egoVehicle = false)
val tickData1 = emptyTickData(currentTick = TickDataUnitSeconds(0.0), actors = listOf(vehicle1, vehicle2))

// Change egoVehicle flag
val changedVehicle1 = emptyVehicle(id = 0, egoVehicle = true,  positionOnLane = 0.5)
val changedVehicle2 = emptyVehicle(id = 1, egoVehicle = false,  positionOnLane = 0.6)
val tickData2 = emptyTickData(currentTick = TickDataUnitSeconds(0.1), actors = listOf(changedVehicle1, changedVehicle2))

// Change egoVehicle flag
val changedVehicle21 = emptyVehicle(id = 0, egoVehicle = true, positionOnLane = 1.0)
val changedVehicle22 = emptyVehicle(id = 1, egoVehicle = false,  positionOnLane = 2.0)
val tickData3 = emptyTickData(currentTick = TickDataUnitSeconds(0.2), actors = listOf(changedVehicle21, changedVehicle22))

val segment = Segment(segmentSource = "", mainInitList = listOf(tickData1, tickData2, tickData3), simulationRunId = "1")

val soBetween =
  predicate(Vehicle::class to Vehicle::class) { _, v0, v1 ->
    v1.tickData.vehicles
      .filter { it.id != v0.id && it.id != v1.id }
      .any { vx ->
        (v0.lane.uid == vx.lane.uid || v1.lane.uid == vx.lane.uid) &&
            (v0.lane.uid != vx.lane.uid || (v0.positionOnLane < vx.positionOnLane)) &&
            (v1.lane.uid != vx.lane.uid || (v1.positionOnLane > vx.positionOnLane))
      }
  }

val testMtcqPred =
  predicate(Vehicle::class) { ctx, v ->
    mtcq(ctx, DLConverter(), "last")
  }

val myTsc = tsc<Actor, TickData, Segment, TickDataUnitSeconds, TickDataDifferenceSeconds> {
  all("TSC Root") {
    leaf("someone between") {
      condition { ctx ->
        ctx.segment.vehicleIds.any { v1 ->
          soBetween.holds(ctx, ctx.segment.ticks.keys.first(), ctx.primaryEntityId, v1)
        }
      }
    }
    leaf("test mtcq") { condition { ctx -> testMtcqPred.holds(ctx) } }
  }
}

val evaluation =
  TSCEvaluation<Actor, TickData, Segment, TickDataUnitSeconds, TickDataDifferenceSeconds>(
    tscList = listOf(myTsc),
    writePlots = true,
    writePlotDataCSV = true,
    writeSerializedResults = true,
    compareToBaselineResults = false,
    compareToPreviousRun = false)
    .apply {
      registerMetricProviders(
        TotalSegmentTickDifferencePerIdentifierMetric(),
        SegmentCountMetric(),
        TotalSegmentTickDifferenceMetric(),
        InvalidTSCInstancesPerTSCMetric(),
        MissedTSCInstancesPerTSCMetric(),
      )
      println("Run Evaluation")
      runEvaluation(segments = listOf(segment).asSequence())
    }
