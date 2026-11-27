import type { ReactNode } from 'react'
import styled from 'styled-components'

import { useTranslation } from '../../i18n'
import { COLORS, FONTS, RADIUS } from '../../theme'
import { type AdminEffect, type AdminSectionId, getDependents, getSection, sectionAnchor } from './adminSections'

/** The colour of each kind of effect: the destructive ones stand out */
const EFFECT_COLORS: Record<AdminEffect, { background: string; text: string }> = {
  completes: { background: '#E6F2EA', text: '#1D5B34' },
  decides: { background: COLORS.info, text: COLORS.link },
  publishes: { background: COLORS.accentSoft, text: '#8A3A12' },
  readOnly: { background: COLORS.divider, text: COLORS.muted },
  recomputes: { background: COLORS.info, text: COLORS.link },
  replacesAll: { background: '#FBE9E4', text: COLORS.reject },
  updatesListed: { background: COLORS.info, text: COLORS.link }
}

type Props = {
  children: ReactNode
  /** What the action does; some sections describe themselves */
  description?: ReactNode
  id: AdminSectionId
  /** Its number in the recommended order */
  step?: number | undefined
}

/**
 * An admin action with what it changes: its kind of effect, the public pages it changes, the sections it relies on,
 * and the sections to run again after it. All come from [ADMIN_SECTIONS], so that they stay consistent.
 */
export function AdminSection({ children, description, id, step }: Props) {
  const { t } = useTranslation()
  const section = getSection(id)
  const dependents = getDependents(id)
  const toRunAgain = dependents.filter(it => it.isJob)
  const affected = dependents.filter(it => !it.isJob)
  const colors = EFFECT_COLORS[section.effect]

  return (
    <Frame aria-labelledby={`${sectionAnchor(id)}-title`} id={sectionAnchor(id)}>
      <Head>
        {step !== undefined && <Step aria-hidden>{step}</Step>}
        <h2 id={`${sectionAnchor(id)}-title`}>{t.admin.sections[id]}</h2>
        <Effect style={{ background: colors.background, color: colors.text }}>{t.admin.effects[section.effect]}</Effect>
      </Head>
      {description && <Description>{description}</Description>}
      <Relations>
        {section.shownOn.length > 0 && (
          <div>
            <dt>{t.admin.shownOn}</dt>
            <dd>{section.shownOn.map(area => t.admin.siteAreas[area]).join(', ')}</dd>
          </div>
        )}
        {section.uses.length > 0 && (
          <div>
            <dt>{t.admin.uses}</dt>
            <dd>
              <SectionLinks ids={section.uses} />
            </dd>
          </div>
        )}
        {toRunAgain.length > 0 && (
          <div>
            <dt>{t.admin.runAgainAfter}</dt>
            <dd>
              <SectionLinks ids={toRunAgain.map(it => it.id)} />
            </dd>
          </div>
        )}
        {affected.length > 0 && (
          <div>
            <dt>{t.admin.alsoAffects}</dt>
            <dd>
              <SectionLinks ids={affected.map(it => it.id)} />
            </dd>
          </div>
        )}
      </Relations>
      <Body>{children}</Body>
    </Frame>
  )
}

function SectionLinks({ ids }: { ids: AdminSectionId[] }) {
  const { t } = useTranslation()

  return (
    <>
      {ids.map((id, index) => (
        <span key={id}>
          {index > 0 && ', '}
          <a href={`#${sectionAnchor(id)}`}>{t.admin.sections[id]}</a>
        </span>
      ))}
    </>
  )
}

const Frame = styled.section`
  scroll-margin-top: 16px;
  padding: 20px 0 24px;
  border-top: 1px solid ${COLORS.border};

  /* The sections render their own layout: their former spacing is not needed here */
  > div > section {
    margin-top: 0;
    max-width: none;
  }

  &:target h2 {
    text-decoration: underline;
    text-decoration-color: ${COLORS.accent};
    text-underline-offset: 4px;
  }
`

const Head = styled.div`
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 6px 10px;

  h2 {
    margin: 0;
    color: ${COLORS.ink};
    font-family: ${FONTS.display};
    font-size: 22px;
  }
`

const Step = styled.span`
  color: ${COLORS.muted};
  font-family: ${FONTS.display};
  font-size: 22px;
  font-variant-numeric: tabular-nums;
`

const Effect = styled.span`
  padding: 1px 7px;
  border-radius: ${RADIUS};
  font-size: 12px;
  font-weight: 600;
`

const Description = styled.p`
  max-width: 760px;
  margin: 8px 0 0;
  color: ${COLORS.text};
  font-size: 14px;
  line-height: 1.5;
`

/** What the action is linked to, as a small table */
const Relations = styled.dl`
  display: grid;
  gap: 3px;
  max-width: 760px;
  margin: 10px 0 0;
  font-size: 13px;

  div {
    display: grid;
    grid-template-columns: 170px 1fr;
    gap: 8px;
  }

  dt {
    color: ${COLORS.muted};
  }

  dd {
    margin: 0;
    color: ${COLORS.ink};
  }

  a {
    color: ${COLORS.link};
  }

  @media (max-width: 560px) {
    div {
      grid-template-columns: 1fr;
      gap: 0;
    }
  }
`

const Body = styled.div`
  max-width: 760px;
  margin-top: 14px;
`
