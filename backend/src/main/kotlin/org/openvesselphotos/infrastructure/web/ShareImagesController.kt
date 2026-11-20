package org.openvesselphotos.infrastructure.web

import org.openvesselphotos.domain.use_cases.public_api.GetPorts
import org.openvesselphotos.domain.use_cases.public_api.GetSpecies
import org.openvesselphotos.infrastructure.web.PortsPagesController.Companion.GEAR_FAMILIES
import org.openvesselphotos.infrastructure.web.PortsPagesController.Companion.millions
import org.openvesselphotos.infrastructure.web.PortsPagesController.Companion.number
import org.openvesselphotos.infrastructure.web.PortsPagesController.Companion.price
import org.openvesselphotos.infrastructure.web.SiteHostFilter.Companion.PORTS_PREFIX
import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.time.Duration

/**
 * The images shown when a page of the ports site is shared (og:image), see [ShareImageRenderer]: the name of the page
 * and its key figures, with their sources, rather than a random photo.
 */
@RestController
class ShareImagesController(
    private val shareImageRenderer: ShareImageRenderer,
    private val publicUrl: PublicUrl,
    private val getPorts: GetPorts,
    private val getSpecies: GetSpecies,
) {
    @GetMapping("$PORTS_PREFIX${PortsSeo.SHARE_IMAGES_PATH}/accueil.png")
    fun home(): ResponseEntity<ByteArray> {
        val ports = getPorts.execute()
        val auctions = ports.mapNotNull { it.auction }
        val year = auctions.maxOfOrNull { it.year }

        return png(
            ShareCard(
                eyebrow = "La pêche professionnelle en France",
                title = "La flotte, les ports et les criées",
                figures =
                    listOfNotNull(
                        number(ports.sumOf { it.vesselCount }) to "navires de pêche",
                        number(ports.size) to "ports d’attache",
                        year?.let {
                            millions(
                                auctions.sumOf { a ->
                                    a.valueThousandEuros
                                },
                            ) to "vendus en criée en $it"
                        },
                    ),
                source = "Sources : registre de la flotte de l’UE, FranceAgriMer" + (year?.let { " $it" } ?: ""),
            ),
        )
    }

    @GetMapping("$PORTS_PREFIX${PortsSeo.SHARE_IMAGES_PATH}/ports/{slug}.png")
    fun port(
        @PathVariable slug: String,
    ): ResponseEntity<ByteArray> {
        val port = getPorts.execute().firstOrNull { it.slug == slug } ?: return ResponseEntity.notFound().build()
        val auction = port.auction
        val mainGear =
            GEAR_FAMILIES
                .map { (label, codes) ->
                    label to
                        port.vesselCountByGear
                            .filterKeys { it in codes }
                            .values
                            .sum()
                }.filter { it.second > 0 }
                .maxByOrNull { it.second }

        return png(
            ShareCard(
                eyebrow = "Port de pêche · ${port.port.locode}",
                title = port.port.name,
                figures =
                    listOfNotNull(
                        number(port.vesselCount) to "navires de pêche",
                        auction?.let { number(it.quantityTonnes) + " t" to "vendues en criée en ${it.year}" },
                        auction?.let { millions(it.valueThousandEuros) to "de ventes en ${it.year}" }
                            ?: mainGear?.let { (label, count) -> number(count) to label.lowercase() },
                    ),
                source =
                    "Sources : registre de la flotte de l’UE, positions des navires" +
                        (auction?.let { ", FranceAgriMer ${it.year}" } ?: ""),
            ),
        )
    }

    @GetMapping("$PORTS_PREFIX${PortsSeo.SHARE_IMAGES_PATH}/especes/{slug}.png")
    fun species(
        @PathVariable slug: String,
    ): ResponseEntity<ByteArray> {
        val species = getSpecies.execute().firstOrNull { it.slug == slug } ?: return ResponseEntity.notFound().build()

        return png(
            ShareCard(
                eyebrow = "Ventes en criée ${species.year}",
                title = species.name,
                figures =
                    listOfNotNull(
                        species.priceEurosPerKg?.let { price(it) to "prix moyen en criée" },
                        number(species.quantityTonnes) + " t" to "vendues dans les criées",
                        species.auctions.firstOrNull()?.let { it.auction to "première criée" },
                    ),
                source = "Source : ${PortsSeo.FRANCE_AGRIMER}, ${species.year}",
            ),
        )
    }

    private fun png(card: ShareCard): ResponseEntity<ByteArray> {
        val host = PublicUrl.hostOf(publicUrl.get(Site.PORTS)) ?: Site.PORTS.siteName

        return ResponseEntity
            .ok()
            .contentType(MediaType.IMAGE_PNG)
            .cacheControl(CacheControl.maxAge(Duration.ofHours(6)).cachePublic())
            .body(shareImageRenderer.render(card, host))
    }
}
