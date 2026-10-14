import { useState } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import { getErrorMessage, useAdminActionMutation, useApproveApiKeyMutation } from '../../api/api'
import type { AdminPhoto, AdminVessel, ApprovedApiKey, ReviewQueue } from '../../api/types'
import { Card, ErrorMessage, Field, Input, Label, Muted, Pill, Select } from '../../components/ui'
import { COLORS, FONTS } from '../../theme'
import { useTranslation } from '../../i18n'
import { getVesselPath } from '../../utils/vesselKey'
import { ActiveApiKeys, ApprovedApiKeyBanner } from './ApiKeys'

type Tab = 'apiKeys' | 'keyLinks' | 'photos' | 'reports'

const TABS: Tab[] = ['photos', 'keyLinks', 'reports', 'apiKeys']

type Item = {
  id: string
  photo?: AdminPhoto
  render: () => JSX.Element
  subtitle: string
  title: string
}

export function ReviewQueuePanel({ queue }: { queue: ReviewQueue }) {
  const [tab, setTab] = useState<Tab>('photos')
  const [selectedId, setSelectedId] = useState<string>()
  const [runAction, { error, isLoading }] = useAdminActionMutation()
  const [approveApiKey, { error: approveError, isLoading: isApproving }] = useApproveApiKeyMutation()
  const [approvedApiKey, setApprovedApiKey] = useState<ApprovedApiKey>()
  const { formatDate, t } = useTranslation()

  const act = (path: string, body?: object) => runAction(body ? { body, path } : { path })

  const approve = async (id: number) => {
    const result = await approveApiKey(id)
    if (result.data) {
      setApprovedApiKey(result.data)
    }
  }

  const items: Record<Tab, Item[]> = {
    apiKeys: queue.apiKeyRequests.map(request => ({
      id: `api-key-${request.id}`,
      render: () => (
        <Detail>
          <h2>{request.name}</h2>
          <Muted>
            {t.admin.requested(formatDate(request.createdAt))} · <a href={`mailto:${request.email}`}>{request.email}</a>
          </Muted>
          <Facts>
            <div>
              <dt>{t.admin.organisation}</dt>
              <dd>{request.organisation ?? '—'}</dd>
            </div>
          </Facts>
          <Message>{request.intendedUse}</Message>
          <Buttons>
            <Approve disabled={isApproving} onClick={() => approve(request.id)} type="button">
              {t.admin.approveKey}
            </Approve>
            <Reject disabled={isLoading} onClick={() => act(`api-keys/${request.id}/reject`)} type="button">
              {t.common.reject}
            </Reject>
          </Buttons>
        </Detail>
      ),
      subtitle: `${request.organisation ?? request.email} · ${t.admin.apiKeyRequest}`,
      title: request.name
    })),
    keyLinks: queue.keyLinks.map(link => ({
      id: `link-${link.id}`,
      render: () => (
        <Detail>
          <LinkCompare>
            <div>
              <small>{t.admin.knownKey}</small>
              <code>{link.vessel.cfr ? `CFR ${link.vessel.cfr}` : `MMSI ${link.vessel.mmsi}`}</code>
            </div>
            <div>
              <small>{t.admin.suggestedKey}</small>
              <code>
                {link.keyType.toUpperCase()} {link.key}
              </code>
            </div>
          </LinkCompare>
          <VesselHeading vessel={link.vessel} />
          <Muted>{t.admin.suggestedBy(formatDate(link.createdAt), link.contributor ?? t.admin.anonymous)}</Muted>
          <Checklist items={t.admin.linkChecks} />
          <Buttons>
            <Approve disabled={isLoading} onClick={() => act(`links/${link.id}/approve`)} type="button">
              {t.admin.linkKeys}
            </Approve>
            <Reject disabled={isLoading} onClick={() => act(`links/${link.id}/reject`)} type="button">
              {t.common.reject}
            </Reject>
          </Buttons>
        </Detail>
      ),
      subtitle: t.admin.addKey(`${link.keyType.toUpperCase()} ${link.key}`),
      title: link.vessel.name ?? t.common.unnamedVessel
    })),
    photos: queue.photos.map(({ apiKeyName, photo, vessel }) => ({
      id: `photo-${photo.id}`,
      photo,
      render: () => (
        <PhotoReview
          key={photo.id}
          apiKeyName={apiKeyName}
          isLoading={isLoading}
          onAction={act}
          photo={photo}
          vessel={vessel}
        />
      ),
      subtitle: `${vessel ? t.admin.newPhoto : t.admin.firstPhoto} · ${photo.licenceName}`,
      title:
        vessel?.name ?? photo.submittedVesselName ?? `${photo.submittedKeyType.toUpperCase()} ${photo.submittedKey}`
    })),
    reports: queue.reports.map(report => ({
      id: `report-${report.id}`,
      photo: report.photo,
      render: () => (
        <Detail>
          <Preview photo={report.photo} />
          <VesselHeading vessel={report.vessel} />
          <Muted>
            {t.admin.reported(
              formatDate(report.createdAt),
              report.photo.number,
              report.photo.credit,
              report.photo.licenceName
            )}
          </Muted>
          <Facts>
            <div>
              <dt>{t.admin.reason}</dt>
              <dd>{t.admin.reportReasons[report.reason]}</dd>
            </div>
          </Facts>
          {report.message && <Message>{report.message}</Message>}
          <Buttons>
            <Reject disabled={isLoading} onClick={() => act(`reports/${report.id}/remove-photo`)} type="button">
              {t.admin.removePhoto}
            </Reject>
            <Neutral disabled={isLoading} onClick={() => act(`reports/${report.id}/dismiss`)} type="button">
              {t.admin.dismissReport}
            </Neutral>
          </Buttons>
        </Detail>
      ),
      subtitle: t.admin.reportSubtitle(t.admin.reportReasons[report.reason] ?? report.reason, report.photo.number),
      title: report.vessel.name ?? t.common.unnamedVessel
    }))
  }

  const list = items[tab]
  const selected = list.find(item => item.id === selectedId) ?? list[0]

  return (
    <>
      <Tabs aria-label={t.admin.queueLabel} role="tablist">
        {TABS.map(item => (
          <Pill
            key={item}
            $active={tab === item}
            aria-selected={tab === item}
            onClick={() => setTab(item)}
            role="tab"
            type="button"
          >
            {t.admin.tabs[item]} <Count>{items[item].length}</Count>
          </Pill>
        ))}
      </Tabs>
      {error && <ErrorMessage>{getErrorMessage(error, t)}</ErrorMessage>}
      {approveError && <ErrorMessage>{getErrorMessage(approveError, t)}</ErrorMessage>}
      {approvedApiKey && (
        <ApprovedApiKeyBanner approved={approvedApiKey} onClose={() => setApprovedApiKey(undefined)} />
      )}

      {list.length === 0 ? (
        <Empty>{t.admin.nothingToReview}</Empty>
      ) : (
        <Columns>
          <List aria-label={t.admin.pendingItems}>
            {list.map(item => (
              <li key={item.id}>
                <ListButton
                  $active={item.id === selected?.id}
                  aria-current={item.id === selected?.id}
                  onClick={() => setSelectedId(item.id)}
                  type="button"
                >
                  {item.photo && <img alt="" src={`/v1/admin/photos/${item.photo.id}.jpg?size=320`} />}
                  <span>
                    <strong>{item.title}</strong>
                    <small>{item.subtitle}</small>
                  </span>
                </ListButton>
              </li>
            ))}
          </List>
          <Card as="article">{selected?.render()}</Card>
        </Columns>
      )}
      {tab === 'apiKeys' && <ActiveApiKeys isLoading={isLoading} onRevoke={id => act(`api-keys/${id}/revoke`)} />}
    </>
  )
}

function PhotoReview({
  apiKeyName,
  isLoading,
  onAction,
  photo,
  vessel
}: {
  apiKeyName: string | null
  isLoading: boolean
  onAction: (path: string, body?: object) => void
  photo: AdminPhoto
  vessel: AdminVessel | null
}) {
  const { formatDate, t } = useTranslation()
  const [vesselName, setVesselName] = useState(photo.submittedVesselName ?? '')
  const [rejectReason, setRejectReason] = useState(t.admin.rejectReasons[0])

  return (
    <Detail>
      <Preview photo={photo} />
      {vessel ? <VesselHeading vessel={vessel} /> : <h2>{t.admin.newVessel}</h2>}
      <Muted>
        {t.admin.submitted(formatDate(photo.createdAt), photo.credit)}
        {photo.contributorEmail && ` (${photo.contributorEmail})`}
        {apiKeyName && t.admin.sentWithKey(apiKeyName)}
      </Muted>
      <Facts>
        <div>
          <dt>{t.admin.submittedKey}</dt>
          <dd>
            {photo.submittedKeyType.toUpperCase()} {photo.submittedKey}
            {photo.submittedOtherKey &&
              ` · ${photo.submittedKeyType === 'cfr' ? 'MMSI' : 'CFR'} ${photo.submittedOtherKey}`}
          </dd>
        </div>
        <div>
          <dt>{t.admin.licence}</dt>
          <dd>{photo.licenceName}</dd>
        </div>
        <div>
          <dt>{t.admin.taken}</dt>
          <dd>
            {formatDate(photo.takenOn)}
            {photo.place && `, ${photo.place}`}
          </dd>
        </div>
        <div>
          <dt>{t.admin.size}</dt>
          <dd>
            {photo.width}×{photo.height}
          </dd>
        </div>
      </Facts>
      {!vessel && (
        <Field>
          <Label htmlFor="vessel-name">{t.admin.vesselName}</Label>
          <Input
            id="vessel-name"
            maxLength={100}
            onChange={event => setVesselName(event.target.value)}
            value={vesselName}
          />
        </Field>
      )}
      <Checklist items={t.admin.photoChecks} />
      <Field>
        <Label htmlFor="reject-reason">{t.admin.rejectWhy}</Label>
        <Select id="reject-reason" onChange={event => setRejectReason(event.target.value)} value={rejectReason}>
          {t.admin.rejectReasons.map(reason => (
            <option key={reason}>{reason}</option>
          ))}
        </Select>
      </Field>
      <Buttons>
        <Approve
          disabled={isLoading}
          onClick={() => onAction(`photos/${photo.id}/approve`, { vesselName: vessel ? undefined : vesselName })}
          type="button"
        >
          {t.admin.approvePhoto}
        </Approve>
        <Reject
          disabled={isLoading}
          onClick={() => onAction(`photos/${photo.id}/reject`, { reason: rejectReason })}
          type="button"
        >
          {t.common.reject}
        </Reject>
      </Buttons>
    </Detail>
  )
}

function Preview({ photo }: { photo: AdminPhoto }) {
  const { t } = useTranslation()

  return (
    <a href={`/v1/admin/photos/${photo.id}.jpg?size=full`} rel="noreferrer" target="_blank">
      <PreviewImage alt={t.admin.previewAlt} src={`/v1/admin/photos/${photo.id}.jpg?size=800`} />
    </a>
  )
}

function VesselHeading({ vessel }: { vessel: AdminVessel }) {
  const { t } = useTranslation()

  return (
    <div>
      <h2>
        <Link rel="noreferrer" target="_blank" to={getVesselPath(vessel)}>
          {vessel.name ?? t.common.unnamedVessel}
        </Link>
      </h2>
      <Muted>
        CFR {vessel.cfr ?? '—'} · MMSI {vessel.mmsi ?? '—'}
      </Muted>
    </div>
  )
}

/** A reminder for the reviewer; nothing is saved. */
function Checklist({ items }: { items: string[] }) {
  const { t } = useTranslation()

  return (
    <fieldset>
      <legend>{t.admin.checklist}</legend>
      {items.map(item => (
        <label key={item}>
          <input type="checkbox" />
          <span>{item}</span>
        </label>
      ))}
    </fieldset>
  )
}

const Tabs = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 16px;
`

const Count = styled.span`
  font-family: ${FONTS.mono};
  font-size: 12px;
  padding: 1px 7px;
  margin-left: 6px;
  border-radius: 10px;
  background: rgba(127, 127, 127, 0.2);
`

const Empty = styled.p`
  color: ${COLORS.text};
`

const Columns = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  align-items: flex-start;

  > article {
    flex: 999 1 560px;
    min-width: 0;
    overflow: hidden;
  }
`

const List = styled.ul`
  flex: 1 1 300px;
  min-width: 0;
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 8px;
`

const ListButton = styled.button<{ $active: boolean }>`
  width: 100%;
  text-align: left;
  padding: 12px;
  border-radius: 12px;
  border: 2px solid ${p => (p.$active ? COLORS.accent : COLORS.cardBorder)};
  background: ${COLORS.white};
  font: inherit;
  color: ${COLORS.ink};
  display: flex;
  gap: 12px;
  align-items: center;
  cursor: pointer;

  img {
    flex: 0 0 64px;
    width: 64px;
    height: 44px;
    object-fit: cover;
    border-radius: 8px;
  }

  span {
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;
  }

  small {
    color: ${COLORS.muted};
  }
`

const Detail = styled.div`
  padding: 20px 24px 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;

  h2 {
    margin: 0;
    font-family: ${FONTS.display};
    font-size: 26px;
  }

  h2 a {
    color: ${COLORS.ink};
  }

  fieldset {
    margin: 0;
    padding: 0;
    border: none;
    display: flex;
    flex-direction: column;
    gap: 6px;
  }

  legend {
    font-size: 14px;
    font-weight: 600;
    margin-bottom: 6px;
  }

  fieldset label {
    display: flex;
    gap: 10px;
    align-items: center;
    font-size: 14px;
    min-height: 32px;
    cursor: pointer;
  }

  fieldset input {
    width: 20px;
    height: 20px;
    margin: 0;
    accent-color: ${COLORS.ink};
  }
`

const PreviewImage = styled.img`
  display: block;
  width: 100%;
  max-height: 420px;
  object-fit: contain;
  background: ${COLORS.ground};
  border-radius: 10px;
`

const LinkCompare = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: 14px;

  > div {
    flex: 1 1 200px;
    padding: 14px 16px;
    border-radius: 12px;
    background: ${COLORS.ground};
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  > div:last-child {
    background: ${COLORS.accentSoft};
    border: 1px solid #f2c4a8;
  }

  small {
    font-size: 12px;
    color: ${COLORS.muted};
  }

  code {
    font-family: ${FONTS.mono};
    font-size: 16px;
  }
`

const Facts = styled.dl`
  margin: 0;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 10px;

  > div {
    padding: 10px 12px;
    border-radius: 10px;
    background: ${COLORS.ground};
  }

  dt {
    font-size: 12px;
    color: ${COLORS.muted};
  }

  dd {
    margin: 2px 0 0;
    font-size: 14px;
  }
`

const Message = styled.blockquote`
  margin: 0;
  padding: 10px 14px;
  border-radius: 10px;
  background: ${COLORS.ground};
  font-size: 14px;
  white-space: pre-wrap;
`

const Buttons = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding-top: 12px;
  border-top: 1px solid ${COLORS.divider};
`

const ActionButton = styled.button`
  min-height: 48px;
  padding: 0 22px;
  border-radius: 12px;
  font: inherit;
  font-weight: 600;
  font-size: 15px;
  cursor: pointer;

  &:disabled {
    opacity: 0.55;
  }
`

const Approve = styled(ActionButton)`
  border: none;
  background: ${COLORS.approve};
  color: ${COLORS.white};
`

const Reject = styled(ActionButton)`
  border: 1px solid ${COLORS.reject};
  background: ${COLORS.white};
  color: ${COLORS.reject};
`

const Neutral = styled(ActionButton)`
  border: 1px solid ${COLORS.inputBorder};
  background: ${COLORS.white};
  color: ${COLORS.ink};
`
