import { useState } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import { getErrorMessage, useAdminActionMutation } from '../../api/api'
import type { AdminPhoto, AdminVessel, ReviewQueue } from '../../api/types'
import { Card, ErrorMessage, Field, Input, Label, Muted, Pill, Select } from '../../components/ui'
import { COLORS, FONTS } from '../../theme'
import { getVesselPath } from '../../utils/vesselKey'

type Tab = 'keyLinks' | 'photos' | 'reports'

const TABS: Array<{ label: string; value: Tab }> = [
  { label: 'Photos', value: 'photos' },
  { label: 'Key links', value: 'keyLinks' },
  { label: 'Reports', value: 'reports' }
]

const REJECT_REASONS = [
  "Vessel doesn't match the key",
  "Not the contributor's own photo",
  'Poor quality or vessel not visible',
  'Duplicate of an existing photo',
  'Other'
]

const REPORT_REASONS: Record<string, string> = {
  COPYRIGHT: 'Copyright / licence',
  OTHER: 'Other',
  PRIVACY: 'People or private details',
  WRONG_VESSEL: 'Wrong vessel'
}

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

  const act = (path: string, body?: object) => runAction({ body, path })

  const items: Record<Tab, Item[]> = {
    keyLinks: queue.keyLinks.map(link => ({
      id: `link-${link.id}`,
      render: () => (
        <Detail>
          <LinkCompare>
            <div>
              <small>Known key</small>
              <code>{link.vessel.cfr ? `CFR ${link.vessel.cfr}` : `MMSI ${link.vessel.mmsi}`}</code>
            </div>
            <div>
              <small>Suggested key</small>
              <code>
                {link.keyType.toUpperCase()} {link.key}
              </code>
            </div>
          </LinkCompare>
          <VesselHeading vessel={link.vessel} />
          <Muted>
            Suggested {formatDate(link.createdAt)} by {link.contributor ?? 'anonymous'}
          </Muted>
          <Checklist items={['Both keys found in a public register', 'Same vessel name and flag']} />
          <Buttons>
            <Approve disabled={isLoading} onClick={() => act(`links/${link.id}/approve`)} type="button">
              Link the keys
            </Approve>
            <Reject disabled={isLoading} onClick={() => act(`links/${link.id}/reject`)} type="button">
              Reject
            </Reject>
          </Buttons>
        </Detail>
      ),
      subtitle: `Add ${link.keyType.toUpperCase()} ${link.key}`,
      title: link.vessel.name ?? 'Unnamed vessel'
    })),
    photos: queue.photos.map(({ photo, vessel }) => ({
      id: `photo-${photo.id}`,
      photo,
      render: () => <PhotoReview key={photo.id} isLoading={isLoading} onAction={act} photo={photo} vessel={vessel} />,
      subtitle: `${vessel ? 'New photo' : 'First photo · creates vessel'} · ${photo.licenceName}`,
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
            Reported {formatDate(report.createdAt)} · photo {report.photo.number} by {report.photo.credit} (
            {report.photo.licenceName})
          </Muted>
          <Facts>
            <div>
              <dt>Reason</dt>
              <dd>{REPORT_REASONS[report.reason]}</dd>
            </div>
          </Facts>
          {report.message && <Message>{report.message}</Message>}
          <Buttons>
            <Reject disabled={isLoading} onClick={() => act(`reports/${report.id}/remove-photo`)} type="button">
              Remove the photo
            </Reject>
            <Neutral disabled={isLoading} onClick={() => act(`reports/${report.id}/dismiss`)} type="button">
              Dismiss the report
            </Neutral>
          </Buttons>
        </Detail>
      ),
      subtitle: `Report · ${REPORT_REASONS[report.reason]} · photo ${report.photo.number}`,
      title: report.vessel.name ?? 'Unnamed vessel'
    }))
  }

  const list = items[tab]
  const selected = list.find(item => item.id === selectedId) ?? list[0]

  return (
    <>
      <Tabs aria-label="Queue" role="tablist">
        {TABS.map(item => (
          <Pill
            key={item.value}
            $active={tab === item.value}
            aria-selected={tab === item.value}
            onClick={() => setTab(item.value)}
            role="tab"
            type="button"
          >
            {item.label} <Count>{items[item.value].length}</Count>
          </Pill>
        ))}
      </Tabs>
      {error && <ErrorMessage>{getErrorMessage(error)}</ErrorMessage>}

      {list.length === 0 ? (
        <Empty>Nothing to review here.</Empty>
      ) : (
        <Columns>
          <List aria-label="Pending items">
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
    </>
  )
}

function PhotoReview({
  isLoading,
  onAction,
  photo,
  vessel
}: {
  isLoading: boolean
  onAction: (path: string, body?: object) => void
  photo: AdminPhoto
  vessel: AdminVessel | null
}) {
  const [vesselName, setVesselName] = useState(photo.submittedVesselName ?? '')
  const [rejectReason, setRejectReason] = useState(REJECT_REASONS[0])

  return (
    <Detail>
      <Preview photo={photo} />
      {vessel ? <VesselHeading vessel={vessel} /> : <h2>New vessel</h2>}
      <Muted>
        Submitted {formatDate(photo.createdAt)} by {photo.credit}
        {photo.contributorEmail && ` (${photo.contributorEmail})`}
      </Muted>
      <Facts>
        <div>
          <dt>Submitted key</dt>
          <dd>
            {photo.submittedKeyType.toUpperCase()} {photo.submittedKey}
          </dd>
        </div>
        <div>
          <dt>Licence</dt>
          <dd>{photo.licenceName}</dd>
        </div>
        <div>
          <dt>Taken</dt>
          <dd>
            {photo.takenOn ?? '—'}
            {photo.place && `, ${photo.place}`}
          </dd>
        </div>
        <div>
          <dt>Size</dt>
          <dd>
            {photo.width}×{photo.height}
          </dd>
        </div>
      </Facts>
      {!vessel && (
        <Field>
          <Label htmlFor="vessel-name">Vessel name</Label>
          <Input
            id="vessel-name"
            maxLength={100}
            onChange={event => setVesselName(event.target.value)}
            value={vesselName}
          />
        </Field>
      )}
      <Checklist
        items={[
          'Vessel in the photo matches the key',
          'Contributor plausibly took it themselves',
          'No close-up faces or private details',
          'Not a duplicate'
        ]}
      />
      <Field>
        <Label htmlFor="reject-reason">If rejecting, why?</Label>
        <Select id="reject-reason" onChange={event => setRejectReason(event.target.value)} value={rejectReason}>
          {REJECT_REASONS.map(reason => (
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
          Approve and publish
        </Approve>
        <Reject
          disabled={isLoading}
          onClick={() => onAction(`photos/${photo.id}/reject`, { reason: rejectReason })}
          type="button"
        >
          Reject
        </Reject>
      </Buttons>
    </Detail>
  )
}

function Preview({ photo }: { photo: AdminPhoto }) {
  return (
    <a href={`/v1/admin/photos/${photo.id}.jpg?size=full`} rel="noreferrer" target="_blank">
      <PreviewImage alt="Submitted photo" src={`/v1/admin/photos/${photo.id}.jpg?size=800`} />
    </a>
  )
}

function VesselHeading({ vessel }: { vessel: AdminVessel }) {
  return (
    <div>
      <h2>
        <Link rel="noreferrer" target="_blank" to={getVesselPath(vessel)}>
          {vessel.name ?? 'Unnamed vessel'}
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
  return (
    <fieldset>
      <legend>Checklist</legend>
      {items.map(item => (
        <label key={item}>
          <input type="checkbox" />
          <span>{item}</span>
        </label>
      ))}
    </fieldset>
  )
}

function formatDate(value: string | null): string {
  return value ? new Date(value).toLocaleDateString() : '—'
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
