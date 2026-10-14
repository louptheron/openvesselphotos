"""
Builds src/features/Ports/coastlines.json: the land of each map frame (mainland Europe and the overseas regions shown
in insets) as SVG paths, from Natural Earth's public domain data (https://www.naturalearthdata.com).

Coordinates are in a Mercator projection measured in degrees: x = longitude, y = -mercator(latitude), so that north is
up. The frontend projects the ports the same way (see projection.ts).

Usage, with shapely installed:
    python3 scripts/coastlines.py ne_50m_land.geojson ne_10m_land.geojson ne_10m_minor_islands.geojson
from https://github.com/nvkelso/natural-earth-vector/tree/master/geojson
"""

import json
import math
import sys
from pathlib import Path

from shapely.geometry import box, shape
from shapely.ops import transform, unary_union

# Clipped a little beyond what a map shows, so that no coast is drawn along its edges
FRAMES = {
    # id: (west, south, east, north), detail, simplification tolerance in degrees, decimals
    "europe": ((-32, 26, 46, 72), "50m", 0.02, 2),
    "GP": ((-61.95, 15.75, -60.9, 16.6), "10m", 0.002, 3),
    "MQ": ((-61.35, 14.3, -60.7, 14.95), "10m", 0.002, 3),
    "GF": ((-54.8, 1.9, -51.3, 6.1), "10m", 0.005, 3),
    "RE": ((55.1, -21.45, 55.95, -20.8), "10m", 0.002, 3),
    "YT": ((44.9, -13.1, 45.4, -12.55), "10m", 0.001, 3),
    "PM": ((-56.5, 46.7, -56.05, 47.2), "10m", 0.001, 3),
    "IC": ((-18.4, 27.4, -13.2, 29.6), "10m", 0.004, 3),
    "AZ": ((-31.5, 36.7, -24.8, 39.9), "10m", 0.004, 3),
    "MD": ((-17.5, 32.2, -16.1, 33.3), "10m", 0.002, 3),
}


def mercator(lon, lat, z=None):
    return lon, -math.degrees(math.log(math.tan(math.pi / 4 + math.radians(lat) / 2)))


def load(path):
    return [shape(feature["geometry"]) for feature in json.loads(Path(path).read_text())["features"]]


def to_path(geometry, decimals):
    polygons = geometry.geoms if geometry.geom_type == "MultiPolygon" else [geometry]
    commands = []
    for polygon in polygons:
        if polygon.is_empty or polygon.geom_type != "Polygon":
            continue
        for ring in [polygon.exterior, *polygon.interiors]:
            points = []
            for x, y in ring.coords[:-1]:
                point = (round(x, decimals), round(y, decimals))
                if not points or points[-1] != point:
                    points.append(point)
            if len(points) >= 3:
                commands.append("M" + "L".join(f"{x:g} {y:g}" for x, y in points) + "Z")

    return "".join(commands)


def main(land_50m, land_10m, minor_islands_10m):
    lands = {"50m": unary_union(load(land_50m)), "10m": unary_union(load(land_10m) + load(minor_islands_10m))}
    frames = {}
    for frame_id, ((west, south, east, north), detail, tolerance, decimals) in FRAMES.items():
        land = lands[detail].intersection(box(west, south, east, north))
        projected = transform(mercator, land).simplify(tolerance, preserve_topology=True)
        x0, y0 = mercator(west, north)
        x1, y1 = mercator(east, south)
        frames[frame_id] = {"bounds": [round(x0, 4), round(y0, 4), round(x1, 4), round(y1, 4)], "land": to_path(projected, decimals)}
        print(frame_id, len(frames[frame_id]["land"]), "characters")
    output = Path(__file__).parent.parent / "src/features/Ports/coastlines.json"
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(frames, separators=(",", ":")))


if __name__ == "__main__":
    main(*sys.argv[1:])
