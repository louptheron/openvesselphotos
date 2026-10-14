package org.openvesselphotos

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
// Saves the usage statistics every minute
@EnableScheduling
class OpenVesselPhotosApplication

fun main(args: Array<String>) {
    runApplication<OpenVesselPhotosApplication>(*args)
}
