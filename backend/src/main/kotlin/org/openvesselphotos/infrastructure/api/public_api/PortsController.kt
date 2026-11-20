package org.openvesselphotos.infrastructure.api.public_api

import org.openvesselphotos.domain.entities.HomePortSearchResult
import org.openvesselphotos.domain.use_cases.public_api.FishingEffortPeriod
import org.openvesselphotos.domain.use_cases.public_api.GetFishingEffort
import org.openvesselphotos.domain.use_cases.public_api.GetPort
import org.openvesselphotos.domain.use_cases.public_api.GetPorts
import org.openvesselphotos.domain.use_cases.public_api.SearchPortVessels
import org.openvesselphotos.infrastructure.api.outputs.PortDataOutput
import org.openvesselphotos.infrastructure.api.outputs.PortDetailsDataOutput
import org.openvesselphotos.infrastructure.web.PublicUrl
import org.springframework.http.CacheControl
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Duration

/** The French fishing vessels by home port, for the ports site. Changes with each import only: cached for 5 minutes. */
@RestController
@RequestMapping("/v1/ports")
class PortsController(
    private val getPorts: GetPorts,
    private val getPort: GetPort,
    private val searchPortVessels: SearchPortVessels,
    private val getFishingEffort: GetFishingEffort,
    private val publicUrl: PublicUrl,
) {
    /** The ports with vessels based at them, by name */
    @GetMapping
    fun ports(): ResponseEntity<List<PortDataOutput>> = cached(getPorts.execute().map(PortDataOutput::from))

    /** The period of the fishing activity map, and whether it is enabled (Global Fishing Watch token) */
    @GetMapping("/fishing-effort")
    fun fishingEffortPeriod(): FishingEffortPeriod = getFishingEffort.period()

    /**
     * A tile of the fishing effort of the French vessels over the last 12 months, for a gear family of the ports site
     * when given (e.g. engin=chalut-de-fond). From Global Fishing Watch, through this app which holds the token.
     */
    @GetMapping("/fishing-effort/{z}/{x}/{y}.png", produces = [MediaType.IMAGE_PNG_VALUE])
    fun fishingEffortTile(
        @PathVariable z: Int,
        @PathVariable x: Int,
        @PathVariable y: Int,
        @RequestParam(required = false) engin: String?,
    ): ResponseEntity<ByteArray> {
        val tile =
            getFishingEffort.tile(z, x, y, engin?.takeIf { it.isNotBlank() })
                ?: return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build()

        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic())
            .contentType(MediaType.IMAGE_PNG)
            .body(tile)
    }

    /**
     * Where the vessels of the port fished over the last 12 months, by cell of 0.05° ([longitude, latitude, hours]), for
     * a trawl gear family when given (engin=chalut-de-fond); from Global Fishing Watch, synchronised in /admin.
     */
    @GetMapping("/{locode:[A-Za-z]{2}[A-Za-z0-9]{3}(?:-[0-9]{1,4})?}/fishing-effort")
    fun portFishingEffort(
        @PathVariable locode: String,
        @RequestParam(required = false) engin: String?,
    ): ResponseEntity<PortFishingEffortDataOutput> {
        val effort = getFishingEffort.port(locode, engin?.takeIf { it.isNotBlank() })

        return cached(
            PortFishingEffortDataOutput(
                effort.trackedVessels,
                effort.cells.map { listOf(it.longitude, it.latitude, it.hours) },
            ),
        )
    }

    /** Vessels by name or CFR (at least 2 characters), with the port they are based at */
    @GetMapping("/vessels")
    fun searchVessels(
        @RequestParam q: String,
    ): List<HomePortSearchResult> = searchPortVessels.execute(q)

    /** A port and its vessels, with links to their pages and photos on Open Vessel Photos */
    @GetMapping("/{locode:[A-Za-z]{2}[A-Za-z0-9]{3}(?:-[0-9]{1,4})?}")
    fun port(
        @PathVariable locode: String,
    ): ResponseEntity<PortDetailsDataOutput> =
        cached(PortDetailsDataOutput.from(publicUrl.get(), getPort.execute(locode)))

    private fun <T : Any> cached(body: T): ResponseEntity<T> =
        ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic()).body(body)
}

data class PortFishingEffortDataOutput(
    /** Vessels of the port found on Global Fishing Watch with fishing */
    val trackedVessels: Int,
    /** [longitude, latitude, hours of fishing] of each cell */
    val cells: List<List<Double>>,
)
