package tools.aqua.stars.logic.mtcq

import openllet.core.KnowledgeBaseImpl
import openllet.mtcq.engine.MTCQNormalFormEngine
import openllet.mtcq.model.kb.InMemoryTemporalKnowledgeBaseImpl
import openllet.mtcq.model.kb.TemporalKnowledgeBase
import openllet.mtcq.parser.MetricTemporalConjunctiveQueryParser
import openllet.query.sparqldl.model.results.QueryResult
import tools.aqua.stars.core.types.EntityType
import tools.aqua.stars.core.types.SegmentType
import tools.aqua.stars.core.types.TickDataType
import tools.aqua.stars.core.types.TickDifference
import tools.aqua.stars.core.types.TickUnit
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

class MTCQEvaluator<
    E : EntityType<E, T, S, U, D>,
    T : TickDataType<E, T, S, U, D>,
    S : SegmentType<E, T, S, U, D>,
    U : TickUnit<U, D>,
    D : TickDifference<D>> {

  // Only stores the last TKB to save memory
  private val lastTkbCache = mutableMapOf<S, TemporalKnowledgeBase>()

  fun eval(segment: S, mtcqString: String): QueryResult {
    val tkb = toTKB(segment)
    val mtcq = MetricTemporalConjunctiveQueryParser.parse(mtcqString, tkb)
    println("MTCQ evaluation called for TKB of size " + tkb.size)
    val eng = MTCQNormalFormEngine()
    val res = eng.exec(mtcq)
    println("Result is: $res")
    return res
  }

  private fun toTKB(segment: S): TemporalKnowledgeBase {
    lastTkbCache[segment]?.let {
      return it
    }

    // New segment - clear cache
    lastTkbCache.clear()

    val tkb = InMemoryTemporalKnowledgeBaseImpl()
    segment.tickData.forEach { tick ->
      println("Initializing TKB for tick " + tick.currentTick)
      val kb = KnowledgeBaseImpl()
      tick.entities.forEach { entity ->
        if (entity is DLConvertible)
          entity.addToKB(kb)
      }

      for (prop in (tick::class as KClass<T>).memberProperties) {
        if (prop.name == "entities")
          break
        val data = prop.get(tick)
        if (data is Iterable<*>)
          data.forEach { entity ->
            if (entity is DLConvertible)
              entity.addToKB(kb)
          }
        if (data is DLConvertible)
          data.addToKB(kb)
      }

      // TODO this misses some other members of it (e.g., weather, daytime, trafficLights)
      // could be solved be reflection to check if some other property is DLConvertible
      tkb.add(kb)
    }

    lastTkbCache[segment] = tkb
    return tkb
  }
}