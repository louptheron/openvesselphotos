import { type FormEvent, type ReactNode, useState } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import {
  getErrorMessage,
  getErrorStatus,
  useGetReviewQueueQuery,
  useLoginMutation,
  useLogoutMutation
} from '../../api/api'
import {
  Card,
  Container,
  ErrorMessage,
  Field,
  Input,
  Label,
  PrimaryButton,
  TextButton,
  Title
} from '../../components/ui'
import { usePageMeta } from '../../hooks/usePageMeta'
import { useTranslation } from '../../i18n'
import { COLORS, FONTS } from '../../theme'
import { AdminSection } from './AdminSection'
import { ADMIN_GROUPS, ADMIN_SECTIONS, type AdminSectionId, sectionAnchor } from './adminSections'
import { FishingActivitySync } from './FishingActivitySync'
import { HomePortsCheck } from './HomePortsCheck'
import { HomePortsFromPortVisits } from './HomePortsFromPortVisits'
import { HomePortsFromRegistration } from './HomePortsFromRegistration'
import { PhotoImport } from './PhotoImport'
import { PhotoNotes } from './PhotoNotes'
import { AuctionSalesImport, HomePortsImport, PortPositionsImport, VesselOwnersImport } from './PortsSiteImports'
import { ReviewQueuePanel } from './ReviewQueuePanel'
import { VesselImport } from './VesselImport'
import { WikidataImport } from './WikidataImport'

export function AdminPage() {
  const { data: queue, error, isLoading } = useGetReviewQueueQuery()
  const [logout] = useLogoutMutation()
  const { t } = useTranslation()
  usePageMeta(t.admin.pageTitle)

  if (isLoading) {
    return <Container>{t.common.loading}</Container>
  }
  if (getErrorStatus(error) === 401) {
    return <LoginForm />
  }
  if (!queue) {
    return (
      <Container>
        <ErrorMessage>{getErrorMessage(error, t)}</ErrorMessage>
      </Container>
    )
  }

  const pendingCount = queue.photos.length + queue.keyLinks.length + queue.reports.length + queue.apiKeyRequests.length
  const content: Record<AdminSectionId, { body: ReactNode; description?: ReactNode }> = {
    auctionSales: { body: <AuctionSalesImport />, description: t.admin.auctionSalesHelp },
    fishingGrounds: { body: <FishingActivitySync />, description: t.admin.fishingActivityText },
    homePorts: { body: <HomePortsImport />, description: t.admin.homePortsText },
    homePortsCheck: { body: <HomePortsCheck />, description: t.admin.homePortsCheckText },
    homePortsFromVisits: { body: <HomePortsFromPortVisits />, description: t.admin.homePortsFromPortVisitsText },
    homePortsFromRegistration: {
      body: <HomePortsFromRegistration />,
      description: t.admin.homePortsFromRegistrationText
    },
    owners: { body: <VesselOwnersImport />, description: t.admin.vesselOwnersText },
    photoNotes: { body: <PhotoNotes />, description: t.admin.photoNotesText },
    photos: { body: <PhotoImport />, description: t.admin.photoImportText },
    portPositions: { body: <PortPositionsImport />, description: t.admin.portPositionsText },
    review: { body: <ReviewQueuePanel queue={queue} /> },
    vessels: { body: <VesselImport /> },
    wikidata: { body: <WikidataImport />, description: t.admin.wikidataImportText }
  }

  return (
    <Page>
      <Header>
        <Title as="h1">{t.admin.adminTitle}</Title>
        <HeaderLinks>
          <Link to="/admin/stats">{t.admin.statistics}</Link>
          <TextButton onClick={() => logout()} type="button">
            {t.admin.signOut}
          </TextButton>
        </HeaderLinks>
      </Header>
      <Layout>
        <Contents aria-label={t.admin.contents}>
          {ADMIN_GROUPS.map(group => (
            <div key={group}>
              <strong>{t.admin.groups[group].title}</strong>
              <ol>
                {ADMIN_SECTIONS.filter(section => section.group === group).map(section => (
                  <li key={section.id}>
                    <a href={`#${sectionAnchor(section.id)}`}>{t.admin.sections[section.id]}</a>
                    {section.id === 'review' && pendingCount > 0 && <Count>{pendingCount}</Count>}
                  </li>
                ))}
              </ol>
            </div>
          ))}
        </Contents>
        <Groups>
          {ADMIN_GROUPS.map(group => {
            const sections = ADMIN_SECTIONS.filter(section => section.group === group)

            return (
              <Group aria-labelledby={`admin-group-${group}`} key={group}>
                <GroupHead>
                  <h2 id={`admin-group-${group}`}>{t.admin.groups[group].title}</h2>
                  <p>{t.admin.groups[group].text}</p>
                </GroupHead>
                {sections.map((section, index) => (
                  <AdminSection
                    description={content[section.id].description}
                    id={section.id}
                    key={section.id}
                    step={sections.length > 1 ? index + 1 : undefined}
                  >
                    {content[section.id].body}
                  </AdminSection>
                ))}
              </Group>
            )
          })}
        </Groups>
      </Layout>
    </Page>
  )
}

export function LoginForm() {
  const [password, setPassword] = useState('')
  const [login, { error, isLoading }] = useLoginMutation()
  const { t } = useTranslation()

  const submit = (event: FormEvent) => {
    event.preventDefault()
    login(password)
  }

  return (
    <Page>
      <LoginCard as="form" onSubmit={submit}>
        <Title as="h1">{t.admin.loginTitle}</Title>
        <p>{t.admin.loginText}</p>
        <Field>
          <Label htmlFor="password">{t.admin.password}</Label>
          <Input
            autoComplete="current-password"
            id="password"
            onChange={event => setPassword(event.target.value)}
            type="password"
            value={password}
          />
        </Field>
        {error && <ErrorMessage>{getErrorMessage(error, t)}</ErrorMessage>}
        <PrimaryButton disabled={!password || isLoading} type="submit">
          {t.admin.signIn}
        </PrimaryButton>
      </LoginCard>
    </Page>
  )
}

const Page = styled(Container)`
  padding-top: 24px;
  padding-bottom: 48px;
`

const Header = styled.div`
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;

  h1 {
    font-size: 36px;
  }
`

const Layout = styled.div`
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr);
  align-items: start;
  gap: 40px;

  @media (max-width: 900px) {
    grid-template-columns: 1fr;
    gap: 16px;
  }
`

/** The sections, in the order to run them: follows the page on wide screens */
const Contents = styled.nav`
  position: sticky;
  top: 16px;
  display: grid;
  gap: 18px;
  max-height: calc(100vh - 32px);
  overflow: auto;
  font-size: 13px;

  strong {
    display: block;
    margin-bottom: 4px;
    color: ${COLORS.ink};
  }

  ol {
    margin: 0;
    padding-left: 20px;
    color: ${COLORS.muted};
  }

  li {
    padding: 2px 0;
  }

  a {
    color: ${COLORS.link};
    text-decoration: none;
  }

  a:hover {
    text-decoration: underline;
  }

  @media (max-width: 900px) {
    position: static;
    max-height: none;
    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
    padding: 12px 0;
    border-top: 1px solid ${COLORS.border};
  }
`

const Count = styled.span`
  margin-left: 6px;
  padding: 0 6px;
  border-radius: 3px;
  background: ${COLORS.accent};
  color: ${COLORS.white};
  font-size: 11px;
  font-weight: 600;
`

const Groups = styled.div`
  display: grid;
  gap: 48px;
`

const Group = styled.section``

const GroupHead = styled.div`
  padding-bottom: 12px;
  border-bottom: 2px solid ${COLORS.ink};

  h2 {
    margin: 0;
    font-family: ${FONTS.display};
    font-size: 28px;
  }

  p {
    max-width: 760px;
    margin: 6px 0 0;
    color: ${COLORS.text};
    font-size: 14px;
    line-height: 1.5;
  }
`

const HeaderLinks = styled.div`
  display: flex;
  align-items: center;
  gap: 12px;
`

const LoginCard = styled(Card)`
  max-width: 420px;
  margin: 24px auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 14px;

  h1 {
    font-size: 32px;
  }

  p {
    margin: 0;
  }
`
