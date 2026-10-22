import { type FormEvent, useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import styled from 'styled-components'

import { useGetRecentVesselsQuery, useSearchVesselsQuery } from '../../api/api'
import { ChartCard, ChartSection, WaveDivider } from '../../components/Chart'
import { VesselCard, VesselGrid } from '../../components/VesselCard'
import { CodeBlock, Container, Muted, PrimaryButton, SectionTitle } from '../../components/ui'
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
              <ExampleButton onClick={() => showExample('FRA000864281')} type="button">
                FRA000864281
              </ExampleButton>{' '}
              ·{' '}
              <ExampleButton onClick={() => showExample('226335000')} type="button">
                226335000
              </ExampleButton>{' '}
              ·{' '}
              <ExampleButton onClick={() => showExample('Danielle Casanova')} type="button">
                Danielle Casanova
              </ExampleButton>
            </Muted>
          </HeroCard>
        </Hero>
        {!recentResult.isLoading && <ChartCredits libraryVesselCount={recentResult.data?.length ?? 0} />}
      </ChartSection>

      <Results aria-label={t.home.resultsLabel} aria-live="polite">
        <ResultsHeader>
          <SectionTitle>{isSearching ? t.home.resultsFor(query) : t.home.recentlyAdded}</SectionTitle>
          {isFetching ? (
            <Muted>{t.common.loading}</Muted>
          ) : (
            !isSearching && <Link to="/vessels">{t.home.allVessels}</Link>
          )}
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
                    <Link rel="nofollow" to={`/contribute?${detectedKey.keyType}=${detectedKey.key}`}>
                      {t.home.contributeFirst}
                    </Link>
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
          <VesselGrid>
            {vessels.map((vessel, index) => (
              <VesselCard key={vessel.cfr ?? vessel.mmsi} index={index} vessel={vessel} />
            ))}
          </VesselGrid>
        )}
      </Results>

      <WaveDivider color={COLORS.ink} />
      <AboutBand>
        <About>
          <SectionTitle>{t.home.aboutTitle}</SectionTitle>
          <p>{t.home.aboutText}</p>
          <p>
            {t.home.contributeText} <Link to="/contribute">{t.home.contributeLink}</Link>
            {t.home.contributeEnd}
          </p>
          <p>{t.home.apiText}</p>
          <CodeBlock>
            {`<img src="${window.location.origin}/v1/cfr/FRA000123456.jpg">\n<img src="${window.location.origin}/v1/mmsi/227000101.jpg">`}
          </CodeBlock>
          <p>
            {t.home.apiMore} <Link to="/docs">{t.home.apiLink}</Link>
            {t.home.apiEnd}
          </p>
          <p>
            {t.home.datasetText} <a href="/v1/export.json">{t.home.datasetLink}</a>
            {t.home.datasetEnd}
          </p>
        </About>
      </AboutBand>
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
    border-radius: 6px;
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

const AboutBand = styled.section`
  background: ${COLORS.ink};
  color: ${COLORS.codeText};
  /* Reaches the footer, without a strip of the page background in between */
  margin-bottom: -24px;
`

const About = styled(Container)`
  padding-top: 32px;
  padding-bottom: 56px;
  display: flex;
  flex-direction: column;
  gap: 14px;

  > * {
    max-width: 720px;
  }

  h2 {
    color: ${COLORS.white};
  }

  p {
    margin: 0;
    color: ${COLORS.inkSoft};
    line-height: 1.6;
  }

  a,
  a:hover {
    color: ${COLORS.white};
  }

  pre {
    background: ${COLORS.codeBackground};
  }
`
