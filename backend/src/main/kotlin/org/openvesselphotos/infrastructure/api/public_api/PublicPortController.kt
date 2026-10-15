package org.openvesselphotos.infrastructure.api.public_api

import org.openvesselphotos.domain.use_cases.public_api.GetPortVessels
import org.openvesselphotos.domain.use_cases.public_api.GetPorts
import org.openvesselphotos.infrastructure.api.outputs.PortDataOutput
import org.openvesselphotos.infrastructure.api.outputs.VesselSummaryDataOutput
import org.springframework.http.CacheControl
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.time.Duration

@RestController
@RequestMapping("/v1/ports")
class PublicPortController(
    private val getPorts: GetPorts,
    private val getPortVessels: GetPortVessels,
) {
    /** Cached a few minutes: the map shows it on each visit. */
    @GetMapping
    fun ports(): ResponseEntity<List<PortDataOutput>> =
        ResponseEntity
            .ok()
            .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
            .body(getPorts.execute().map { PortDataOutput.from(it) })

    @GetMapping("/{code:[A-Za-z0-9]{5}}")
    fun port(
        @PathVariable code: String,
    ): PortDataOutput = PortDataOutput.from(getPorts.execute(code))

    @GetMapping("/{code:[A-Za-z0-9]{5}}/vessels")
    fun vessels(
        @PathVariable code: String,
        @RequestParam photographed: Boolean,
        @RequestParam(defaultValue = "0") page: Int,
    ): List<VesselSummaryDataOutput> {
        val baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString()

        return getPortVessels.execute(code, photographed, page).map { VesselSummaryDataOutput.from(baseUrl, it) }
    }
}
