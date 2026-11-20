-- LOCAL DEVELOPMENT ONLY: dummy EU fleet register data for the vessels of the ports site, so that their type, gear,
-- length and year show without importing the register. Run with `make load-dummy-fleet-register`, after importing the
-- home ports (/admin). Each vessel's values are picked from its CFR, so they stay the same from a run to another.
-- Vessels are created when missing; existing ones keep their data, only the missing fields are filled in.

WITH profiles (vessel_type, gears, min_length, max_length, weight) AS (
    VALUES
        -- Chalutiers
        ('TO', ARRAY['OTB', 'OTB', 'OTT', 'PTM'], 12.0, 24.0, 30),
        -- Fileyeurs
        ('GO', ARRAY['GNS', 'GNS', 'GTR'], 7.5, 14.0, 22),
        -- Polyvalents (casiers, filets, chaluts)
        ('MO', ARRAY['FPO', 'GNS', 'OTB'], 6.5, 12.0, 18),
        -- Palangriers
        ('LL', ARRAY['LLS', 'LLD'], 8.0, 16.0, 10),
        -- Ligneurs
        ('LO', ARRAY['LHM', 'LTL'], 5.5, 10.0, 10),
        -- Dragueurs
        ('DO', ARRAY['DRB'], 9.0, 16.0, 7),
        -- Bolincheurs
        ('SO', ARRAY['PS'], 14.0, 25.0, 3)
),
weighted AS (
    SELECT *, sum(weight) OVER (ORDER BY vessel_type) - weight AS from_weight, sum(weight) OVER () AS total_weight
    FROM profiles
),
home_vessels AS (
    SELECT h.cfr, h.vessel_name, p.name AS port, abs(hashtext(h.cfr)::bigint) AS hash
    FROM vessel_home_ports h
    JOIN ports p ON p.locode = h.port_locode
),
dummy AS (
    SELECT
        v.cfr,
        v.vessel_name,
        v.port,
        w.vessel_type,
        w.gears[1 + (v.hash / 7) % array_length(w.gears, 1)] AS main_gear,
        round((w.min_length + (w.max_length - w.min_length) * ((v.hash / 11) % 1000) / 1000.0)::numeric, 2) AS length_m,
        1968 + ((v.hash / 13) % 56)::int AS year_built,
        -- Call signs of French vessels start with F, e.g. FGHS
        'F' || chr(65 + ((v.hash / 17) % 26)::int) || chr(65 + ((v.hash / 19) % 26)::int)
            || chr(65 + ((v.hash / 23) % 26)::int) AS call_sign
    FROM home_vessels v
    JOIN weighted w ON v.hash % w.total_weight >= w.from_weight AND v.hash % w.total_weight < w.from_weight + w.weight
)
INSERT INTO vessels (cfr, name, port, vessel_type, main_gear, length_m, year_built, engine_power_kw, tonnage_gt,
                     call_sign, register_imported_on)
SELECT
    cfr,
    vessel_name,
    port,
    vessel_type,
    main_gear,
    length_m,
    year_built,
    -- Rough orders of magnitude for small fishing vessels
    round(length_m * 16, 2),
    round((0.05 * power(length_m, 2.5))::numeric, 2),
    call_sign,
    current_date
FROM dummy
ON CONFLICT (cfr) DO UPDATE SET
    name = coalesce(vessels.name, excluded.name),
    port = coalesce(vessels.port, excluded.port),
    vessel_type = coalesce(vessels.vessel_type, excluded.vessel_type),
    main_gear = coalesce(vessels.main_gear, excluded.main_gear),
    length_m = coalesce(vessels.length_m, excluded.length_m),
    year_built = coalesce(vessels.year_built, excluded.year_built),
    engine_power_kw = coalesce(vessels.engine_power_kw, excluded.engine_power_kw),
    tonnage_gt = coalesce(vessels.tonnage_gt, excluded.tonnage_gt),
    call_sign = coalesce(vessels.call_sign, excluded.call_sign),
    register_imported_on = coalesce(vessels.register_imported_on, excluded.register_imported_on);
