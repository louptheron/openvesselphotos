import { Link } from 'react-router-dom'
import styled from 'styled-components'

import type { VesselSummary } from '../api/types'
import { useTranslation } from '../i18n'
import { COLORS, FONTS, SKIES, getSkyAndSea, RADIUS } from '../theme'
import { getNamedPhotoUrl, getPhotoSlug, getVesselPath } from '../utils/vesselKey'
import { BoatPlaceholder } from './Logo'
import { Skeleton } from './ui'

export function VesselCard({ index, vessel }: { index: number; vessel: VesselSummary }) {
  const { t } = useTranslation()
  const photo = vessel.mainPhoto
  const name = vessel.name ?? t.common.unnamedVessel
  // Named URLs, as on the vessel page: search engines read the words of an image's URL
  const slug = getPhotoSlug(vessel)

  return (
    <Article>
      <Picture $sky={getSkyAndSea(SKIES[index % SKIES.length] ?? COLORS.info)}>
        {photo ? (
          <img
            alt={t.vesselCard.photoAlt(
              vessel.registration ? `${name} (${vessel.registration})` : name,
              vessel.port,
              photo.credit
            )}
            loading="lazy"
            sizes="(max-width: 600px) 100vw, 320px"
            src={getNamedPhotoUrl(photo.urls, slug, '320')}
            srcSet={`${getNamedPhotoUrl(photo.urls, slug, '320')} 320w, ${getNamedPhotoUrl(photo.urls, slug, '800')} 800w`}
          />
        ) : (
          <BoatPlaceholder />
        )}
      </Picture>
      <Body>
        <h3>
          <Link to={getVesselPath(vessel)}>{vessel.name ?? t.common.unnamedVessel}</Link>
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
        {photo ? (
          <Credit>
            <CreditName title={`© ${photo.credit}`}>© {photo.credit}</CreditName>
            <LicenceBadge>{photo.licenceName}</LicenceBadge>
          </Credit>
        ) : (
          <Credit>
            <span>{[vessel.registration, vessel.port].filter(Boolean).join(' · ')}</span>
            <span>{t.common.noPhotoYet}</span>
          </Credit>
        )}
      </Body>
    </Article>
  )
}

/** Stands for a vessel card while the vessels load */
export function VesselCardSkeleton() {
  return (
    <Article aria-hidden="true">
      <Skeleton $height="168px" style={{ borderRadius: 0 }} />
      <Body>
        <Skeleton $height="20px" $width="60%" />
        <Skeleton $height="28px" />
        <Skeleton $height="22px" $width="80%" />
      </Body>
    </Article>
  )
}

/** A grid of card skeletons, as many as a page of vessels usually fills */
export function VesselGridSkeleton({ count = 8 }: { count?: number }) {
  return (
    <VesselGrid>
      {Array.from({ length: count }, (_, index) => (
        <VesselCardSkeleton key={index} />
      ))}
    </VesselGrid>
  )
}

/** Vessel cards, as many per row as fit */
export const VesselGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 18px;
`

/** The licence, as small caps text: a fact, not a button */
export const LicenceBadge = styled.span`
  color: ${COLORS.muted};
  font-family: ${FONTS.mono};
  font-size: 11px;
  white-space: nowrap;
`

const Article = styled.article`
  position: relative;
  background: ${COLORS.white};
  border-radius: ${RADIUS};
  overflow: hidden;
  border: 1px solid ${COLORS.cardBorder};
  display: flex;
  flex-direction: column;

  /* The whole card is the link: its edge darkens and the name is underlined (see h3 a:hover), with no motion */
  &:has(a:hover) {
    border-color: ${COLORS.inputBorder};
  }
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
  flex: 1;
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
  font-variant-numeric: tabular-nums;

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
  /* Pushed to the bottom, so that the cards of a row line up */
  margin-top: auto;
`

/** One line, so that every card has the same height: the full credit shows on hover */
const CreditName = styled.span`
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`
