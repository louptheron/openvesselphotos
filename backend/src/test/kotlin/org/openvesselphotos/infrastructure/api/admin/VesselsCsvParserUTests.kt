package org.openvesselphotos.infrastructure.api.admin

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class VesselsCsvParserUTests {
    @Test
    fun `parse Should read an export of the current EU fleet register`() {
        val csv =
            """
            Country of Registration;Vessel Type;CFR;UVI;Event Type;Event Start Date;Event End Date;IRCS;Registration Number;External Marking;MMSI;Name of Vessel;Country of Importation/Exportation;Place of Registration Code;Place of Registration Name;Year of Construction;Power of Main Engine (kW);Power of Auxiliary Engine (kW);LOA (m);LBP (m);Tonnage GT;Other Tonnage;Tonnage GTs;Main Fishing Gear;Subsidiary Fishing Gear 1;Subsidiary Fishing Gear 2;Subsidiary Fishing Gear 3;Subsidiary Fishing Gear 4;Subsidiary Fishing Gear 5;IRCS Indicator;VMS Indicator;ERS Indicator;ERS Exemption Indicator;AIS Indicator;Licence Indicator;Segment;Type of Export;Public Aid;Date of Entry Into Service;Hull Material
            FRA;FX;FRA000284793;;MOD;13/10/2026;31/12/2100;FO9789;284793;BR284793;227590780;L' AUTHENTIK II;;FRBES;Brest;1975;73.00;0.00;9.70;0.00;5.77;5.70;;DRB;LVT;GTN;FPO;;;Y;Y;N;Y;Y;Y;MFL;;;01/10/1975;1
            """.trimIndent()

        val rows = VesselsCsvParser.parse(csv)

        assertThat(rows).hasSize(1)
        assertThat(rows.first().cfr).isEqualTo("FRA000284793")
        assertThat(rows.first().mmsi).isEqualTo("227590780")
        assertThat(rows.first().name).isEqualTo("L' AUTHENTIK II")
        assertThat(rows.first().registration).isEqualTo("BR284793")
        assertThat(rows.first().port).isEqualTo("Brest")
        assertThat(rows.first().portCode).isEqualTo("FRBES")
        assertThat(rows.first().event).isEqualTo("MOD")
        assertThat(rows.first().vesselType).isEqualTo("FX")
        assertThat(rows.first().yearBuilt).isEqualTo("1975")
        assertThat(rows.first().length).isEqualTo("9.70")
        assertThat(rows.first().enginePower).isEqualTo("73.00")
        assertThat(rows.first().tonnage).isEqualTo("5.77")
        assertThat(rows.first().mainGear).isEqualTo("DRB")
        assertThat(rows.first().otherGears).containsExactly("LVT", "GTN", "FPO")
        assertThat(rows.first().callSign).isEqualTo("FO9789")
    }
}
