package org.openvesselphotos.infrastructure.api.public_api

import org.openvesselphotos.domain.entities.HomePortSearchResult
import org.openvesselphotos.domain.use_cases.public_api.GetPort
import org.openvesselphotos.domain.use_cases.public_api.GetPorts
import org.openvesselphotos.domain.use_cases.public_api.SearchPortVessels
import org.openvesselphotos.infrastructure.api.outputs.PortDataOutput
import org.openvesselphotos.infrastructure.api.outputs.PortDetailsDataOutput
import org.openvesselphotos.infrastructure.web.PublicUrl
import org.springframework.http.CacheControl
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
    private val publicUrl: PublicUrl,
) {
    /** The ports with vessels based at them, by name */
    @GetMapping
    fun ports(): ResponseEntity<List<PortDataOutput>> = cached(getPorts.execute().map(PortDataOutput::from))

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
