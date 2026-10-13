package org.openvesselphotos

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class OpenVesselPhotosApplication

fun main(args: Array<String>) {
    runApplication<OpenVesselPhotosApplication>(*args)
}
