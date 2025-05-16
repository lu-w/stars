package tools.aqua.stars.logic.mtcq

import openllet.core.KnowledgeBaseImpl
import openllet.mtcq.model.kb.InMemoryTemporalKnowledgeBaseImpl
import openllet.mtcq.model.kb.TemporalKnowledgeBase
import tools.aqua.stars.core.types.EntityType
import tools.aqua.stars.core.types.SegmentType
import tools.aqua.stars.core.types.TickDataType
import tools.aqua.stars.core.types.TickDifference
import tools.aqua.stars.core.types.TickUnit

class DLConverter<E : EntityType<E, T, S, U, D>,
        T : TickDataType<E, T, S, U, D>,
        S : SegmentType<E, T, S, U, D>,
        U : TickUnit<U, D>,
        D : TickDifference<D>> {

    private val tkbCache = mutableMapOf<S, TemporalKnowledgeBase>()

    fun toTKB(segment: S): TemporalKnowledgeBase {
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