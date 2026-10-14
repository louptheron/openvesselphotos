"""
Builds src/main/resources/ports.csv (code,latitude,longitude): the coordinates of the places of the EU and its outermost regions, from UN/LOCODE
(https://unece.org/trade/uncefact/unlocode), where the EU fleet register's "Place of Registration Code" comes from.

Usage: python3 scripts/ports.py code-list.csv
with code-list.csv from https://github.com/datasets/un-locode (data/code-list.csv)
"""

import csv
import re
import sys
from pathlib import Path

COUNTRIES = {
    # EU member states
    "AT", "BE", "BG", "CY", "CZ", "DE", "DK", "EE", "ES", "FI", "FR", "GR", "HR", "HU", "IE", "IT", "LT", "LU", "LV",
    "MT", "NL", "PL", "PT", "RO", "SE", "SI", "SK",
    # French overseas regions and territories, with their own country codes
    "GF", "GP", "MQ", "RE", "YT", "PM", "BL", "MF",
}

# Codes of the register missing from UN/LOCODE or without coordinates
EXTRA = [
    # Pseudo-code of the register for Réunion, placed at its main fishing port (Le Port)
    ("REREU", "La Réunion", -20.9356, 55.2925),
    ("FRLC5", "La Cotinière", 45.9133, -1.3306),
]


def to_degrees(value):
    """ "4824N 00429W" -> (48.4, -4.4833) """
    match = re.fullmatch(r"(\d{2})(\d{2})([NS]) (\d{3})(\d{2})([EW])", value.strip())
    if not match:
        return None
    lat = int(match[1]) + int(match[2]) / 60
    lon = int(match[4]) + int(match[5]) / 60

    return (-lat if match[3] == "S" else lat), (-lon if match[6] == "W" else lon)


def main(code_list):
    places = {}
    with open(code_list, encoding="utf-8") as file:
        for row in csv.DictReader(file):
            coordinates = to_degrees(row["Coordinates"])
            if row["Country"] in COUNTRIES and row["Location"] and coordinates and row["Change"] != "X":
                places[row["Country"] + row["Location"]] = coordinates
    for code, _name, lat, lon in EXTRA:
        places[code] = (lat, lon)

    output = Path(__file__).parent.parent / "src/main/resources/ports.csv"
    with open(output, "w", encoding="utf-8", newline="") as file:
        writer = csv.writer(file, lineterminator="\n")
        # Sorted by code, without header: loaded into arrays and searched by dichotomy (PortLocations)
        for code in sorted(places):
            lat, lon = places[code]
            writer.writerow([code, f"{lat:.4f}", f"{lon:.4f}"])
    print(len(places), "places")


if __name__ == "__main__":
    main(sys.argv[1])
