import styled, { css } from 'styled-components'

import { COLORS, FONTS } from '../theme'

export const Container = styled.div`
  max-width: 1240px;
  margin: 0 auto;
  padding: 0 24px;
`

export const Title = styled.h1`
  margin: 0;
  font-family: ${FONTS.display};
  font-weight: 700;
  font-size: clamp(32px, 5vw, 52px);
  line-height: 1.04;
`

export const SectionTitle = styled.h2`
  margin: 0;
  font-family: ${FONTS.display};
  font-size: 26px;
`

export const Lead = styled.p`
  margin: 0;
  font-size: 17px;
  line-height: 1.5;
  color: ${COLORS.text};
  max-width: 640px;
`

export const Muted = styled.span`
  font-size: 13px;
  color: ${COLORS.muted};
`

export const Card = styled.div`
  background: ${COLORS.white};
  border: 1px solid ${COLORS.cardBorder};
  border-radius: 14px;
`

export const Chip = styled.span`
  align-self: flex-start;
  font-size: 13px;
  padding: 6px 12px;
  border-radius: 16px;
  background: ${COLORS.white};
  border: 1px solid ${COLORS.border};
`

export const Mono = styled.span`
  font-family: ${FONTS.mono};
`

const buttonBase = css`
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 44px;
  padding: 0 18px;
  border-radius: 12px;
  font: inherit;
  font-size: 15px;
  font-weight: 600;
  text-decoration: none;
  cursor: pointer;

  &:disabled {
    opacity: 0.55;
    cursor: not-allowed;
  }
`

export const PrimaryButton = styled.button`
  ${buttonBase};
  border: none;
  background: ${COLORS.accent};
  color: ${COLORS.accentInk};
`

export const SecondaryButton = styled.button`
  ${buttonBase};
  border: 1px solid ${COLORS.ink};
  background: ${COLORS.white};
  color: ${COLORS.ink};
`

export const TextButton = styled.button`
  ${buttonBase};
  border: none;
  background: transparent;
  color: ${COLORS.link};
  font-weight: 500;
  padding: 0 8px;
`

/** A toggle pill; `$active` fills it. */
export const Pill = styled.button<{ $active: boolean }>`
  min-height: 44px;
  padding: 0 16px;
  border-radius: 22px;
  border: 1px solid ${p => (p.$active ? COLORS.ink : COLORS.inputBorder)};
  background: ${p => (p.$active ? COLORS.ink : COLORS.white)};
  color: ${p => (p.$active ? COLORS.white : COLORS.ink)};
  font: inherit;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
`

export const Field = styled.div`
  display: flex;
  flex-direction: column;
  gap: 6px;
`

export const Label = styled.label`
  font-size: 14px;
  font-weight: 500;
`

const inputBase = css`
  min-height: 48px;
  padding: 0 12px;
  border: 1px solid ${COLORS.inputBorder};
  border-radius: 10px;
  font: inherit;
  /* 16 px at least: iOS Safari zooms into smaller fields when they get the focus */
  font-size: 16px;
  background: ${COLORS.white};
  color: ${COLORS.ink};
  width: 100%;
`

export const Input = styled.input`
  ${inputBase};
`

export const Select = styled.select`
  ${inputBase};
`

export const TextArea = styled.textarea`
  ${inputBase};
  padding: 10px 12px;
  min-height: 88px;
  resize: vertical;
`

export const CodeBlock = styled.pre`
  margin: 0;
  background: ${COLORS.ink};
  color: ${COLORS.codeText};
  border-radius: 12px;
  padding: 14px 16px;
  font-family: ${FONTS.mono};
  font-size: 13px;
  line-height: 1.6;
  overflow-x: auto;
`

export const ErrorMessage = styled.p`
  margin: 0;
  padding: 10px 12px;
  border-radius: 10px;
  background: ${COLORS.accentSoft};
  color: ${COLORS.reject};
  font-size: 14px;
`

export const SuccessMessage = styled.div`
  padding: 14px 16px;
  border-radius: 10px;
  background: ${COLORS.info};
  font-size: 15px;
  line-height: 1.5;
`
