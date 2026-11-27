#!/usr/bin/env python3
"""
Looks for new editions of the reference data of La Pêche française, which are copied or imported by hand:

- the EU TACs and France's quotas (backend/src/main/resources/eu/fishing-opportunities.csv): a regulation amending the
  one in use, a corrigendum, or the next regulation fixing the fishing opportunities in Union waters;
- the Ifremer stock statuses (backend/src/main/resources/ifremer/stock-status.csv): the next yearly "Bilan … du statut
  des ressources halieutiques";
- FranceAgriMer's auction sales ("Ventes déclarées en halles à marée"): the workbook of the year after the one shown in
  production, which is imported in /admin.

Each new edition is printed; with --open-issues, it opens one GitHub issue (label "reference-data") unless an issue
already exists for it, open or closed: closing the issue once the data is updated is enough. The acts of the EU already
reviewed are listed in the "# checked:" line of fishing-opportunities.csv, including those that change nothing shown.

Standard library only; needs the `gh` CLI and GH_TOKEN for --open-issues. Exits with 1 when a source can not be read,
so that a scheduled run fails visibly instead of finding nothing.
"""

import argparse
import datetime
import json
import os
import re
import subprocess
import sys
import urllib.error
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ElementTree
from dataclasses import dataclass
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RESOURCES = ROOT / "backend/src/main/resources"
EU_CSV = RESOURCES / "eu/fishing-opportunities.csv"
IFREMER_CSV = RESOURCES / "ifremer/stock-status.csv"

USER_AGENT = "openvesselphotos-reference-data-watch"
EU_SPARQL = "https://publications.europa.eu/webapi/rdf/sparql"
ARCHIMER_OAI = "https://archimer.ifremer.fr/oai/OAIHandler"
FRANCE_AGRIMER_FILES = "https://www.franceagrimer.fr/sites/default/files"
PRODUCTION_URL = os.environ.get("PRODUCTION_URL", "https://lapechefrancaise.com")

LABEL = "reference-data"


@dataclass
class Finding:
    key: str
    title: str
    body: str


def http_get(url: str, data: bytes | None = None, headers: dict | None = None, timeout: int = 90) -> bytes:
    request = urllib.request.Request(url, data=data, headers={"User-Agent": USER_AGENT, **(headers or {})})
    with urllib.request.urlopen(request, timeout=timeout) as response:
        return response.read()


def exists(url: str) -> bool:
    request = urllib.request.Request(url, method="HEAD", headers={"User-Agent": USER_AGENT})
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            return response.status == 200
    except urllib.error.HTTPError as error:
        if error.code == 404:
            return False
        raise


def metadata(path: Path) -> dict[str, str]:
    """The "# key: value" comments of a reference CSV"""
    values = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        match = re.match(r"^#\s*(\w+):\s*(.+)$", line)
        if match:
            values[match.group(1)] = match.group(2).strip()
    return values


def sparql(query: str) -> list[dict[str, str]]:
    body = urllib.parse.urlencode({"query": query}).encode()
    response = http_get(
        EU_SPARQL,
        data=body,
        headers={
            "Accept": "application/sparql-results+json",
            "Content-Type": "application/x-www-form-urlencoded",
        },
    )
    bindings = json.loads(response)["results"]["bindings"]
    return [{name: value["value"] for name, value in binding.items()} for binding in bindings]


def check_eu() -> list[Finding]:
    values = metadata(EU_CSV)
    regulation = re.search(r"/eli/reg/(\d{4})/(\d+)/", values["url"])
    if not regulation:
        raise ValueError(f"No regulation in the url of {EU_CSV.name}: {values['url']}")
    checked = set(values.get("checked", "").split())

    # The acts that amend the regulation in use, the corrigenda of it and of its amendments, and the next regulations
    # of the Council fixing the fishing opportunities in Union waters. Other seas (Baltic, Mediterranean) and the
    # fisheries agreements with third countries are left out.
    year = regulation.group(1)
    celex = f"3{year}R{int(regulation.group(2)):04d}"
    rows = sparql(
        f"""
        PREFIX cdm: <http://publications.europa.eu/ontology/cdm#>
        SELECT DISTINCT ?work ?celex ?date ?title WHERE {{
          ?base cdm:resource_legal_id_celex "{celex}"^^<http://www.w3.org/2001/XMLSchema#string> .
          {{
            {{ ?work cdm:resource_legal_amends_resource_legal ?base }}
            UNION {{ ?work cdm:resource_legal_corrects_resource_legal ?base }}
            UNION {{ ?work cdm:resource_legal_corrects_resource_legal ?amendment .
                    ?amendment cdm:resource_legal_amends_resource_legal ?base }}
          }} UNION {{
            ?work cdm:work_created_by_agent <http://publications.europa.eu/resource/authority/corporate-body/CONSIL> ;
                  cdm:work_has_resource-type <http://publications.europa.eu/resource/authority/resource-type/REG> ;
                  cdm:work_date_document ?adopted .
            FILTER(?adopted >= "{year}-01-01"^^<http://www.w3.org/2001/XMLSchema#date>)
            ?named cdm:expression_belongs_to_work ?work ;
                   cdm:expression_uses_language <http://publications.europa.eu/resource/authority/language/ENG> ;
                   cdm:expression_title ?name .
            FILTER(CONTAINS(LCASE(STR(?name)), "fixing") &&
                   CONTAINS(LCASE(STR(?name)), "fishing opportunities") &&
                   CONTAINS(LCASE(STR(?name)), "union waters"))
          }}
          ?work cdm:resource_legal_id_celex ?celex ;
                cdm:work_date_document ?date .
          ?expression cdm:expression_belongs_to_work ?work ;
                      cdm:expression_uses_language <http://publications.europa.eu/resource/authority/language/ENG> ;
                      cdm:expression_title ?title .
        }} ORDER BY ?date
        """
    )
    findings = []
    for row in rows:
        celex = row["celex"]
        if celex in checked:
            continue
        pdf = french_pdf(row["work"])
        findings.append(
            Finding(
                key=f"eu:{celex}",
                title=f"TAC et quotas : nouvel acte de l'UE {celex} ({row['date']})",
                body="\n".join(
                    [
                        f"**{row['title']}**",
                        "",
                        f"- EUR-Lex : https://eur-lex.europa.eu/legal-content/FR/TXT/?uri=CELEX:{celex}",
                        f"- PDF (Cellar, téléchargeable par script) : {pdf}" if pdf else "",
                        "",
                        "Pour mettre à jour `backend/src/main/resources/eu/fishing-opportunities.csv` :",
                        "",
                        "- [ ] `pdftotext -layout` sur le PDF, puis lister les codes TAC des tableaux qu'il remplace",
                        "- [ ] Les comparer aux `tac_code` du CSV ; corriger `tac_tonnes` / `france_tonnes` des lignes touchées"
                        " (un TAC sur deux régions a deux lignes)",
                        "- [ ] Pour un nouveau règlement annuel : changer `# year`, `# url` et l'en-tête, puis reprendre"
                        " toutes les lignes",
                        f"- [ ] Ajouter `{celex}` à la ligne `# checked:`, même si l'acte ne change aucun TAC affiché",
                        "- [ ] `./gradlew :backend:test`",
                    ]
                ),
            )
        )
    return findings


def french_pdf(work: str) -> str | None:
    rows = sparql(
        f"""
        PREFIX cdm: <http://publications.europa.eu/ontology/cdm#>
        SELECT ?item WHERE {{
          ?expression cdm:expression_belongs_to_work <{work}> ;
                      cdm:expression_uses_language <http://publications.europa.eu/resource/authority/language/FRA> .
          ?manifestation cdm:manifestation_manifests_expression ?expression ;
                         cdm:manifestation_type ?type .
          FILTER(STRSTARTS(STR(?type), "pdf"))
          ?item cdm:item_belongs_to_manifestation ?manifestation .
        }}
        """
    )
    return rows[0]["item"] if rows else None


OAI = {"oai": "http://www.openarchives.org/OAI/2.0/", "dc": "http://purl.org/dc/elements/1.1/"}


def check_ifremer() -> list[Finding]:
    edition = int(metadata(IFREMER_CSV)["edition"])
    # Each Bilan is published in the spring of the year after its edition: harvest every record since then
    url = (
        f"{ARCHIMER_OAI}?verb=ListRecords&metadataPrefix=oai_dc&set=LANGUE:FR&from={edition + 1}-01-01"
    )
    findings = {}
    pages = 0
    while url:
        pages += 1
        if pages > 500:
            raise RuntimeError("Archimer: too many pages")
        root = ElementTree.fromstring(http_get(url))
        error = root.find("oai:error", OAI)
        if error is not None:
            if error.get("code") == "noRecordsMatch":
                break
            raise RuntimeError(f"Archimer: {error.get('code')} {error.text}")
        for record in root.iterfind(".//oai:record", OAI):
            for title in record.iterfind(".//dc:title", OAI):
                match = re.search(r"Bilan (\d{4}) du statut des ressources halieutiques", title.text or "")
                if match and int(match.group(1)) > edition:
                    links = [
                        identifier.text
                        for identifier in record.iterfind(".//dc:identifier", OAI)
                        if (identifier.text or "").startswith("http")
                    ]
                    year = int(match.group(1))
                    findings[year] = Finding(
                        key=f"ifremer:{year}",
                        title=f"Statut des stocks : Bilan Ifremer {year} publié",
                        body="\n".join(
                            [
                                f"**{title.text}**",
                                "",
                                *[f"- {link}" for link in links],
                                "",
                                "Pour mettre à jour `backend/src/main/resources/ifremer/stock-status.csv` :",
                                "",
                                "- [ ] Rendre les pages de l'Annexe 3 en PNG (`pdftoppm`) : ses tableaux sont des images",
                                "- [ ] Reprendre chaque stock, statut et tonnage par région, avec les noms FranceAgriMer",
                                "- [ ] Changer `# edition`, `# url` et l'en-tête",
                                "- [ ] `./gradlew :backend:test` (les TAC doivent toujours correspondre à des stocks)",
                            ]
                        ),
                    )
        token = root.find(".//oai:resumptionToken", OAI)
        url = (
            f"{ARCHIMER_OAI}?verb=ListRecords&resumptionToken={urllib.parse.quote(token.text)}"
            if token is not None and token.text
            else None
        )
    return list(findings.values())


def check_france_agrimer(today: datetime.date) -> list[Finding]:
    ports = json.loads(http_get(f"{PRODUCTION_URL}/v1/ports"))
    years = [port["auction"]["year"] for port in ports if isinstance(port.get("auction"), dict)]
    if not years:
        raise RuntimeError(f"No auction sales in {PRODUCTION_URL}/v1/ports")
    year = max(years) + 1
    # The workbook of a year is published in the next one (A25 in 2026-07), in the folder of its month. Look until
    # the end of the year after, in case it is late.
    for publication_year in (year + 1, year + 2):
        for month in range(1, 13):
            if datetime.date(publication_year, month, 1) > today:
                return []
            for extension in ("xlsm", "pdf"):
                url = (
                    f"{FRANCE_AGRIMER_FILES}/{publication_year}-{month:02d}/"
                    f"BIL-MER-VENTES_HAM-A{year % 100:02d}.{extension}"
                )
                if exists(url):
                    return [
                        Finding(
                            key=f"franceagrimer:{year}",
                            title=f"Ventes en criée : bilan FranceAgriMer {year} publié",
                            body="\n".join(
                                [
                                    f"La production montre les ventes de {year - 1} ; FranceAgriMer a publié {year} :",
                                    "",
                                    f"- {url}",
                                    "",
                                    "- [ ] Télécharger le classeur `.xlsm` (le PDF a les mêmes chiffres, mais ne"
                                    " s'importe pas)",
                                    "- [ ] L'importer dans `/admin` → « Ventes en criée »",
                                    "- [ ] Rattacher les criées restées sans port (`ImportAuctionSales.ALIASES`)",
                                ]
                            ),
                        )
                    ]
    return []


def open_issues(findings: list[Finding]) -> None:
    subprocess.run(
        ["gh", "label", "create", LABEL, "--color", "0e8a16", "--description", "A new edition of reference data",
         "--force"],
        check=True,
        capture_output=True,
    )
    listed = subprocess.run(
        ["gh", "issue", "list", "--label", LABEL, "--state", "all", "--limit", "1000", "--json", "body"],
        check=True,
        capture_output=True,
        text=True,
    )
    bodies = "\n".join(issue["body"] for issue in json.loads(listed.stdout))
    for finding in findings:
        marker = f"<!-- {LABEL}:{finding.key} -->"
        if marker in bodies:
            print(f"{finding.key}: issue already opened")
            continue
        subprocess.run(
            ["gh", "issue", "create", "--label", LABEL, "--title", finding.title, "--body",
             f"{finding.body}\n\n{marker}"],
            check=True,
        )


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("--open-issues", action="store_true", help="open a GitHub issue for each new edition")
    arguments = parser.parse_args()

    today = datetime.date.today()
    checks = {
        "EU TACs": check_eu,
        "Ifremer": check_ifremer,
        "FranceAgriMer": lambda: check_france_agrimer(today),
    }
    findings = []
    failed = False
    for name, check in checks.items():
        try:
            found = check()
        except Exception as error:  # noqa: BLE001 - report every source, then fail
            print(f"::error::{name}: {error!r}")
            failed = True
            continue
        print(f"{name}: {len(found) or 'no'} new edition{'s' if len(found) != 1 else ''}")
        for finding in found:
            print(f"  {finding.key}: {finding.title}")
        findings += found

    if arguments.open_issues and findings:
        open_issues(findings)
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
