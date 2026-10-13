package org.openvesselphotos.config

import org.springframework.stereotype.Component

/** Marks a domain use case, as in MonitorFish: one class, one `execute` method. */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Component
annotation class UseCase
