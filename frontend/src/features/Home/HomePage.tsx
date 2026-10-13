import { type FormEvent, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import styled from 'styled-components'

import { useGetRecentVesselsQuery, useSearchVesselsQuery } from '../../api/api'
import { VesselCard } from '../../components/VesselCard'
import { Chip, Container, Muted, PrimaryButton, SectionTitle } from '../../components/ui'
import { COLORS, FONTS } from '../../theme'
import { detectKey } from '../../utils/vesselKey'

export function HomePage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const query = searchParams.get('q') ?? ''
  const [input, setInput] = useState(query)
  const navigate = useNavigate()

  const isSearching = query.trim().length >= 2
  const searchResult = useSearchVesselsQuery(query, { skip: !isSearching })
  const recentResult = useGetRecentVesselsQuery(undefined, { skip: isSearching })
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
    setSearchParams({ q: example })
  }

  const detectedKey = isSearching ? detectKey(query) : undefined

  return (
    <>
      <Hero>
        <Chips>
          <Chip>Free for everyone</Chip>
          <Chip>Open-source code</Chip>
          <Chip>Openly licensed photos</Chip>
        </Chips>
        <h1>Find a photo of any vessel, by its CFR or MMSI.</h1>
        <p>
          A simple, community-built library of vessel photos. Search it here or call the free API — every photo comes
          with its credit and licence.
        </p>
        <SearchForm aria-label="Search vessels" onSubmit={search}>
          <label className="visually-hidden" htmlFor="q">
            Search by name, CFR or MMSI
          </label>
          <input
            id="q"
            onChange={event => setInput(event.target.value)}
            placeholder="Name, CFR (FRA000123456) or MMSI (227000101)"
            type="search"
            value={input}
          />
          <PrimaryButton type="submit">Search</PrimaryButton>
        </SearchForm>
        <Muted>
          Try{' '}
          <ExampleButton onClick={() => showExample('FRA')} type="button">
            FRA
          </ExampleButton>{' '}
          ·{' '}
          <ExampleButton onClick={() => showExample('227')} type="button">
            227
          </ExampleButton>
        </Muted>
      </Hero>

      <Results aria-label="Results" aria-live="polite">
        <ResultsHeader>
          <SectionTitle>{isSearching ? `Results for “${query}”` : 'Recently added'}</SectionTitle>
          {isFetching && <Muted>Loading…</Muted>}
        </ResultsHeader>
        {isError && <p>The search is unavailable right now. Please try again later.</p>}
        {vessels && vessels.length === 0 && (
          <Empty>
            {isSearching ? (
              <>
                <p>No vessel matches “{query}”.</p>
                {detectedKey ? (
                  <p>
                    No photo of {detectedKey.keyType.toUpperCase()} {detectedKey.key} yet.{' '}
                    <Link to={`/contribute?${detectedKey.keyType}=${detectedKey.key}`}>Contribute the first one →</Link>
                  </p>
                ) : (
                  <p>
                    Search by CFR or MMSI to be sure, or <Link to="/contribute">contribute a photo</Link>.
                  </p>
                )}
              </>
            ) : (
              <p>
                No photo yet. <Link to="/contribute">Contribute the first one →</Link>
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

      <ApiBand>
        <ApiBandInner>
          <div>
            <h2>One URL per vessel.</h2>
            <p>No key, no account. Drop the image straight into your app, or fetch the JSON for credit and licence.</p>
            <PrimaryButton as={Link} to="/docs">
              Read the API docs
            </PrimaryButton>
          </div>
          <pre>
            {`<img src="${window.location.origin}/v1/cfr/FRA000123456.jpg">\n\n<img src="${window.location.origin}/v1/mmsi/227000101.jpg">`}
          </pre>
        </ApiBandInner>
      </ApiBand>

      <Features>
        <div>
          <h3>Contribute a photo</h3>
          <p>Upload your own picture, pick an open licence, and it goes live once reviewed.</p>
          <Link to="/contribute">Upload a photo →</Link>
        </div>
        <div>
          <h3>Self-host it</h3>
          <p>The whole service is open source. Run your own instance with one container and a PostgreSQL database.</p>
          <Link to="/docs#self-host">How to run it →</Link>
        </div>
        <div>
          <h3>Download everything</h3>
          <p>Full dataset export of vessels, photo URLs, credits and licences, always up to date.</p>
          <a href="/v1/export.json">Open data (JSON) →</a>
        </div>
      </Features>
    </>
  )
}

const Hero = styled(Container).attrs({ as: 'section' })`
  padding-top: 48px;
  padding-bottom: 40px;
  display: flex;
  flex-direction: column;
  gap: 20px;

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
    color: ${COLORS.text};
    max-width: 640px;
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
    font-weight: 500;
  }
`
