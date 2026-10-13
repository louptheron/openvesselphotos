import { Link } from 'react-router-dom'
import styled from 'styled-components'

import type { VesselSummary } from '../api/types'
import { COLORS, FONTS, SKIES } from '../theme'
import { getPhotoUrl, getVesselPath } from '../utils/vesselKey'
import { BoatPlaceholder } from './Logo'

export function VesselCard({ index, vessel }: { index: number; vessel: VesselSummary }) {
  const photo = vessel.mainPhoto

  return (
    <Article>
      <Picture $sky={SKIES[index % SKIES.length] ?? COLORS.info}>
        {photo ? (
          <img
            alt={`${vessel.name ?? 'Vessel'}, photo by ${photo.credit}`}
            loading="lazy"
            sizes="(max-width: 600px) 100vw, 320px"
            src={getPhotoUrl(photo.urls, '320')}
            srcSet={`${getPhotoUrl(photo.urls, '320')} 320w, ${getPhotoUrl(photo.urls, '800')} 800w`}
          />
        ) : (
          <BoatPlaceholder />
        )}
      </Picture>
      <Body>
        <h3>
          <Link to={getVesselPath(vessel)}>{vessel.name ?? 'Unnamed vessel'}</Link>
        </h3>
        <Keys>
          <div>
            <dt>CFR</dt>
            <dd>{vessel.cfr ?? '—'}</dd>
          </div>
          <div>
            <dt>MMSI</dt>
            <dd>{vessel.mmsi ?? '—'}</dd>
          </div>
        </Keys>
        {photo && (
          <Credit>
            <span>© {photo.credit}</span>
            <LicenceBadge>{photo.licenceName}</LicenceBadge>
          </Credit>
        )}
      </Body>
    </Article>
  )
}

export const LicenceBadge = styled.span`
  padding: 3px 8px;
  border-radius: 10px;
  background: ${COLORS.info};
  color: ${COLORS.link};
  font-weight: 500;
  white-space: nowrap;
`

const Article = styled.article`
  position: relative;
  background: ${COLORS.white};
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid ${COLORS.cardBorder};
  display: flex;
  flex-direction: column;
`

const Picture = styled.div<{ $sky: string }>`
  height: 168px;
  background: ${p => p.$sky};
  display: flex;
  align-items: flex-end;
  justify-content: center;
  padding-bottom: 18px;

  &:has(img) {
    padding: 0;
  }

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
`

const Body = styled.div`
  padding: 14px 16px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;

  h3 {
    margin: 0;
    font-size: 18px;
    font-weight: 600;
  }

  h3 a {
    color: ${COLORS.ink};
    text-decoration: none;
  }

  /* The whole card is clickable */
  h3 a::after {
    content: '';
    position: absolute;
    inset: 0;
  }

  h3 a:hover {
    text-decoration: underline;
  }
`

const Keys = styled.dl`
  margin: 0;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px;
  font-family: ${FONTS.mono};
  font-size: 12px;

  dt {
    color: ${COLORS.muted};
    font-family: ${FONTS.sans};
  }

  dd {
    margin: 0;
  }
`

const Credit = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: ${COLORS.text};
  border-top: 1px solid ${COLORS.divider};
  padding-top: 10px;
`
