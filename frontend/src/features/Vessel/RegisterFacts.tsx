import { type ReactNode, useState } from 'react'
import styled from 'styled-components'

import type { Vessel } from '../../api/types'
import { Card } from '../../components/ui'
import { useTranslation } from '../../i18n'
import { COLORS, FONTS } from '../../theme'

/** The vessel's technical data from the EU fleet register, when imported. */
export function RegisterFacts({ vessel }: { vessel: Vessel }) {
  const { formatNumber, t } = useTranslation()
  const [currentYear] = useState(() => new Date().getFullYear())
  const register = vessel.register
  if (!register) {
    return null
  }
  const vesselType = register.vesselType ? t.register.vesselTypes[register.vesselType] : undefined
  const facts: { name: string; value: ReactNode }[] = [
    { name: t.register.type, value: vesselType },
    {
      name: t.register.built,
      value: register.yearBuilt && t.register.yearAndAge(register.yearBuilt, currentYear - register.yearBuilt)
    },
    { name: t.register.length, value: register.lengthMeters && t.register.meters(formatNumber(register.lengthMeters)) },
    {
      name: t.register.engine,
      value:
        register.enginePowerKw &&
        t.register.power(
          formatNumber(register.enginePowerKw),
          formatNumber(Math.round(register.enginePowerKw * t.register.horsepowerPerKilowatt))
        )
    },
    {
      name: t.register.tonnage,
      value: register.tonnageGt && t.register.grossTonnage(formatNumber(register.tonnageGt))
    },
    { name: t.register.mainGear, value: register.mainGear && label(t.register.gears, register.mainGear) },
    {
      name: t.register.otherGears,
      value: register.otherGears.length > 0 && register.otherGears.map(gear => label(t.register.gears, gear)).join(', ')
    },
    { name: t.register.callSign, value: register.callSign && <code>{register.callSign}</code> },
    { name: vessel.flag === 'FRA' ? t.register.frenchPort : t.register.port, value: vessel.port }
  ]
  const shown = facts.filter(({ value }) => value)
  if (shown.length === 0) {
    return null
  }

  return (
    <Panel as="section" aria-labelledby="register-title">
      <h2 id="register-title">{t.register.title}</h2>
      <dl>
        {shown.map(({ name, value }) => (
          <div key={name}>
            <dt>{name}</dt>
            <dd>{value}</dd>
          </div>
        ))}
      </dl>
      <small>{t.register.source}</small>
    </Panel>
  )
}

function label(labels: Record<string, string>, code: string): string {
  return labels[code] ?? code
}

const Panel = styled(Card)`
  padding: 18px 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;

  h2 {
    margin: 0;
    font-family: ${FONTS.display};
    font-size: 20px;
  }

  dl {
    margin: 0;
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px 18px;
  }

  dt {
    font-size: 12px;
    color: ${COLORS.muted};
  }

  dd {
    margin: 2px 0 0;
    font-weight: 500;
    color: ${COLORS.ink};
  }

  code {
    font-family: ${FONTS.mono};
    font-size: 14px;
  }

  small {
    color: ${COLORS.muted};
  }
`
