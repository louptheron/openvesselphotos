package org.openvesselphotos.domain.repositories

import org.openvesselphotos.domain.entities.PortLocation

interface PortLocationRepository {
    fun findByCode(code: String): PortLocation?
}
