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

class MTCQEvaluator<
    E : EntityType<E, T, S, U, D>,
    T : TickDataType<E, T, S, U, D>,
    S : SegmentType<E, T, S, U, D>,
    U : TickUnit<U, D>,
    D : TickDifference<D>> {

    private val tkbCache = mutableMapOf<S, TemporalKnowledgeBase>()

    fun eval(segment: S, mtcqString: String): QueryResult {
        val tkb = toTKB(segment)
        val mtcq = MetricTemporalConjunctiveQueryParser.parse(mtcqString, tkb)
        println("MTCQ eval called for TKB of size " + tkb.size)
        val eng = MTCQNormalFormEngine()
        return eng.exec(mtcq)
    }

    private fun toTKB(segment: S): TemporalKnowledgeBase {
        tkbCache[segment]?.let {
            return it
        }

        val tkb = InMemoryTemporalKnowledgeBaseImpl()
        segment.tickData.forEach {
            println("Initializing TKB for tick " + it.currentTick)
            val kb = KnowledgeBaseImpl()
            // TODO dont do conversion when checking if formula holds - we might check N formulae
            it.entities.forEach {
                if (it is DLConvertible)
                    it.addToKB(kb)
            }
            // TODO
            // this misses some other members of it (weather, daytime, trafficLights)
            tkb.add(kb)
        }

        tkbCache[segment] = tkb
        return tkb
    }
}