import { useState } from 'react'
import styled from 'styled-components'

import { useGetActiveApiKeysQuery } from '../../api/api'
import type { ApprovedApiKey } from '../../api/types'
import { Card, Muted, PrimaryButton, SecondaryButton, SectionTitle, TextButton } from '../../components/ui'
import { COLORS, FONTS } from '../../theme'

/** Shows a key once, right after its approval: only its hash is stored. */
export function ApprovedApiKeyBanner({ approved, onClose }: { approved: ApprovedApiKey; onClose: () => void }) {
  const [isCopied, setIsCopied] = useState(false)
  const { apiKey, key } = approved
  const docsUrl = `${window.location.origin}/docs#api-key`
  const mailto =
    `mailto:${encodeURIComponent(apiKey.email)}` +
    `?subject=${encodeURIComponent('Your Open Vessel Photos API key')}` +
    `&body=${encodeURIComponent(
      `Hello ${apiKey.name},\n\nYour API key request was approved. Here is your key:\n\n${key}\n\n` +
        `Send it in the x-api-key header of your POST requests, and keep it secret. Documentation: ${docsUrl}\n`
    )}`

  const copy = async () => {
    await navigator.clipboard.writeText(key)
    setIsCopied(true)
  }

  return (
    <Banner>
      <strong>API key created for {apiKey.name}</strong>
      <span>Send it now: it won't be shown again.</span>
      <code>{key}</code>
      <div>
        <PrimaryButton as="a" href={mailto}>
          Email it to {apiKey.email}
        </PrimaryButton>
        <SecondaryButton onClick={copy} type="button">
          {isCopied ? 'Copied' : 'Copy the key'}
        </SecondaryButton>
        <TextButton onClick={onClose} type="button">
          Done
        </TextButton>
      </div>
    </Banner>
  )
}

export function ActiveApiKeys({ isLoading, onRevoke }: { isLoading: boolean; onRevoke: (id: number) => void }) {
  const { data: apiKeys } = useGetActiveApiKeysQuery()

  return (
    <Section>
      <SectionTitle as="h2">Active API keys</SectionTitle>
      {apiKeys?.length === 0 && <Muted>No active key.</Muted>}
      {apiKeys && apiKeys.length > 0 && (
        <TableBox>
          <table>
            <thead>
              <tr>
                <th scope="col">Name</th>
                <th scope="col">Email</th>
                <th scope="col">Key</th>
                <th scope="col">Created</th>
                <th scope="col">Last used</th>
                <th scope="col">
                  <span className="visually-hidden">Actions</span>
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
                      Revoke
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

function formatDate(value: string | null): string {
  return value ? new Date(value).toLocaleDateString() : '—'
}

const Banner = styled.output`
  margin-bottom: 16px;
  padding: 16px 18px;
  border-radius: 12px;
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
