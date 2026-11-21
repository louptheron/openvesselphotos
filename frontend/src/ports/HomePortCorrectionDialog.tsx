import { useEffect, useRef } from 'react'
import styled from 'styled-components'

import type { PortVessel } from '../api/types'
import { HomePortCorrectionForm } from '../components/HomePortCorrectionForm'
import { fr } from '../i18n/fr'
import { COLORS, FONTS } from '../theme'

type Props = {
  /** The port the vessel is listed at */
  locode: string
  onClose: () => void
  vessel: PortVessel
}

/** Suggests the port a vessel of the list is really based at, in a modal dialog: the admin checks it first. */
export function HomePortCorrectionDialog({ locode, onClose, vessel }: Props) {
  const ref = useRef<HTMLDialogElement>(null)

  useEffect(() => {
    const dialog = ref.current
    // Not closed on unmount: its removal takes it out of the top layer, while close() would call onClose
    if (dialog && !dialog.open) {
      dialog.showModal()
    }
  }, [])

  return (
    <Dialog
      aria-labelledby="home-port-correction-title"
      onClick={event => {
        // A click on the backdrop
        if (event.target === event.currentTarget) {
          onClose()
        }
      }}
      onClose={onClose}
      ref={ref}
    >
      <Content>
        <Title id="home-port-correction-title">{vessel.name ?? 'Navire sans nom'} n’est pas basé ici&nbsp;?</Title>
        <Lead>
          Indiquez le port où il est basé&nbsp;: il y sera rattaché une fois votre signalement vérifié.
          <Mono>{vessel.registration ? `${vessel.registration} · ${vessel.cfr}` : vessel.cfr}</Mono>
        </Lead>
        <HomePortCorrectionForm
          currentLocode={locode}
          onCancel={onClose}
          t={fr}
          vesselKey={{ key: vessel.cfr, keyType: 'cfr' }}
        />
      </Content>
    </Dialog>
  )
}

const Dialog = styled.dialog`
  width: min(440px, calc(100vw - 32px));
  max-height: calc(100dvh - 32px);
  padding: 0;
  border: 1px solid ${COLORS.cardBorder};
  border-radius: 8px;
  background: ${COLORS.white};
  color: ${COLORS.ink};

  &::backdrop {
    background: rgb(0 0 0 / 40%);
  }
`

const Content = styled.div`
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 20px;
`

const Title = styled.h2`
  margin: 0;
  font-family: ${FONTS.display};
  font-size: 20px;
  line-height: 1.2;
`

const Lead = styled.p`
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin: 0;
  color: ${COLORS.text};
  font-size: 14px;
  line-height: 1.45;
`

const Mono = styled.span`
  font-family: ${FONTS.mono};
  font-size: 12px;
  color: ${COLORS.muted};
`
