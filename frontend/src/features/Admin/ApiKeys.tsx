import { useState } from 'react'
import styled from 'styled-components'

import { useGetActiveApiKeysQuery } from '../../api/api'
import type { ApprovedApiKey } from '../../api/types'
import { Card, Muted, PrimaryButton, SecondaryButton, SectionTitle, TextButton } from '../../components/ui'
import { COLORS, FONTS } from '../../theme'
import { useTranslation } from '../../i18n'

/** Shows a key once, right after its approval: only its hash is stored. */
export function ApprovedApiKeyBanner({ approved, onClose }: { approved: ApprovedApiKey; onClose: () => void }) {
  const [isCopied, setIsCopied] = useState(false)
  const { t } = useTranslation()
  const { apiKey, key } = approved
  const docsUrl = `${window.location.origin}/docs#api-key`
  const mailto =
    `mailto:${encodeURIComponent(apiKey.email)}` +
    `?subject=${encodeURIComponent(t.admin.emailSubject)}` +
    `&body=${encodeURIComponent(t.admin.emailBody(apiKey.name, key, docsUrl))}`

  const copy = async () => {
    await navigator.clipboard.writeText(key)
    setIsCopied(true)
  }

  return (
    <Banner>
      <strong>{t.admin.keyCreatedFor(apiKey.name)}</strong>
      <span>{t.admin.sendItNow}</span>
      <code>{key}</code>
      <div>
        <PrimaryButton as="a" href={mailto}>
          {t.admin.emailIt(apiKey.email)}
        </PrimaryButton>
        <SecondaryButton onClick={copy} type="button">
          {isCopied ? t.common.copied : t.admin.copyKey}
        </SecondaryButton>
        <TextButton onClick={onClose} type="button">
          {t.admin.done}
        </TextButton>
      </div>
    </Banner>
  )
}

export function ActiveApiKeys({ isLoading, onRevoke }: { isLoading: boolean; onRevoke: (id: number) => void }) {
  const { data: apiKeys } = useGetActiveApiKeysQuery()
  const { formatDate, t } = useTranslation()

  return (
    <Section>
      <SectionTitle as="h2">{t.admin.activeKeys}</SectionTitle>
      {apiKeys?.length === 0 && <Muted>{t.admin.noActiveKey}</Muted>}
      {apiKeys && apiKeys.length > 0 && (
        <TableBox>
          <table>
            <thead>
              <tr>
                <th scope="col">{t.admin.keyColumns.name}</th>
                <th scope="col">{t.admin.keyColumns.email}</th>
                <th scope="col">{t.admin.keyColumns.key}</th>
                <th scope="col">{t.admin.keyColumns.created}</th>
                <th scope="col">{t.admin.keyColumns.lastUsed}</th>
                <th scope="col">
                  <span className="visually-hidden">{t.admin.keyColumns.actions}</span>
                </th>
              </tr>
            </thead>
            <tbody>
              {apiKeys.map(apiKey => (
                <tr key={apiKey.id}>
                  <td>
                    {apiKey.name}
                    {apiKey.organisation && <Muted> · {apiKey.organisation}</Muted>}
                  </td>
                  <td>
                    <a href={`mailto:${apiKey.email}`}>{apiKey.email}</a>
                  </td>
                  <td>
                    <code>{apiKey.keyPrefix}…</code>
                  </td>
                  <td>{formatDate(apiKey.reviewedAt)}</td>
                  <td>{formatDate(apiKey.lastUsedAt)}</td>
                  <td>
                    <RevokeButton disabled={isLoading} onClick={() => onRevoke(apiKey.id)} type="button">
                      {t.admin.revoke}
                    </RevokeButton>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </TableBox>
      )}
    </Section>
  )
}

const Banner = styled.output`
  margin-bottom: 16px;
  padding: 16px 18px;
  border-radius: 6px;
  background: ${COLORS.info};
  border: 1px solid #b8d3e3;
  display: flex;
  flex-direction: column;
  gap: 8px;

  code {
    font-family: ${FONTS.mono};
    font-size: 15px;
    padding: 10px 12px;
    border-radius: 8px;
    background: ${COLORS.white};
    overflow-wrap: anywhere;
  }

  > div {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }
`

const Section = styled.section`
  margin-top: 32px;
  display: flex;
  flex-direction: column;
  gap: 12px;
`

const TableBox = styled(Card)`
  overflow-x: auto;

  table {
    width: 100%;
    min-width: 720px;
    border-collapse: collapse;
    font-size: 14px;
  }

  th {
    text-align: left;
    color: ${COLORS.muted};
    font-size: 12px;
    font-weight: 500;
  }

  th,
  td {
    padding: 10px 14px;
    border-bottom: 1px solid ${COLORS.divider};
  }

  code {
    font-family: ${FONTS.mono};
    font-size: 13px;
  }
`

const RevokeButton = styled.button`
  min-height: 36px;
  padding: 0 12px;
  border-radius: 8px;
  border: 1px solid ${COLORS.reject};
  background: ${COLORS.white};
  color: ${COLORS.reject};
  font: inherit;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
`
