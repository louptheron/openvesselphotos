package org.openvesselphotos.domain.repositories

import org.openvesselphotos.domain.entities.StockAssessment

interface StockAssessmentRepository {
    /** Ifremer's last assessment of the stocks landed by French vessels */
    fun find(): StockAssessment
}
