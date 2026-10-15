package org.openvesselphotos.domain.entities

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PortUTests {
    @Test
    fun `prettyName Should write the small words of French names in lower case`() {
        assertThat(Port.prettyName("Fort-De-France")).isEqualTo("Fort-de-France")
        assertThat(Port.prettyName("Les Sables-D'Olonne")).isEqualTo("Les Sables-d'Olonne")
        assertThat(Port.prettyName("Noirmoutier-En-L'Île")).isEqualTo("Noirmoutier-en-l'Île")
        assertThat(Port.prettyName("Boulogne-Sur-Mer")).isEqualTo("Boulogne-sur-Mer")
        assertThat(Port.prettyName("La Cotinière")).isEqualTo("La Cotinière")
        assertThat(Port.prettyName("Le Havre")).isEqualTo("Le Havre")
        assertThat(Port.prettyName("Pointe-A-Pitre")).isEqualTo("Pointe-à-Pitre")
    }

    @Test
    fun `isCode Should accept UN LOCODEs only`() {
        assertThat(Port.isCode("FRBES")).isTrue()
        assertThat(Port.isCode("FRM2R")).isTrue()
        assertThat(Port.isCode("frbes")).isFalse()
        assertThat(Port.isCode("FRBES1")).isFalse()
    }
}
