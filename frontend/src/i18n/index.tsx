import { createContext, type ReactNode, useContext, useEffect, useMemo, useState } from 'react'

import { en, type Messages } from './en'
import { fr } from './fr'

export type Language = 'en' | 'fr'

const MESSAGES: Record<Language, Messages> = { en, fr }
const STORAGE_KEY = 'ovp.language'

/** The saved choice, else the first of the browser's languages that the site speaks, else English. */
function detectLanguage(): Language {
  try {
    const saved = window.localStorage.getItem(STORAGE_KEY)
    if (saved === 'en' || saved === 'fr') {
      return saved
    }
  } catch {
    // Storage can be blocked: the browser's languages decide
  }
  for (const language of navigator.languages ?? [navigator.language]) {
    const code = language.toLowerCase().slice(0, 2)
    if (code === 'fr' || code === 'en') {
      return code
    }
  }

  return 'en'
}

type I18n = {
  formatDate: (value: string | null | undefined) => string
  language: Language
  setLanguage: (language: Language) => void
  t: Messages
}

const I18nContext = createContext<I18n | undefined>(undefined)

export function I18nProvider({ children }: { children: ReactNode }) {
  const [language, setLanguage] = useState<Language>(detectLanguage)

  useEffect(() => {
    document.documentElement.lang = language
    try {
      window.localStorage.setItem(STORAGE_KEY, language)
    } catch {
      // Not saved: the language is detected again on the next visit
    }
  }, [language])

  const value = useMemo<I18n>(
    () => ({
      formatDate: date => (date ? new Date(date).toLocaleDateString(language === 'fr' ? 'fr-FR' : 'en-GB') : '—'),
      language,
      setLanguage,
      t: MESSAGES[language]
    }),
    [language]
  )

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>
}

export function useTranslation(): I18n {
  const context = useContext(I18nContext)
  if (!context) {
    throw new Error('useTranslation must be used within an I18nProvider')
  }

  return context
}
