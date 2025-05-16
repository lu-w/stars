package tools.aqua.stars.logic.mtcq

import openllet.core.KnowledgeBase

interface DLConvertible {
    fun addToKB(kb: KnowledgeBase)
}