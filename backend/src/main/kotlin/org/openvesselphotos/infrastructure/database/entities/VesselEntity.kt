package org.openvesselphotos.infrastructure.database.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.openvesselphotos.domain.entities.Vessel
import java.time.ZonedDateTime

@Entity
@Table(name = "vessels")
class VesselEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,
    val cfr: String?,
    val mmsi: String?,
    val name: String?,
    val registration: String?,
    val port: String?,
    @Column(name = "port_code")
    val portCode: String?,
    @Column(name = "created_at", insertable = false, updatable = false)
    val createdAt: ZonedDateTime? = null,
) {
    fun toVessel() =
        Vessel(
            id = id,
            cfr = cfr,
            mmsi = mmsi,
            name = name,
            registration = registration,
            port = port,
            portCode = portCode,
            createdAt = createdAt,
        )

    companion object {
        fun fromVessel(vessel: Vessel) =
            VesselEntity(
                id = vessel.id,
                cfr = vessel.cfr,
                mmsi = vessel.mmsi,
                name = vessel.name,
                registration = vessel.registration,
                port = vessel.port,
                portCode = vessel.portCode,
                createdAt = vessel.createdAt,
            )
    }
}
