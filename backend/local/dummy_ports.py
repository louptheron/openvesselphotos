#!/usr/bin/env python3
"""
LOCAL DEVELOPMENT ONLY: fills the local database with dummy data for the ports site, so that the map, the port pages,
the fish auctions and the vessel photos show without importing anything. Run with `make load-dummy-ports`.

Writes SQL to stdout:
- ports of the French coasts, with their positions;
- vessels based at each of them (vessel_home_ports), from all the home port sources;
- their EU fleet register data (dummy-fleet-register.sql), and a hull marking;
- the sales of the fish auctions of some ports, over two years, and the seasons of the species;
- the zones where some vessels fished (vessel_fishing_cells), for the fishing effort layer;
- drawn photos of some vessels, in the three sizes (needs Pillow: `pip install pillow`).

Every value is picked from a seeded random generator, so that a run gives the same data as the last one. Dummy vessels
have a CFR starting with FRA9 and dummy photos a source URL on example.invalid: a run first deletes them, so it can be
run again. Species are named as FranceAgriMer names them, so that their stock status and quota come from the
reference CSVs (ifremer/stock-status.csv, eu/tac-quotas.csv).
"""

import io
import math
import random
import sys
from datetime import date
from pathlib import Path

random.seed(20261127)

TODAY = date.today().isoformat()
HERE = Path(__file__).parent

# locode, name, latitude, longitude, vessels, quartier maritime (hull marking), direction of the open sea (lat, lon)
PORTS = [
    ("FRLRT", "Lorient", 47.7290, -3.3640, 84, "LO", (-1.0, -0.6)),
    ("FRPLL", "Port-Louis", 47.7080, -3.3540, 9, "LO", (-1.0, -0.4)),
    ("FRCOC", "Concarneau", 47.8720, -3.9190, 62, "CC", (-1.0, -0.3)),
    ("FRGVC", "Le Guilvinec", 47.7940, -4.2850, 71, "GV", (-1.0, -0.8)),
    ("FRSGU", "Saint-Guénolé", 47.8150, -4.3760, 24, "GV", (-0.6, -1.0)),
    ("FRDRZ", "Douarnenez", 48.0980, -4.3290, 18, "DZ", (0.0, -1.0)),
    ("FRQUI", "Quiberon", 47.4830, -3.1190, 14, "AY", (-1.0, -0.5)),
    ("FRTSM", "La Trinité-sur-Mer", 47.5850, -3.0130, 11, "AY", (-1.0, 0.0)),
    ("FRERQ", "Erquy", 48.6330, -2.4660, 38, "SB", (1.0, 0.0)),
    ("FRBOL", "Boulogne-sur-Mer", 50.7260, 1.5970, 52, "BL", (0.2, -1.0)),
    ("FRLSO", "Les Sables-d'Olonne", 46.4960, -1.7920, 46, "LS", (-0.3, -1.0)),
    ("FRSET", "Sète", 43.4000, 3.6990, 31, "ST", (-1.0, 0.2)),
]

# The auctions, by port: (species as FranceAgriMer names them, tonnes, euros per kg) for the last year
AUCTIONS = {
    "FRLRT": [
        ("Merlu", 1240, 4.03),
        ("Lingue bleue", 1111, 3.08),
        ("Lieu noir", 940, 2.16),
        ("Baudroies", 894, 6.19),
        ("Langoustine", 328, 16.42),
        ("Lingue franche", 310, 3.68),
        ("Cardine franche", 247, 3.92),
        ("Pieuvre, poulpe", 241, 6.69),
        ("Sole", 151, 20.41),
        ("Seiche", 138, 5.12),
        ("Calmars", 122, 9.80),
        ("Maquereau", 96, 1.45),
    ],
    "FRCOC": [
        ("Thon albacore", 2600, 2.10),
        ("Merlu", 820, 3.95),
        ("Baudroies", 610, 6.40),
        ("Langoustine", 190, 17.10),
        ("Sole", 95, 21.30),
        ("Seiche", 84, 5.40),
    ],
    "FRGVC": [
        ("Langoustine", 610, 16.80),
        ("Baudroies", 1320, 6.05),
        ("Merlu", 1180, 3.88),
        ("Cardine franche", 420, 3.70),
        ("Sole", 210, 19.90),
        ("Bar", 120, 14.20),
        ("Calmars", 160, 10.10),
    ],
    "FRERQ": [
        ("Coquille Saint-Jacques", 3200, 3.40),
        ("Seiche", 140, 4.90),
        ("Bar", 60, 15.10),
        ("Araignée de mer", 210, 3.10),
    ],
    "FRBOL": [
        ("Merlan", 3100, 1.90),
        ("Calmars", 1400, 8.70),
        ("Coquille Saint-Jacques", 2400, 3.20),
        ("Encornet", 300, 7.40),
        ("Rouget-Barbet", 420, 6.30),
        ("Sole", 180, 18.70),
        ("Plie", 160, 2.10),
    ],
    "FRLSO": [
        ("Sole", 640, 19.40),
        ("Merlu", 520, 4.20),
        ("Seiche", 600, 5.00),
        ("Bar", 230, 13.90),
        ("Maigre", 150, 9.10),
        ("Anchois", 300, 1.70),
    ],
    "FRSET": [
        ("Thon rouge", 900, 11.20),
        ("Merlu", 380, 6.10),
        ("Anchois", 640, 2.20),
        ("Sardine", 410, 1.30),
        ("Pieuvre, poulpe", 120, 7.40),
    ],
}

# The month when each species sells most (0 = January), and how sharp its season is
SEASONS = {
    "Anchois": (5, 2.5),
    "Araignée de mer": (4, 2.0),
    "Bar": (11, 1.0),
    "Baudroies": (3, 0.6),
    "Calmars": (10, 2.0),
    "Cardine franche": (4, 0.6),
    "Coquille Saint-Jacques": (0, 3.0),
    "Encornet": (9, 2.0),
    "Langoustine": (5, 1.5),
    "Lieu noir": (2, 1.0),
    "Lingue bleue": (3, 0.8),
    "Lingue franche": (3, 0.8),
    "Maigre": (5, 1.5),
    "Maquereau": (2, 2.0),
    "Merlan": (11, 1.0),
    "Merlu": (6, 0.5),
    "Pieuvre, poulpe": (9, 2.5),
    "Plie": (4, 1.0),
    "Rouget-Barbet": (8, 1.5),
    "Sardine": (7, 2.0),
    "Seiche": (2, 2.5),
    "Sole": (1, 1.2),
    "Thon albacore": (7, 1.5),
    "Thon rouge": (6, 3.0),
}

FIRST_NAMES = ["Marie", "Anne", "Yann", "Gwen", "Loïc", "Erwan", "Soizic", "Maëlle", "Corentin", "Nolwenn", "Tristan",
               "Jean-Pierre", "Sterenn", "Morgane", "Ronan", "Klervi"]
NAMES = ["Ar Vag", "Belle-Île", "Kerhervy", "Men Du", "Glenan", "Penmarc'h", "Étoile du Matin", "Aurore", "Albatros",
         "Goéland", "Cormoran", "Pen Hir", "Avel Vor", "Mor Braz", "Tréhic", "Le Saint-Pierre", "Vent d'Ouest",
         "Atlantique", "Iroise", "Bugaled", "Kenavo", "Petit Mousse", "Fleur de Lampaul", "Notre-Dame",
         "Sant Gwenole", "Ker Ys", "Gast", "Ar Raz", "Bara Kozh", "Talion", "Mélodie", "Victoire", "Gavrais"]

LICENCES = ["CC_BY_4_0", "CC_BY_SA_4_0", "CC0"]
CREDITS = ["Photo factice", "Dummy Photographer", "Studio Local", "Archives fictives"]


def sql(value) -> str:
    """A SQL literal"""
    if value is None:
        return "NULL"
    if isinstance(value, (int, float)):
        return repr(value)

    return "'" + str(value).replace("'", "''") + "'"


def vessel_name() -> str:
    name = random.choice(NAMES)
    if random.random() < 0.35:
        name = f"{random.choice(FIRST_NAMES)} {random.choice(['', 'II', 'III'])}".strip()
    if random.random() < 0.15:
        name += f" {random.randint(2, 9)}"

    return name.upper()


def main() -> None:
    out = ["-- Generated by backend/local/dummy_ports.py: LOCAL DEVELOPMENT ONLY", "BEGIN;", ""]

    # Cleanup of the last run
    out += [
        "DELETE FROM photos WHERE source_url LIKE 'https://example.invalid/dummy/%';",
        "DELETE FROM vessel_fishing_cells WHERE cfr LIKE 'FRA9%';",
        "DELETE FROM vessel_home_ports WHERE cfr LIKE 'FRA9%';",
        "DELETE FROM home_port_checks WHERE cfr LIKE 'FRA9%';",
        "DELETE FROM vessels WHERE cfr LIKE 'FRA9%';",
        f"DELETE FROM auction_sales WHERE port_locode IN ({', '.join(sql(locode) for locode in AUCTIONS)});",
        "",
    ]

    # Ports
    for locode, name, latitude, longitude, *_ in PORTS:
        out.append(
            f"INSERT INTO ports (locode, name, country, latitude, longitude) VALUES ({sql(locode)}, {sql(name)}, 'FR', "
            f"{latitude}, {longitude}) ON CONFLICT (locode) DO UPDATE SET name = excluded.name, "
            "latitude = excluded.latitude, longitude = excluded.longitude;"
        )
    out.append("")

    # Vessels based at each port, from all the sources of the home ports
    vessels = []
    number = 0
    for locode, _name, latitude, longitude, count, quartier, sea in PORTS:
        for _ in range(count):
            number += 1
            cfr = f"FRA9{number:08d}"
            source = random.choices(["IMPORT", "GFW", "REGISTER", "RETURNS"], weights=[70, 12, 10, 8])[0]
            total = random.randint(200, 4000) if source == "IMPORT" else 0
            at_port = int(total * random.uniform(0.45, 0.95))
            distance = round(random.uniform(0.2, 6), 2) if source == "IMPORT" else None
            name = vessel_name()
            vessels.append((cfr, locode, latitude, longitude, quartier, sea))
            out.append(
                "INSERT INTO vessel_home_ports (cfr, vessel_name, port_locode, observations_total, "
                f"observations_at_port, mean_distance_km, imported_on, source) VALUES ({sql(cfr)}, {sql(name)}, "
                f"{sql(locode)}, {total}, {at_port}, {sql(distance)}, {sql(TODAY)}, {sql(source)});"
            )
    out.append("")

    # Their EU fleet register data, then a hull marking, e.g. "LO 912345"
    out.append((HERE / "dummy-fleet-register.sql").read_text())
    for cfr, _locode, _lat, _lon, quartier, _sea in vessels:
        out.append(
            f"UPDATE vessels SET registration = {sql(f'{quartier} {random.randint(100000, 999999)}')} "
            f"WHERE cfr = {sql(cfr)};"
        )
    # Some industrial vessels (24 m and more), which the register's profiles above seldom give
    out.append(
        "UPDATE vessels SET length_m = 24 + (abs(hashtext(cfr)) % 160) / 10.0, vessel_type = 'TO', main_gear = 'OTB' "
        "WHERE cfr LIKE 'FRA9%' AND abs(hashtext(cfr)) % 9 = 0;"
    )
    out.append("")

    # Where a third of the vessels fished: cells of 0.05°, out to sea from their port
    for cfr, _locode, latitude, longitude, _quartier, (sea_lat, sea_lon) in vessels:
        if random.random() > 0.35:
            continue
        reach = random.uniform(0.2, 1.4)
        center_lat = latitude + sea_lat * reach + random.uniform(-0.15, 0.15)
        center_lon = longitude + sea_lon * reach + random.uniform(-0.15, 0.15)
        cells = {}
        for _ in range(random.randint(8, 40)):
            cell = (
                round(round((center_lat + random.gauss(0, 0.12)) / 0.05) * 0.05, 3),
                round(round((center_lon + random.gauss(0, 0.18)) / 0.05) * 0.05, 3),
            )
            cells[cell] = cells.get(cell, 0) + round(random.uniform(1, 30), 2)
        for (cell_lat, cell_lon), hours in cells.items():
            out.append(
                "INSERT INTO vessel_fishing_cells (cfr, latitude, longitude, hours) VALUES "
                f"({sql(cfr)}, {cell_lat}, {cell_lon}, {round(hours, 2)});"
            )
    out.append("")

    # The auctions: the last year and the one before, the total of all species and each species
    last_year = date.today().year - 1
    port_names = {locode: name for locode, name, *_ in PORTS}
    for locode, species_list in AUCTIONS.items():
        auction = port_names[locode]
        for year, factor in ((last_year - 1, random.uniform(0.85, 1.2)), (last_year, 1.0)):
            total_t = total_keur = 0.0
            for species, tonnes, price in species_list:
                quantity = round(tonnes * factor * random.uniform(0.9, 1.1), 3)
                value = round(quantity * price * random.uniform(0.95, 1.05), 3)
                total_t += quantity
                total_keur += value
                out.append(
                    "INSERT INTO auction_sales (auction, port_locode, year, species, quantity_t, value_keur) VALUES "
                    f"({sql(auction)}, {sql(locode)}, {year}, {sql(species)}, {quantity}, {value});"
                )
            # The species above are the main ones: the total has some more
            total_t *= 1.18
            total_keur *= 1.12
            out.append(
                "INSERT INTO auction_sales (auction, port_locode, year, species, quantity_t, value_keur) VALUES "
                f"({sql(auction)}, {sql(locode)}, {year}, NULL, {round(total_t, 3)}, {round(total_keur, 3)});"
            )
    out.append("")

    # The seasons, in all French auctions, for the last year
    all_species = sorted({species for species_list in AUCTIONS.values() for species, *_ in species_list})
    for species in all_species:
        peak, sharpness = SEASONS.get(species, (5, 1.0))
        base = sum(tonnes for species_list in AUCTIONS.values() for name, tonnes, _ in species_list if name == species)
        for month in range(12):
            distance = min(abs(month - peak), 12 - abs(month - peak))
            share = math.exp(-sharpness * (distance / 3) ** 2) * random.uniform(0.85, 1.15) + 0.03
            out.append(
                "INSERT INTO species_monthly_sales (species, year, month, quantity_t) VALUES "
                f"({sql(species)}, {last_year}, {month + 1}, {round(base * 4 * share / 12, 3)}) "
                "ON CONFLICT (species, year, month) DO UPDATE SET quantity_t = excluded.quantity_t;"
            )
    out.append("")

    out += photos(vessels)
    out += ["", "COMMIT;"]
    sys.stdout.write("\n".join(out) + "\n")


def photos(vessels) -> list[str]:
    """Drawn photos, for 6 vessels of Lorient and 2 of each other port, in the three sizes the site serves"""
    try:
        from PIL import Image, ImageDraw
    except ImportError:
        print("Pillow is not installed (pip install pillow): no dummy photos", file=sys.stderr)

        return ["-- No photos: Pillow is not installed"]

    out = []
    chosen = []
    for locode, *_ in PORTS:
        port_vessels = [vessel for vessel in vessels if vessel[1] == locode]
        chosen += port_vessels[: 6 if locode == "FRLRT" else 2]
    for index, (cfr, *_rest) in enumerate(chosen):
        hull = random.choice([(30, 80, 140), (180, 40, 35), (230, 230, 225), (25, 40, 60), (40, 110, 70)])
        sky = random.choice([(190, 215, 232), (220, 210, 195), (175, 195, 210)])
        files = []
        # Stored by the name of ImageSize
        for size, width in (("SMALL", 320), ("MEDIUM", 800), ("FULL", 1600)):
            height = width * 3 // 4
            image = Image.new("RGB", (width, height), sky)
            draw = ImageDraw.Draw(image)
            sea_top = int(height * 0.62)
            draw.rectangle((0, sea_top, width, height), fill=(70, 110, 140))
            # The hull, the wheelhouse and a mast
            unit = width / 32
            draw.polygon(
                [(unit * 6, sea_top - unit * 3), (unit * 27, sea_top - unit * 3.6), (unit * 24, sea_top + unit),
                 (unit * 8, sea_top + unit)],
                fill=hull,
            )
            draw.rectangle((unit * 15, sea_top - unit * 7, unit * 21, sea_top - unit * 3.2), fill=(240, 240, 235))
            draw.rectangle((unit * 16, sea_top - unit * 6.2, unit * 20, sea_top - unit * 5.2), fill=(40, 50, 60))
            draw.line((unit * 12, sea_top - unit * 3, unit * 12, sea_top - unit * 12), fill=(40, 40, 40), width=max(1, int(unit / 3)))
            buffer = io.BytesIO()
            image.save(buffer, "JPEG", quality=70)
            files.append((size, buffer.getvalue()))
        credit = random.choice(CREDITS)
        licence = random.choice(LICENCES)
        url = f"https://example.invalid/dummy/{index + 1}"
        out.append(
            "WITH photo AS (INSERT INTO photos (vessel_id, number, status, submitted_key_type, submitted_key, credit, "
            "licence, width, height, reviewed_at, source_url) "
            f"SELECT id, 1, 'APPROVED', 'CFR', {sql(cfr)}, {sql(credit)}, {sql(licence)}, 1600, 1200, now(), {sql(url)} "
            f"FROM vessels WHERE cfr = {sql(cfr)} RETURNING id) "
            "INSERT INTO photo_files (photo_id, size, content) "
            + " UNION ALL ".join(f"SELECT id, {sql(size)}, '\\x{content.hex()}'::bytea FROM photo" for size, content in files)
            + ";"
        )

    return out


if __name__ == "__main__":
    main()
