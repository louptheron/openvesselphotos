import styled from 'styled-components'

import { getErrorMessage, useFindHomePortsFromRegistrationMutation } from '../../api/api'
import { ErrorMessage, SecondaryButton, SuccessMessage } from '../../components/ui'
import { useTranslation } from '../../i18n'

/**
 * Gives the vessels of the EU fleet register still without home port the port of their quartier d'immatriculation,
 * and lists the quartiers matched to no port, to complete the aliases.
 */
export function HomePortsFromRegistration() {
  const [start, { data: result, error, isLoading }] = useFindHomePortsFromRegistrationMutation()
  const { t } = useTranslation()

  return (
    <Section>
      <div>
        <SecondaryButton disabled={isLoading} onClick={() => start()} type="button">
          {isLoading ? t.admin.importing : t.admin.homePortsFromRegistrationStart}
        </SecondaryButton>
      </div>
      {error && <ErrorMessage>{getErrorMessage(error, t)}</ErrorMessage>}
      {result && (
        <SuccessMessage aria-live="polite">
          {t.admin.homePortsFromRegistrationResult(result)}
          {result.unmatched.length > 0 && (
            <p>
              {t.admin.homePortsFromRegistrationUnmatched} {result.unmatched.join(', ')}
            </p>
          )}
          {result.matched.length > 0 && (
            <details>
              <summary>{t.admin.homePortsFromRegistrationMatched}</summary>
              <List>
                {result.matched.map(line => (
                  <li key={line}>{line}</li>
                ))}
              </List>
            </details>
          )}
        </SuccessMessage>
      )}
    </Section>
  )
}

const Section = styled.section`
  margin-top: 32px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-width: 760px;
`

const List = styled.ul`
  margin: 8px 0 0;
  padding-left: 18px;
  font-size: 13px;
`
