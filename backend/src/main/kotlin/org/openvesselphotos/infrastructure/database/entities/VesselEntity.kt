package org.openvesselphotos.infrastructure.database.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.openvesselphotos.domain.entities.RegisterData
import org.openvesselphotos.domain.entities.Vessel
import java.math.BigDecimal
import java.time.LocalDate
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
    @Column(name = "vessel_type")
    val vesselType: String? = null,
    @Column(name = "year_built")
    val yearBuilt: Int? = null,
    @Column(name = "length_m")
    val lengthMeters: BigDecimal? = null,
    @Column(name = "engine_power_kw")
    val enginePowerKw: BigDecimal? = null,
    @Column(name = "tonnage_gt")
    val tonnageGt: BigDecimal? = null,
    @Column(name = "main_gear")
    val mainGear: String? = null,
    /** Comma-separated */
    @Column(name = "other_gears")
    val otherGears: String? = null,
    @Column(name = "call_sign")
    val callSign: String? = null,
    @Column(name = "register_imported_on")
    val registerImportedOn: LocalDate? = null,
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
            register =
                RegisterData(
                    vesselType = vesselType,
                    yearBuilt = yearBuilt,
                    lengthMeters = lengthMeters?.toDouble(),
                    enginePowerKw = enginePowerKw?.toDouble(),
                    tonnageGt = tonnageGt?.toDouble(),
                    mainGear = mainGear,
                    otherGears = otherGears?.split(",")?.filter { it.isNotBlank() }.orEmpty(),
                    callSign = callSign,
                    importedOn = registerImportedOn,
                ),
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
                vesselType = vessel.register.vesselType,
                yearBuilt = vessel.register.yearBuilt,
                lengthMeters = vessel.register.lengthMeters?.toBigDecimal(),
                enginePowerKw = vessel.register.enginePowerKw?.toBigDecimal(),
                tonnageGt = vessel.register.tonnageGt?.toBigDecimal(),
                mainGear = vessel.register.mainGear,
                otherGears =
                    vessel.register.otherGears
                        .joinToString(",")
                        .ifEmpty { null },
                callSign = vessel.register.callSign,
                registerImportedOn = vessel.register.importedOn,
                createdAt = vessel.createdAt,
            )
    }
}
