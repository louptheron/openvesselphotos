package org.openvesselphotos.infrastructure.ports

import org.openvesselphotos.domain.entities.PortLocation
import org.openvesselphotos.domain.repositories.PortLocationRepository
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Repository

/**
 * The coordinates of the places of the EU and its outermost regions, from UN/LOCODE (ports.csv, built by
 * scripts/ports.py). About 47,000 places, kept in arrays rather than objects to spare memory (about 1.5 MB), and found
 * by dichotomy: the file is sorted by code.
 */
@Repository
class CsvPortLocationRepository : PortLocationRepository {
    private val codes: Array<String>
    private val latitudes: DoubleArray
    private val longitudes: DoubleArray

    init {
        val lines =
            ClassPathResource("ports.csv")
                .inputStream
                .bufferedReader()
                .useLines { it.filter { line -> line.isNotBlank() }.toList() }
        codes = Array(lines.size) { lines[it].substring(0, CODE_LENGTH) }
        latitudes = DoubleArray(lines.size) { lines[it].split(",")[1].toDouble() }
        longitudes = DoubleArray(lines.size) { lines[it].split(",")[2].toDouble() }
        check(codes.indices.drop(1).all { codes[it - 1] < codes[it] }) { "ports.csv must be sorted by code" }
    }

    override fun findByCode(code: String): PortLocation? {
        val index = codes.binarySearch(code)

        return if (index >= 0) PortLocation(latitudes[index], longitudes[index]) else null
    }

    companion object {
        private const val CODE_LENGTH = 5
    }
}
