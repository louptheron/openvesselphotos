import styled from 'styled-components'

import type { Port } from '../../api/types'
import { useTranslation } from '../../i18n'
import { COLORS } from '../../theme'
import { getRegisterCountries } from './regions'

/** Which member states' registers are imported: for now, France only. */
export function ScopeNote({ ports }: { ports: Port[] }) {
  const { language, t } = useTranslation()
  const countries = getRegisterCountries(ports)
  const names = new Intl.DisplayNames([language], { type: 'region' })

  return (
    <Note role="note">
      {countries.length === 1 && countries[0] === 'FR'
        ? t.ports.scopeFranceOnly
        : t.ports.scopeCountries(countries.map(code => names.of(code) ?? code).join(', '))}
    </Note>
  )
}

const Note = styled.p`
  margin: 0;
  padding: 12px 16px;
  border-radius: 10px;
  background: ${COLORS.info};
  color: ${COLORS.text};
  font-size: 15px;
  line-height: 1.5;
`
