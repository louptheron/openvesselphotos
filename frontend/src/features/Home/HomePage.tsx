import { type FormEvent, useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import styled from 'styled-components'

import { useGetRecentVesselsQuery, useSearchVesselsQuery } from '../../api/api'
import { ChartCard, ChartSection, WaveDivider } from '../../components/Chart'
import { VesselCard } from '../../components/VesselCard'
import { Chip, Container, Muted, PrimaryButton, SectionTitle } from '../../components/ui'
import { usePageMeta } from '../../hooks/usePageMeta'
import { useTranslation } from '../../i18n'
import { CHART_COLORS, COLORS, FONTS } from '../../theme'
import { detectKey } from '../../utils/vesselKey'
import { ChartCredits } from './ChartCredits'
import { ChartVessels } from './ChartVessels'

export function HomePage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const query = searchParams.get('q') ?? ''
  const [input, setInput] = useState(query)
  const navigate = useNavigate()
  const isNarrow = useIsNarrow()
  const { t } = useTranslation()

  const isSearching = query.trim().length >= 2
  const searchResult = useSearchVesselsQuery(query, { skip: !isSearching })
  // Always loaded: the latest vessels are also drawn on the chart
  const recentResult = useGetRecentVesselsQuery()
  const { data: vessels, isError, isFetching } = isSearching ? searchResult : recentResult

  const search = (event: FormEvent) => {
    event.preventDefault()
    // A CFR or an MMSI opens the vessel page directly
    const key = detectKey(input)
    if (key) {
      navigate(`/vessels/${key.keyType}/${key.key}`)

      return
    }
    setSearchParams(input.trim() ? { q: input.trim() } : {})
  }

  const showExample = (example: string) => {
    setInput(example)
    const key = detectKey(example)
    if (key) {
      navigate(`/vessels/${key.keyType}/${key.key}`)

      return
    }
    setSearchParams({ q: example })
  }

  const detectedKey = isSearching ? detectKey(query) : undefined
  usePageMeta(isSearching ? t.home.searchPageTitle(query) : t.home.pageTitle)

  return (
    <>
      <ChartSection overlay={!recentResult.isLoading && <ChartVessels vessels={recentResult.data ?? []} />}>
        <Hero>
          <HeroCard>
            <Chips>
              {t.home.chips.map(chip => (
                <Chip key={chip}>{chip}</Chip>
              ))}
            </Chips>
            <h1>{t.home.title}</h1>
            <p>{t.home.lead}</p>
            <SearchForm aria-label={t.home.searchFormLabel} onSubmit={search}>
              <label className="visually-hidden" htmlFor="q">
                {t.home.searchLabel}
              </label>
              <input
                id="q"
                onChange={event => setInput(event.target.value)}
                placeholder={isNarrow ? t.home.searchPlaceholderShort : t.home.searchPlaceholder}
                type="search"
                value={input}
              />
              <PrimaryButton type="submit">{t.home.searchButton}</PrimaryButton>
            </SearchForm>
            <Muted>
              {t.home.try}{' '}
              <ExampleButton onClick={() => showExample('FRA000123456')} type="button">
                FRA000123456
              </ExampleButton>{' '}
              ·{' '}
              <ExampleButton onClick={() => showExample('227000101')} type="button">
                227000101
              </ExampleButton>{' '}
              ·{' '}
              <ExampleButton onClick={() => showExample('Ker Avel')} type="button">
                Ker Avel
              </ExampleButton>
            </Muted>
          </HeroCard>
        </Hero>
        {!recentResult.isLoading && <ChartCredits libraryVesselCount={recentResult.data?.length ?? 0} />}
      </ChartSection>

      <Results aria-label={t.home.resultsLabel} aria-live="polite">
        <ResultsHeader>
          <SectionTitle>{isSearching ? t.home.resultsFor(query) : t.home.recentlyAdded}</SectionTitle>
          {isFetching && <Muted>{t.common.loading}</Muted>}
        </ResultsHeader>
        {isError && <p>{t.home.searchUnavailable}</p>}
        {vessels && vessels.length === 0 && (
          <Empty>
            {isSearching ? (
              <>
                <p>{t.home.noMatch(query)}</p>
                {detectedKey ? (
                  <p>
                    {t.home.noPhotoOfKey(`${detectedKey.keyType.toUpperCase()} ${detectedKey.key}`)}{' '}
                    <Link to={`/contribute?${detectedKey.keyType}=${detectedKey.key}`}>{t.home.contributeFirst}</Link>
                  </p>
                ) : (
                  <p>
                    {t.home.searchByKey} <Link to="/contribute">{t.home.contributeAPhoto}</Link>.
                  </p>
                )}
              </>
            ) : (
              <p>
                {t.home.noPhotoAtAll} <Link to="/contribute">{t.home.contributeFirst}</Link>
              </p>
            )}
          </Empty>
        )}
        {vessels && vessels.length > 0 && (
          <Grid>
            {vessels.map((vessel, index) => (
              <VesselCard key={vessel.cfr ?? vessel.mmsi} index={index} vessel={vessel} />
            ))}
          </Grid>
        )}
      </Results>

      <WaveDivider />
      <ApiBand>
        <ApiBandInner>
          <div>
            <h2>{t.home.apiTitle}</h2>
            <p>{t.home.apiLead}</p>
            <PrimaryButton as={Link} to="/docs">
              {t.home.apiButton}
            </PrimaryButton>
          </div>
          <pre>
            {`<img src="${window.location.origin}/v1/cfr/FRA000123456.jpg">\n\n<img src="${window.location.origin}/v1/mmsi/227000101.jpg">`}
          </pre>
        </ApiBandInner>
      </ApiBand>

      <Features>
        <div>
          <h3>{t.home.contributeTitle}</h3>
          <p>{t.home.contributeText}</p>
          <Link to="/contribute">{t.home.contributeLink}</Link>
        </div>
        <div>
          <h3>{t.home.openTitle}</h3>
          <p>{t.home.openText}</p>
          <Link to="/docs#licences">{t.home.openLink}</Link>
        </div>
        <div>
          <h3>{t.home.datasetTitle}</h3>
          <p>{t.home.datasetText}</p>
          <a href="/v1/export.json">{t.home.datasetLink}</a>
        </div>
      </Features>
    </>
  )
}

/** True on phone-width screens, where the full search placeholder would be cut. */
function useIsNarrow(): boolean {
  const [isNarrow, setIsNarrow] = useState(() => window.matchMedia(NARROW_QUERY).matches)
  useEffect(() => {
    const query = window.matchMedia(NARROW_QUERY)
    const update = () => setIsNarrow(query.matches)
    query.addEventListener('change', update)

    return () => query.removeEventListener('change', update)
  }, [])

  return isNarrow
}

const NARROW_QUERY = '(max-width: 600px)'

const Hero = styled(Container).attrs({ as: 'section' })`
  padding-top: 56px;
  padding-bottom: 64px;
  /* Lets the clicks through to the vessels drawn on the chart */
  pointer-events: none;

  @media (max-width: 600px) {
    padding-top: 24px;
    padding-bottom: 24px;
  }
`

const HeroCard = styled(ChartCard)`
  max-width: 780px;
  padding: 32px;
  gap: 20px;
  pointer-events: auto;

  @media (max-width: 600px) {
    padding: 20px;
  }

  h1 {
    margin: 0;
    font-family: ${FONTS.display};
    font-weight: 700;
    font-size: clamp(36px, 6vw, 68px);
    line-height: 1.02;
    max-width: 900px;
  }

  p {
    margin: 0;
    font-size: 18px;
    line-height: 1.5;
    color: ${CHART_COLORS.text};
    max-width: 640px;
  }

  > span {
    color: ${CHART_COLORS.textMuted};
  }
`

const Chips = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
`

const SearchForm = styled.form`
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  max-width: 760px;
  margin-top: 8px;

  input {
    flex: 1 1 320px;
    min-width: 0;
    height: 56px;
    padding: 0 18px;
    border: 2px solid ${COLORS.ink};
    border-radius: 12px;
    font: inherit;
    font-size: 16px;
    background: ${COLORS.white};
  }

  button {
    height: 56px;
    padding: 0 26px;
    font-size: 16px;
  }
`

const ExampleButton = styled.button`
  border: none;
  background: none;
  padding: 0;
  font: inherit;
  color: ${COLORS.link};
  text-decoration: underline;
  cursor: pointer;
`

const Results = styled(Container).attrs({ as: 'section' })`
  padding-top: 48px;
  padding-bottom: 48px;
  display: flex;
  flex-direction: column;
  gap: 16px;
`

const ResultsHeader = styled.div`
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: baseline;
  gap: 8px;
`

const Empty = styled.div`
  color: ${COLORS.text};

  p {
    margin: 0 0 6px;
  }
`

const Grid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 18px;
`

const ApiBand = styled.section`
  background: ${COLORS.ink};
  color: ${COLORS.codeText};
`

const ApiBandInner = styled(Container)`
  padding-top: 48px;
  padding-bottom: 48px;
  display: flex;
  flex-wrap: wrap;
  gap: 32px;
  align-items: center;

  > div {
    flex: 1 1 340px;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  h2 {
    margin: 0;
    font-family: ${FONTS.display};
    font-size: 32px;
    color: ${COLORS.white};
  }

  p {
    margin: 0;
    font-size: 16px;
    line-height: 1.5;
    color: ${COLORS.inkSoft};
  }

  a {
    align-self: flex-start;
    border-radius: 22px;
  }

  pre {
    flex: 1 1 480px;
    min-width: 0;
    margin: 0;
    background: ${COLORS.codeBackground};
    border-radius: 12px;
    padding: 18px;
    font-family: ${FONTS.mono};
    font-size: 13px;
    line-height: 1.7;
    overflow-x: auto;
  }
`

const Features = styled(Container).attrs({ as: 'section' })`
  padding-top: 48px;
  padding-bottom: 48px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 18px;

  > div {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  h3 {
    margin: 0;
    font-family: ${FONTS.display};
    font-size: 22px;
  }

  p {
    margin: 0;
    color: ${COLORS.text};
    line-height: 1.5;
  }

  a {
    align-self: flex-start;
    padding: 12px 0;
    font-weight: 500;
  }
`
