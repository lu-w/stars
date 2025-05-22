package tools.aqua.stars.logic.mtcq

import DLConvertible
import openllet.core.KnowledgeBase
import openllet.core.KnowledgeBaseImpl
import openllet.mtcq.engine.MTCQNormalFormEngine
import openllet.mtcq.model.kb.InMemoryTemporalKnowledgeBaseImpl
import openllet.mtcq.model.kb.TemporalKnowledgeBase
import openllet.mtcq.parser.MetricTemporalConjunctiveQueryParser
import openllet.owlapi.OpenlletReasonerFactory
import openllet.query.sparqldl.model.results.QueryResult
import tools.aqua.stars.core.types.*
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties
import org.semanticweb.owlapi.apibinding.OWLManager
import kotlin.jvm.optionals.getOrNull


class MTCQEvaluator<
    E : EntityType<E, T, S, U, D>,
    T : TickDataType<E, T, S, U, D>,
    S : SegmentType<E, T, S, U, D>,
    U : TickUnit<U, D>,
    D : TickDifference<D>> {

  // Global ontology (TBox) loaded from file (optional)
  var ontology: KnowledgeBase? = null
  var prefix: String = ""

  // Cache, only stores the last TKB to save memory
  private val lastTkbCache = mutableMapOf<S, TemporalKnowledgeBase>()

  fun setOntology(ontologyFile: File) {
    require(ontologyFile.exists()) { "Ontology file does not exist: $ontologyFile" }
    val man = OWLManager.createOWLOntologyManager()
    val ont = man.loadOntologyFromOntologyDocument(ontologyFile)
    val reasoner = OpenlletReasonerFactory.getInstance().createReasoner(ont)
    ontology = reasoner.kb
    prefix = if (ont.ontologyID.ontologyIRI != null) ont.ontologyID.ontologyIRI.getOrNull()!!.iriString else ""
  }

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
    // Check cache
    lastTkbCache[segment]?.let {
      return it
    }
    // New segment - clear cache
    lastTkbCache.clear()
    // Add any DL-convertible member of any tick in segment to a temporal knowledge base
    val tkb = InMemoryTemporalKnowledgeBaseImpl()
    segment.tickData.forEach { tick ->
      println("Initializing TKB for tick " + tick.currentTick)
      val kb = when (ontology) {
        null -> KnowledgeBaseImpl()
        else -> ontology!!.copy()
      }
      for (prop in (tick::class as KClass<T>).memberProperties) {
        val data = prop.get(tick)
        if (data is Iterable<*>)
          data.forEach { entity ->
            if (entity != null && entity::class.hasAnnotation<DLConvertible>())
              entity.addToKB(kb, prefix)
          }
        if (data != null && data::class.hasAnnotation<DLConvertible>())
          data.addToKB(kb, prefix)
      }
      tkb.add(kb)
    }
    // Update cache
    lastTkbCache[segment] = tkb
    return tkb
  }
}