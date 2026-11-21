import { useDeferredValue, useEffect, useMemo, useState } from 'react'
import { Link, useLocation, useSearchParams } from 'react-router-dom'
import styled, { css } from 'styled-components'

import { useGetPortQuery, useGetPortsDataQualityQuery, useSearchPortVesselsQuery } from '../api/api'
import type { Port, PortVessel } from '../api/types'
import { fr } from '../i18n/fr'
import { COLORS, FONTS, SKIES, getSkyAndSea } from '../theme'
import { getNamedPhotoUrl, getPhotoSlug } from '../utils/vesselKey'
import { MOBILE_QUERY, PANEL_MARGIN, PANEL_WIDTH, SHEET_HEIGHT } from './layout'
import { PortPortrait } from './PortPortrait'
import { AuctionList } from './AuctionList'
import { AuctionSales } from './AuctionSales'
import { FishingFacts } from './FishingFacts'
import { HomePortCorrectionDialog } from './HomePortCorrectionDialog'
import { type GearFamily, GearIcon, countVessels, hasGear } from './gears'
import { FactPill, INDUSTRIAL_LENGTH } from './impacts'
import { type Extent, REGIONS, isInExtent } from './regions'
import { SiteMark } from './SiteMark'
import { formatDate, formatNumber, formatPercent, getPortPath, normalize, SITE_NAME, SITE_TAGLINE } from './texts'

const OPEN_VESSEL_PHOTOS_URL = 'https://openvesselphotos.org/fr'
/** In the URL of a port, the CFR of the vessel found by the search, e.g. ?navire=FRA000123456 */
const VESSEL_PARAM = 'navire'

/** The query string with the parameter set, or removed when undefined */
function withParam(search: string, name: string, value: string | undefined): string {
  const params = new URLSearchParams(search)
  if (value === undefined) {
    params.delete(name)
  } else {
    params.set(name, value)
  }
  const query = params.toString()

  return query ? `?${query}` : ''
}

type Props = {
  /** Shows only the vessels using this family of gears */
  gear: GearFamily | undefined
  isError: boolean
  isLoading: boolean
  locode: string | undefined
  onFitExtent: (extent: Extent) => void
  onHighlight: (locode: string | undefined) => void
  onSelect: (locode: string | undefined) => void
  ports: Port[]
}

/** Floating over the map on wide screens, a sheet at the bottom on phones */
export function PortPanel({ gear, isError, isLoading, locode, onFitExtent, onHighlight, onSelect, ports }: Props) {
  const [isExpanded, setIsExpanded] = useState(false)
  const { search } = useLocation()

  return (
    <Panel $isExpanded={isExpanded} aria-label="Ports et navires">
      <SheetHandle
        aria-expanded={isExpanded}
        aria-label={isExpanded ? 'Réduire le panneau' : 'Agrandir le panneau'}
        onClick={() => setIsExpanded(!isExpanded)}
        type="button"
      />
      <Brand>
        <BrandLink to={{ pathname: '/', search }}>
          <BrandMark>
            <SiteMark size={32} />
          </BrandMark>
          <span>
            <BrandName>{SITE_NAME}</BrandName>
            <BrandTagline>{SITE_TAGLINE}</BrandTagline>
          </span>
        </BrandLink>
      </Brand>
      {locode ? (
        <PortView gear={gear} key={locode} locode={locode} />
      ) : (
        <PortsView
          gear={gear}
          isError={isError}
          isLoading={isLoading}
          onFitExtent={onFitExtent}
          onHighlight={onHighlight}
          onSelect={onSelect}
          ports={ports}
        />
      )}
      <Footer>
        <a href="/especes">Espèces et prix</a> · <a href="/petite-peche-cotiere">Petite pêche</a> ·{' '}
        <a href="/methodologie">Méthodologie et sources</a> · Photos de{' '}
        <a href={OPEN_VESSEL_PHOTOS_URL} rel="noopener" target="_blank">
          Open Vessel Photos
        </a>{' '}
        · Fond de carte © OpenStreetMap
      </Footer>
    </Panel>
  )
}

type SortOrder = 'count' | 'name'

function PortsView({ gear, isError, isLoading, onFitExtent, onHighlight, onSelect, ports }: Omit<Props, 'locode'>) {
  const { search: urlSearch } = useLocation()
  const [query, setQuery] = useState('')
  const [sortOrder, setSortOrder] = useState<SortOrder>('count')
  const deferredQuery = useDeferredValue(query)
  const vesselQuery = deferredQuery.trim()
  const { data: foundVessels = [] } = useSearchPortVesselsQuery(vesselQuery, { skip: vesselQuery.length < 2 })
  const regions = useMemo(
    () =>
      REGIONS.map(region => ({
        ...region,
        portCount: ports.filter(port => isInExtent(port, region.extent)).length
      })).filter(region => region.portCount > 0),
    [ports]
  )
  const shownPorts = useMemo(() => {
    const search = normalize(deferredQuery.trim())

    return (
      ports
        // With a gear filter, the ports where vessels use it, by their number
        .map(port => ({ count: countVessels(port, gear), port }))
        .filter(({ count }) => count > 0)
        .filter(
          ({ port }) => !search || normalize(port.name).includes(search) || normalize(port.locode).includes(search)
        )
        // oxlint-disable-next-line unicorn/no-array-sort
        .sort((a, b) =>
          sortOrder === 'count'
            ? b.count - a.count || a.port.name.localeCompare(b.port.name, 'fr')
            : a.port.name.localeCompare(b.port.name, 'fr')
        )
    )
  }, [deferredQuery, gear, ports, sortOrder])
  const maxCount = Math.max(1, ...shownPorts.map(({ count }) => count))

  return (
    <>
      <SearchBar>
        <SearchField
          onChange={setQuery}
          onSubmit={() => shownPorts[0] && onSelect(shownPorts[0].port.locode)}
          placeholder="Rechercher un port ou un navire"
          value={query}
        />
      </SearchBar>
      <Scroll>
        {!query && (
          <Section>
            <SectionHeader>
              <SectionTitle>Comprendre la pêche française</SectionTitle>
            </SectionHeader>
            <FishingFacts />
          </Section>
        )}
        {!query && ports.some(port => port.auction) && (
          <Section>
            <SectionHeader>
              <SectionTitle>Les criées</SectionTitle>
            </SectionHeader>
            <AuctionList onHighlight={onHighlight} ports={ports} search={urlSearch} />
          </Section>
        )}
        {query && foundVessels.length > 0 && (
          <Section>
            <SectionHeader>
              <SectionTitle>Navires</SectionTitle>
            </SectionHeader>
            <FoundVessels>
              {foundVessels.map(vessel => (
                <li key={vessel.cfr}>
                  <Link
                    to={{
                      pathname: getPortPath(
                        ports.find(port => port.locode === vessel.portLocode) ?? { locode: vessel.portLocode }
                      ),
                      search: withParam(urlSearch, VESSEL_PARAM, vessel.cfr)
                    }}
                  >
                    <RowIcon>
                      <BoatIcon />
                    </RowIcon>
                    <span>
                      <strong>{vessel.name ?? 'Navire sans nom'}</strong>
                      <Mono>{vessel.cfr}</Mono>
                    </span>
                    <FoundPort>{vessel.portName}</FoundPort>
                  </Link>
                </li>
              ))}
            </FoundVessels>
          </Section>
        )}
        {!query && regions.length > 1 && (
          <Section>
            <SectionHeader>
              <SectionTitle>Régions</SectionTitle>
            </SectionHeader>
            <Chips>
              {regions.map(region => (
                <Chip key={region.name} onClick={() => onFitExtent(region.extent)} type="button">
                  {region.name}
                  <ChipCount>{region.portCount}</ChipCount>
                </Chip>
              ))}
            </Chips>
          </Section>
        )}
        <Section>
          <SectionHeader>
            <SectionTitle>
              {deferredQuery || gear
                ? `${shownPorts.length} port${shownPorts.length > 1 ? 's' : ''}${gear ? ` · ${gear.label}` : ''}`
                : 'Tous les ports'}
            </SectionTitle>
            <Segmented
              label="Trier les ports"
              onChange={setSortOrder}
              options={[
                { label: 'Flotte', value: 'count' },
                { label: 'A–Z', value: 'name' }
              ]}
              value={sortOrder}
            />
          </SectionHeader>
          {isError && <ErrorText>Les ports n’ont pas pu être chargés. Réessayez dans un instant.</ErrorText>}
          {isLoading && <Skeletons count={8} height={44} />}
          <PortList>
            {shownPorts.map(({ count, port }) => (
              <li key={port.locode}>
                <PortRow
                  onBlur={() => onHighlight(undefined)}
                  onClick={() => onHighlight(undefined)}
                  onFocus={() => onHighlight(port.locode)}
                  onMouseEnter={() => onHighlight(port.locode)}
                  onMouseLeave={() => onHighlight(undefined)}
                  to={{ pathname: getPortPath(port), search: urlSearch }}
                >
                  <PortName>
                    <span>{port.name}</span>
                    <Locode>{port.locode}</Locode>
                  </PortName>
                  <PortCount>{formatNumber(count)}</PortCount>
                  <Bar aria-hidden style={{ width: `${(count / maxCount) * 100}%` }} />
                </PortRow>
              </li>
            ))}
          </PortList>
          {!isLoading && shownPorts.length === 0 && (
            <Empty>
              {query ? (
                <>Aucun port ne correspond à «&nbsp;{query}&nbsp;».</>
              ) : (
                'Aucun navire connu n’utilise cet engin.'
              )}
            </Empty>
          )}
        </Section>
        <Section>
          <DataQualityNote />
        </Section>
      </Scroll>
    </>
  )
}

/** How complete and how fresh the data is: shown, so that its limits are known (see /methodologie) */
function DataQualityNote() {
  const { data } = useGetPortsDataQualityQuery()
  if (!data) {
    return null
  }

  return (
    <SourcesNote>
      {data.share !== null && (
        <>
          Port d’attache connu pour {formatPercent(data.share)} des {formatNumber(data.registerVessels)} navires
          français du registre de la flotte de l’UE : {formatNumber(data.fromObservations)} d’après leurs positions à
          quai, {formatNumber(data.fromPortVisits)} d’après leurs escales (Global Fishing Watch)
          {data.fromRegistration > 0 &&
            `, ${formatNumber(data.fromRegistration)} d’après leur quartier d’immatriculation, faute de mieux`}
          .{' '}
        </>
      )}
      {data.lastUpdate && <>Mis à jour le {formatDate(data.lastUpdate)} · </>}
      <a href="/mises-a-jour">Historique des mises à jour</a> · <a href="/methodologie">Méthodologie</a>
    </SourcesNote>
  )
}

function PortView({ gear, locode }: { gear: GearFamily | undefined; locode: string }) {
  const { data, isError, isLoading } = useGetPortQuery(locode)
  const { pathname, search: urlSearch } = useLocation()
  const [searchParams] = useSearchParams()
  // A vessel found by the search: shown even when the gear filter would hide it, and scrolled to
  const targetCfr = searchParams.get(VESSEL_PARAM)
  const [query, setQuery] = useState('')
  const [correctedVessel, setCorrectedVessel] = useState<PortVessel | undefined>(undefined)
  const deferredQuery = useDeferredValue(query)
  const portVessels = useMemo(() => data?.vessels ?? [], [data])

  useEffect(() => {
    if (data && targetCfr) {
      document.getElementById(`navire-${targetCfr}`)?.scrollIntoView({ behavior: 'smooth', block: 'center' })
    }
  }, [data, targetCfr])
  const shownVessels = useMemo(() => {
    const search = normalize(deferredQuery.trim())

    return portVessels.filter(
      vessel =>
        (hasGear(vessel, gear) || vessel.cfr === targetCfr) &&
        (!search ||
          normalize(vessel.name ?? '').includes(search) ||
          normalize(vessel.cfr).includes(search) ||
          normalize(vessel.registration ?? '').includes(search))
    )
  }, [deferredQuery, gear, portVessels, targetCfr])
  const gearVesselCount = portVessels.filter(vessel => hasGear(vessel, gear)).length
  const photographed = shownVessels.filter(vessel => vessel.mainPhoto)
  const notPhotographed = shownVessels.filter(vessel => !vessel.mainPhoto)
  const photoCount = portVessels.filter(vessel => vessel.mainPhoto).length
  const ownerSources = [...new Set(portVessels.flatMap(vessel => (vessel.owner ? [vessel.owner.sourceUrl] : [])))]

  return (
    <>
      <SearchBar>
        <BackLink
          aria-label="Tous les ports"
          title="Tous les ports"
          to={{ pathname: '/', search: withParam(urlSearch, VESSEL_PARAM, undefined) }}
        >
          <BackIcon />
        </BackLink>
        <SearchField onChange={setQuery} placeholder="Nom, CFR ou immatriculation" value={query} />
      </SearchBar>
      <Scroll>
        {isError && (
          <Intro>
            <Title>Port introuvable</Title>
            <Lead>
              Ce port n’existe pas ou n’a aucun navire. <Link to="/">Voir tous les ports</Link>
            </Lead>
          </Intro>
        )}
        {isLoading && (
          <Intro>
            <Skeletons count={1} height={36} />
            <Skeletons count={3} height={120} />
          </Intro>
        )}
        {data && (
          <>
            <Intro>
              <Eyebrow>
                Port d’attache · <Locode>{data.port.locode}</Locode>
              </Eyebrow>
              <Title>{data.port.name}</Title>
              {gear && (
                <FilterNote>
                  <FilterIcon>
                    <GearIcon family={gear} size={20} />
                  </FilterIcon>
                  <span>
                    <strong>
                      {gearVesselCount} sur {portVessels.length}
                    </strong>{' '}
                    {gearVesselCount > 1 ? 'navires pêchent' : 'navire pêche'} avec cet engin ·{' '}
                    {gear.label.toLowerCase()}
                  </span>
                  <Link to={{ pathname }}>Toute la flotte</Link>
                </FilterNote>
              )}
              <PortPortrait vessels={portVessels} />
              {data.auction && (
                // On wide screens, on the right of the map (see PortsApp)
                <OnlyOnPhones>
                  <AuctionSales auction={data.auction} />
                </OnlyOnPhones>
              )}
            </Intro>
            {photographed.length > 0 && (
              <Section>
                <SectionHeader>
                  <SectionTitle>En photo · {photographed.length}</SectionTitle>
                </SectionHeader>
                <VesselGrid>
                  {photographed.map(vessel => (
                    <VesselCard
                      isTarget={vessel.cfr === targetCfr}
                      key={vessel.cfr}
                      onCorrect={() => setCorrectedVessel(vessel)}
                      vessel={vessel}
                    />
                  ))}
                </VesselGrid>
              </Section>
            )}
            {notPhotographed.length > 0 && (
              <Section>
                <SectionHeader>
                  <SectionTitle>
                    {photographed.length > 0 ? 'Sans photo' : 'La flotte'} · {notPhotographed.length}
                  </SectionTitle>
                </SectionHeader>
                {photoCount === 0 && !deferredQuery && (
                  <Callout>
                    Aucun navire de ce port n’a encore de photo. Vous en avez pris un&nbsp;?{' '}
                    <a href={`${OPEN_VESSEL_PHOTOS_URL}/contribute`} rel="noopener" target="_blank">
                      Partagez-la sur Open Vessel Photos
                    </a>
                  </Callout>
                )}
                <VesselRows>
                  {notPhotographed.map(vessel => (
                    <VesselRow
                      isTarget={vessel.cfr === targetCfr}
                      key={vessel.cfr}
                      onCorrect={() => setCorrectedVessel(vessel)}
                      vessel={vessel}
                    />
                  ))}
                </VesselRows>
              </Section>
            )}
            {shownVessels.length === 0 && (
              <Section>
                <Empty>
                  {query ? (
                    <>Aucun navire ne correspond à «&nbsp;{query}&nbsp;».</>
                  ) : (
                    'Aucun navire de ce port n’utilise cet engin.'
                  )}
                </Empty>
              </Section>
            )}
            <Section>
              <SourcesNote>
                Pêche industrielle : navires de 24 m ou plus.
                {ownerSources.length > 0 && (
                  <>
                    {' '}
                    Armateurs :{' '}
                    {ownerSources.map((url, index) => (
                      <span key={url}>
                        {index > 0 && ', '}
                        <a href={url} rel="noopener" target="_blank">
                          {new URL(url).hostname.replace(/^www\./, '')}
                        </a>
                      </span>
                    ))}
                    .
                  </>
                )}
              </SourcesNote>
            </Section>
          </>
        )}
      </Scroll>
      {correctedVessel && (
        <HomePortCorrectionDialog
          key={correctedVessel.cfr}
          locode={locode}
          onClose={() => setCorrectedVessel(undefined)}
          vessel={correctedVessel}
        />
      )}
    </>
  )
}

/** The vessel's type, main gear and year built, from the EU fleet register */
function getFacts(vessel: PortVessel): string[] {
  return [
    vessel.vesselType ? (fr.register.vesselTypes[vessel.vesselType] ?? vessel.vesselType) : undefined,
    vessel.mainGear ? (fr.register.gears[vessel.mainGear] ?? vessel.mainGear) : undefined,
    vessel.yearBuilt ? `Construit en ${vessel.yearBuilt}` : undefined
  ].filter((fact): fact is string => Boolean(fact))
}

/** e.g. "11,9 m" */
function getLength(vessel: PortVessel): string | undefined {
  return vessel.lengthMeters ? `${vessel.lengthMeters.toLocaleString('fr', { maximumFractionDigits: 1 })} m` : undefined
}

function getVesselPageUrl(vessel: PortVessel): string | undefined {
  return vessel.pageUrl?.replace('/vessels/', '/fr/vessels/')
}

/** Whether it is an industrial vessel (24 m or more), its owner when known, and when its home port was deduced */
function VesselBadges({ vessel }: { vessel: PortVessel }) {
  const isIndustrial = vessel.lengthMeters !== null && vessel.lengthMeters >= INDUSTRIAL_LENGTH
  const isFromPortVisits = vessel.homePortSource === 'GFW'
  const isFromRegistration = vessel.homePortSource === 'REGISTER'
  const isCorrected = vessel.homePortSource === 'MANUAL'
  if (!isIndustrial && !vessel.owner && !isFromPortVisits && !isFromRegistration && !isCorrected) {
    return null
  }

  return (
    <Badges>
      {isIndustrial && <FactPill title="Navire de 24 m ou plus">Pêche industrielle</FactPill>}
      {isFromPortVisits && (
        <PortVisitsNote
          title={`Port de ${vessel.observationsAtPort} de ses ${vessel.observationsTotal} escales en un an, d’après ses positions AIS (Global Fishing Watch)`}
        >
          Port déduit de ses escales
        </PortVisitsNote>
      )}
      {isFromRegistration && (
        <PortVisitsNote title="Ni observé à quai ni suivi par AIS : rattaché au port de son quartier d’immatriculation, qui en compte souvent plusieurs">
          Port de son quartier d’immatriculation
        </PortVisitsNote>
      )}
      {isCorrected && (
        <PortVisitsNote title="Rattaché à ce port sur signalement d’un visiteur, vérifié">Port vérifié</PortVisitsNote>
      )}
      {vessel.owner && (
        <FactPill title={`Source : ${vessel.owner.sourceUrl}`}>
          {vessel.owner.name}
          {vessel.owner.group && ` · groupe ${vessel.owner.group}`}
        </FactPill>
      )}
    </Badges>
  )
}

type VesselItemProps = {
  isTarget: boolean
  /** Opens the form to say the vessel is based at another port */
  onCorrect: () => void
  vessel: PortVessel
}

function VesselCard({ isTarget, onCorrect, vessel }: VesselItemProps) {
  const photo = vessel.mainPhoto
  const name = vessel.name ?? 'Navire sans nom'
  const length = getLength(vessel)
  const facts = [...(length ? [length] : []), ...getFacts(vessel)]
  const pageUrl = getVesselPageUrl(vessel)
  const slug = getPhotoSlug(vessel)

  return (
    <CardItem id={`navire-${vessel.cfr}`}>
      <Card
        $isLink={Boolean(pageUrl)}
        data-target={isTarget}
        as={pageUrl ? 'a' : 'div'}
        href={pageUrl}
        rel={pageUrl ? 'noopener' : undefined}
        target={pageUrl ? '_blank' : undefined}
      >
        <Picture $sky={getSkyAndSea(SKIES[0] ?? COLORS.info, 72)}>
          {photo && (
            <img
              alt={`${name} © ${photo.credit}`}
              loading="lazy"
              sizes="(max-width: 760px) 50vw, 190px"
              src={getNamedPhotoUrl(photo.urls, slug, '320')}
              srcSet={`${getNamedPhotoUrl(photo.urls, slug, '320')} 320w, ${getNamedPhotoUrl(photo.urls, slug, '800')} 800w`}
            />
          )}
          {photo && <Credit>© {photo.credit}</Credit>}
        </Picture>
        <CardBody>
          <VesselName>{name}</VesselName>
          <Mono>{vessel.registration ?? vessel.cfr}</Mono>
          {facts.length > 0 && (
            <Facts>
              {facts.map(fact => (
                <li key={fact}>{fact}</li>
              ))}
            </Facts>
          )}
          <VesselBadges vessel={vessel} />
        </CardBody>
      </Card>
      <CorrectButton onClick={onCorrect} title={`${name} n’est pas basé à ce port ? Signalez-le`} type="button">
        Mauvais port&nbsp;?
      </CorrectButton>
    </CardItem>
  )
}

function VesselRow({ isTarget, onCorrect, vessel }: VesselItemProps) {
  const facts = getFacts(vessel)
  const length = getLength(vessel)
  const pageUrl = getVesselPageUrl(vessel)
  const name = vessel.name ?? 'Navire sans nom'
  const content = (
    <>
      <RowIcon>
        <BoatIcon />
      </RowIcon>
      <RowMain>
        <RowTitle>{name}</RowTitle>
        <Mono>{[vessel.registration, vessel.cfr].filter(Boolean).join(' · ')}</Mono>
        {facts.length > 0 && (
          <Facts>
            {facts.map(fact => (
              <li key={fact}>{fact}</li>
            ))}
          </Facts>
        )}
        <VesselBadges vessel={vessel} />
        {pageUrl && <RowCta>Voir sur Open Vessel Photos</RowCta>}
      </RowMain>
      <RowAside>
        {length && <Length title="Longueur hors tout">{length}</Length>}
        {pageUrl && <ArrowIcon />}
      </RowAside>
    </>
  )

  return (
    <Row id={`navire-${vessel.cfr}`}>
      {pageUrl ? (
        <RowContent
          as="a"
          data-target={isTarget}
          href={pageUrl}
          rel="noopener"
          target="_blank"
          title={`${name} sur Open Vessel Photos (nouvel onglet)`}
        >
          {content}
        </RowContent>
      ) : (
        <RowContent data-target={isTarget}>{content}</RowContent>
      )}
      <CorrectButton $inRow onClick={onCorrect} title={`${name} n’est pas basé à ce port ? Signalez-le`} type="button">
        Mauvais port&nbsp;?
      </CorrectButton>
    </Row>
  )
}

function SearchField({
  onChange,
  onSubmit,
  placeholder,
  value
}: {
  onChange: (value: string) => void
  onSubmit?: () => void
  placeholder: string
  value: string
}) {
  return (
    <SearchLabel>
      <SearchIcon />
      <input
        aria-label={placeholder}
        onChange={event => onChange(event.target.value)}
        onKeyDown={event => {
          if (event.key === 'Enter') {
            onSubmit?.()
          }
          if (event.key === 'Escape' && value) {
            event.stopPropagation()
            onChange('')
          }
        }}
        placeholder={placeholder}
        type="search"
        value={value}
      />
    </SearchLabel>
  )
}

function Segmented<T extends string>({
  label,
  onChange,
  options,
  value
}: {
  label: string
  onChange: (value: T) => void
  options: { label: string; value: T }[]
  value: T
}) {
  return (
    <SegmentedGroup aria-label={label}>
      {options.map(option => (
        <SegmentButton
          $isActive={option.value === value}
          aria-pressed={option.value === value}
          key={option.value}
          onClick={() => onChange(option.value)}
          type="button"
        >
          {option.label}
        </SegmentButton>
      ))}
    </SegmentedGroup>
  )
}

function Skeletons({ count, height }: { count: number; height: number }) {
  return (
    <SkeletonList aria-busy aria-label="Chargement">
      {Array.from({ length: count }, (_, index) => (
        <Skeleton key={index} style={{ height }} />
      ))}
    </SkeletonList>
  )
}

function SearchIcon() {
  return (
    <svg aria-hidden fill="none" height="18" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24" width="18">
      <circle cx="11" cy="11" r="7" />
      <path d="m20 20-4-4" strokeLinecap="round" />
    </svg>
  )
}

function BackIcon() {
  return (
    <svg aria-hidden fill="none" height="20" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24" width="20">
      <path d="M15 5l-7 7 7 7" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

function ArrowIcon() {
  return (
    <svg aria-hidden fill="none" height="16" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24" width="16">
      <path d="M7 17 17 7M9 7h8v8" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

function BoatIcon() {
  return (
    <svg aria-hidden fill="none" height="30" stroke="currentColor" strokeWidth="1.6" viewBox="0 0 32 32" width="30">
      <path d="M4 20h24l-3.5 6h-17z" strokeLinejoin="round" />
      <path d="M9 20v-6h10l3 6M13 14V8h4v6M19 8l3-3" strokeLinejoin="round" strokeLinecap="round" />
    </svg>
  )
}

/* Layout */

const Panel = styled.aside<{ $isExpanded: boolean }>`
  position: absolute;
  top: ${PANEL_MARGIN}px;
  bottom: ${PANEL_MARGIN}px;
  left: ${PANEL_MARGIN}px;
  z-index: 1;
  width: ${PANEL_WIDTH}px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid ${COLORS.border};
  border-radius: 6px;
  background: #fbfaf6;
  box-shadow: 0 1px 2px rgba(11, 34, 51, 0.1);

  @media (max-width: 760px) {
    top: auto;
    right: 0;
    bottom: 0;
    left: 0;
    width: auto;
    height: ${({ $isExpanded }) => ($isExpanded ? '88dvh' : `${SHEET_HEIGHT * 100}dvh`)};
    border-radius: 6px 22px 0 0;
    transition: height 0.3s cubic-bezier(0.2, 0.8, 0.2, 1);
  }
`

const SheetHandle = styled.button`
  display: none;

  @media (max-width: 760px) {
    display: block;
    flex: none;
    align-self: center;
    width: 64px;
    height: 20px;
    margin: 4px 0 -8px;
    border: none;
    background: transparent;
    cursor: pointer;

    &::before {
      content: '';
      display: block;
      width: 40px;
      height: 5px;
      margin: auto;
      border-radius: 3px;
      background: ${COLORS.border};
    }
  }
`

const Brand = styled.header`
  flex: none;
  padding: 18px 20px 6px;
`

const BrandLink = styled(Link)`
  display: flex;
  align-items: center;
  gap: 12px;
  color: ${COLORS.ink};
  text-decoration: none;

  &:hover {
    color: ${COLORS.ink};
  }
`

const BrandMark = styled.span`
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: 4px;
  background: ${COLORS.ink};
`

const BrandName = styled.span`
  display: block;
  font-family: ${FONTS.sans};
  font-size: 19px;
  font-weight: 600;
  letter-spacing: -0.01em;
`

const BrandTagline = styled.span`
  display: block;
  font-size: 12.5px;
  color: ${COLORS.muted};
`

const SearchBar = styled.div`
  flex: none;
  display: flex;
  gap: 8px;
  padding: 12px 20px 14px;
  border-bottom: 1px solid ${COLORS.divider};
`

const SearchLabel = styled.label`
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  height: 42px;
  padding: 0 14px;
  border: 1px solid ${COLORS.border};
  border-radius: 4px;
  background: ${COLORS.white};
  color: ${COLORS.muted};
  transition:
    border-color 0.15s,
    box-shadow 0.15s;

  &:focus-within {
    border-color: ${COLORS.link};
    box-shadow: 0 0 0 4px rgba(14, 74, 110, 0.12);
  }

  input {
    flex: 1;
    min-width: 0;
    border: none;
    outline: none;
    background: transparent;
    color: ${COLORS.ink};
    font: 15px ${FONTS.sans};
  }
`

const BackLink = styled(Link)`
  flex: none;
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border: 1px solid ${COLORS.border};
  border-radius: 4px;
  background: ${COLORS.white};
  color: ${COLORS.ink};
  transition: background 0.15s;

  &:hover {
    background: ${COLORS.info};
    color: ${COLORS.ink};
  }
`

const Scroll = styled.div`
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding-bottom: 12px;
  scrollbar-width: thin;
`

const Footer = styled.footer`
  flex: none;
  padding: 10px 20px 12px;
  border-top: 1px solid ${COLORS.divider};
  color: ${COLORS.muted};
  font-size: 11.5px;

  a {
    color: ${COLORS.muted};
  }
`

/* Content */

const Intro = styled.div`
  padding: 18px 20px 4px;
`

const Eyebrow = styled.p`
  margin: 0 0 4px;
  color: ${COLORS.accent};
  font-size: 12.5px;
  font-weight: 600;
`

const Title = styled.h1`
  margin: 0 0 8px;
  color: ${COLORS.ink};
  font-family: ${FONTS.sans};
  font-size: 24px;
  font-weight: 600;
  letter-spacing: -0.02em;
  line-height: 1.12;
  text-wrap: balance;
`

const Lead = styled.p`
  margin: 0 0 16px;
  color: ${COLORS.text};
  font-size: 14.5px;
  line-height: 1.5;
  text-wrap: pretty;
`

const Section = styled.section`
  padding: 18px 20px 0;
`

const SectionHeader = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
`

/** A label over a thin rule, as in a printed table */
const SectionTitle = styled.h2`
  flex: 1;
  margin: 0;
  padding-bottom: 5px;
  border-bottom: 1px solid ${COLORS.border};
  color: ${COLORS.ink};
  font-size: 13px;
  font-weight: 600;
`

const Chips = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
`

const Chip = styled.button`
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 6px 6px 12px;
  border: 1px solid ${COLORS.border};
  border-radius: 6px;
  background: ${COLORS.white};
  color: ${COLORS.ink};
  font: 500 13px ${FONTS.sans};
  cursor: pointer;
  transition:
    background 0.15s,
    border-color 0.15s;

  &:hover {
    border-color: ${COLORS.link};
    background: ${COLORS.info};
  }
`

const ChipCount = styled.span`
  min-width: 22px;
  padding: 1px 7px;
  border-radius: 6px;
  background: ${COLORS.divider};
  color: ${COLORS.muted};
  font-size: 11.5px;
  text-align: center;
`

const SegmentedGroup = styled.fieldset`
  margin: 0;
  border: none;
  display: inline-flex;
  padding: 3px;
  border-radius: 4px;
  background: ${COLORS.divider};
`

const SegmentButton = styled.button<{ $isActive: boolean }>`
  padding: 5px 10px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: ${COLORS.muted};
  font: 500 12.5px ${FONTS.sans};
  white-space: nowrap;
  cursor: pointer;

  ${({ $isActive }) =>
    $isActive &&
    css`
      background: ${COLORS.white};
      box-shadow: inset 0 0 0 1px ${COLORS.border};
      color: ${COLORS.ink};
    `}
`

const PortList = styled.ul`
  margin: 0 -8px;
  padding: 0;
  list-style: none;
`

const PortRow = styled(Link)`
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 8px 12px;
  border-radius: 4px;
  color: ${COLORS.ink};
  text-decoration: none;
  transition: background 0.12s;

  &:hover {
    background: ${COLORS.white};
    color: ${COLORS.ink};
  }
`

const PortName = styled.span`
  display: flex;
  align-items: baseline;
  gap: 8px;
  min-width: 0;
  font-size: 14.5px;
  font-weight: 500;

  span {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
`

const Locode = styled.span`
  color: ${COLORS.muted};
  font-family: ${FONTS.mono};
  font-size: 11px;
  font-weight: 400;
`

const PortCount = styled.span`
  color: ${COLORS.ink};
  font-size: 14px;
  font-variant-numeric: tabular-nums;
  font-weight: 600;
`

const Bar = styled.span`
  position: absolute;
  bottom: 5px;
  left: 8px;
  max-width: calc(100% - 16px);
  height: 3px;
  border-radius: 2px;
  background: ${COLORS.link};
  opacity: 0.55;
`

const VesselGrid = styled.ul`
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;
`

const Card = styled.div<{ $isLink?: boolean }>`
  display: flex;
  flex-direction: column;
  height: 100%;

  &[data-target='true'] {
    outline: 3px solid ${COLORS.accent};
  }
  overflow: hidden;
  border: 1px solid ${COLORS.cardBorder};
  border-radius: 6px;
  background: ${COLORS.white};
  color: inherit;
  text-decoration: none;
  transition:
    transform 0.15s,
    box-shadow 0.15s;

  ${({ $isLink }) =>
    $isLink &&
    css`
      &:hover {
        color: inherit;
        border-color: ${COLORS.link};
      }
    `}
`

const Picture = styled.div<{ $sky: string }>`
  position: relative;
  aspect-ratio: 4 / 3;
  background: ${({ $sky }) => $sky};

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
`

const FilterNote = styled.div`
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px 10px;
  margin-top: 10px;
  padding: 2px 0 2px 10px;
  border-left: 3px solid ${COLORS.ink};
  color: ${COLORS.text};
  font-size: 13px;

  span {
    flex: 1;
    min-width: 160px;
  }

  strong {
    color: ${COLORS.ink};
  }

  a {
    color: ${COLORS.link};
    font-weight: 600;
    white-space: nowrap;
  }
`

const FilterIcon = styled.span`
  flex: none !important;
  min-width: 0 !important;
  display: grid;
  place-items: center;
  color: ${COLORS.ink};
`

/** The card, then the button to correct its port under it */
const CardItem = styled.li`
  display: flex;
  flex-direction: column;
  gap: 2px;

  > ${Card} {
    flex: 1;
    height: auto;
  }
`

const CorrectButton = styled.button<{ $inRow?: boolean }>`
  align-self: ${({ $inRow }) => ($inRow ? 'flex-end' : 'flex-start')};
  min-height: 32px;
  margin: ${({ $inRow }) => ($inRow ? '-6px 6px 2px 0' : '0')};
  padding: 0 6px;
  border: none;
  background: transparent;
  color: ${COLORS.muted};
  font: inherit;
  font-size: 12px;
  text-decoration: underline;
  text-underline-offset: 2px;
  cursor: pointer;

  &:hover,
  &:focus-visible {
    color: ${COLORS.link};
  }
`

const PortVisitsNote = styled.span`
  padding: 2px 8px;
  border: 1px dashed ${COLORS.border};
  border-radius: 6px;
  color: ${COLORS.muted};
  font-size: 11px;
`

const Badges = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 6px;
`

const SourcesNote = styled.p`
  margin: 4px 0 8px;
  color: ${COLORS.muted};
  font-size: 11.5px;
  line-height: 1.5;

  a {
    color: ${COLORS.muted};
  }
`

const FoundVessels = styled.ul`
  margin: 0 -8px;
  padding: 0;
  list-style: none;

  a {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 8px;
    border-radius: 4px;
    color: ${COLORS.ink};
    text-decoration: none;
  }

  a:hover {
    background: ${COLORS.white};
    color: ${COLORS.ink};
  }

  a > span:nth-child(2) {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
  }

  strong {
    overflow: hidden;
    font-size: 14px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
`

const FoundPort = styled.span`
  color: ${COLORS.muted};
  font-size: 12.5px;
  text-align: right;
`

const OnlyOnPhones = styled.div`
  @media not ${MOBILE_QUERY} {
    display: none;
  }
`

const Callout = styled.p`
  margin: 0 0 10px;
  padding: 2px 0 2px 10px;
  border-left: 3px solid ${COLORS.accent};
  color: ${COLORS.text};
  font-size: 13px;
  line-height: 1.45;

  a {
    color: ${COLORS.link};
    font-weight: 600;
  }
`

const VesselRows = styled.ul`
  margin: 0;
  padding: 0;
  list-style: none;
  border: 1px solid ${COLORS.cardBorder};
  border-radius: 6px;
  background: ${COLORS.white};
`

const Row = styled.li`
  display: flex;
  flex-direction: column;

  & + & {
    border-top: 1px solid ${COLORS.divider};
  }

  &:first-child > * {
    border-radius: 6px 14px 0 0;
  }

  &:last-child > * {
    border-radius: 0 0 14px 14px;
  }
`

const RowCta = styled.div`
  display: none;
  margin-top: 6px;
  color: ${COLORS.link};
  font-size: 12px;
  font-weight: 600;
`

const RowAside = styled.span`
  flex: none;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
  color: ${COLORS.muted};
`

const RowContent = styled.div`
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 10px 12px;
  color: inherit;
  text-decoration: none;

  /* The vessel found by the search */
  &[data-target='true'] {
    background: ${COLORS.accentSoft};
    box-shadow: inset 3px 0 0 ${COLORS.accent};
  }
  transition: background 0.12s;

  &[href]:hover,
  &[href]:focus-visible {
    background: ${COLORS.info};
    color: inherit;
  }

  &[href]:hover ${RowCta}, &[href]:focus-visible ${RowCta} {
    display: block;
  }

  &[href]:hover ${RowAside} svg {
    color: ${COLORS.link};
  }
`

const Length = styled.span`
  margin-top: 2px;
  color: ${COLORS.ink};
  font-family: ${FONTS.sans};
  font-size: 15px;
  font-variant-numeric: tabular-nums;
  font-weight: 600;
  white-space: nowrap;
`

const RowIcon = styled.span`
  flex: none;
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border-radius: 4px;
  background: ${COLORS.info};
  color: ${COLORS.link};

  svg {
    width: 22px;
    height: 22px;
  }
`

const RowMain = styled.div`
  flex: 1;
  min-width: 0;

  span {
    display: block;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
`

const RowTitle = styled.div`
  overflow: hidden;
  color: ${COLORS.ink};
  font-size: 14px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;

  a {
    color: inherit;
    text-decoration: none;
  }

  a:hover {
    color: ${COLORS.link};
    text-decoration: underline;
  }
`

const Credit = styled.span`
  position: absolute;
  right: 6px;
  bottom: 6px;
  max-width: calc(100% - 12px);
  overflow: hidden;
  padding: 2px 6px;
  border-radius: 6px;
  background: rgba(11, 34, 51, 0.6);
  color: ${COLORS.white};
  font-size: 10.5px;
  text-overflow: ellipsis;
  white-space: nowrap;
`

const CardBody = styled.div`
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 3px;
  padding: 10px 11px 11px;
`

const VesselName = styled.h3`
  margin: 0;
  overflow: hidden;
  color: ${COLORS.ink};
  font-size: 14px;
  font-weight: 600;
  line-height: 1.25;
  text-overflow: ellipsis;
  white-space: nowrap;
`

const Mono = styled.span`
  color: ${COLORS.muted};
  font-family: ${FONTS.mono};
  font-size: 11px;
`

const Facts = styled.ul`
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin: 4px 0 0;
  padding: 0;
  list-style: none;

  /* Plain text, separated by a middle dot */
  li {
    color: ${COLORS.text};
    font-size: 12px;
  }

  li + li::before {
    content: '·';
    margin-right: 4px;
    color: ${COLORS.muted};
  }
`

const ErrorText = styled.p`
  color: ${COLORS.reject};
  font-size: 14px;
`

const Empty = styled.p`
  margin: 8px 0;
  padding: 20px;
  border: 1px dashed ${COLORS.border};
  border-radius: 4px;
  color: ${COLORS.muted};
  font-size: 14px;
  text-align: center;
`

const SkeletonList = styled.div`
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin: 8px 0;
`

const Skeleton = styled.div`
  border-radius: 4px;
  background: ${COLORS.divider};
`
