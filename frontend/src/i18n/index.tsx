import { createContext, type ReactNode, useContext, useEffect, useMemo } from 'react'

import { en, type Messages } from './en'
import { fr } from './fr'

export type Language = 'en' | 'fr'

const MESSAGES: Record<Language, Messages> = { en, fr }
const STORAGE_KEY = 'ovp.language'
const FRENCH_PREFIX = '/fr'

/** Pages are in French under /fr, in English elsewhere, so that search engines index both (see PagesController). */
export function getPathLanguage(pathname: string): Language {
  return pathname === FRENCH_PREFIX || pathname.startsWith(`${FRENCH_PREFIX}/`) ? 'fr' : 'en'
}

/** The router's base path in a language: its links then stay in that language. */
export function getBasename(language: Language): string {
  return language === 'fr' ? FRENCH_PREFIX : '/'
}

/** e.g. "/fr/vessels" for "/vessels" in French, and "/vessels" for "/fr/vessels" in English */
export function getLanguagePath(language: Language, pathname: string): string {
  const path = getPathLanguage(pathname) === 'fr' ? pathname.slice(FRENCH_PREFIX.length) || '/' : pathname

  return language === 'fr' ? `${FRENCH_PREFIX}${path === '/' ? '' : path}` : path
}

/**
 * The French page of an English URL, for a visitor who chose French or whose browser prefers it. Search engines crawl
 * in English: they are not redirected.
 */
export function getPreferredLanguageUrl(location: Location): string | undefined {
  if (getPathLanguage(location.pathname) === 'fr' || detectLanguage() !== 'fr') {
    return undefined
  }

  return `${getLanguagePath('fr', location.pathname)}${location.search}${location.hash}`
}

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
  formatNumber: (value: number) => string
  language: Language
  setLanguage: (language: Language) => void
  t: Messages
}

const I18nContext = createContext<I18n | undefined>(undefined)

/** Switches to the same page in the other language, and remembers the choice. */
function setLanguage(language: Language) {
  try {
    window.localStorage.setItem(STORAGE_KEY, language)
  } catch {
    // Not saved: the language is detected again on the next visit
  }
  const { hash, pathname, search } = window.location
  window.location.assign(`${getLanguagePath(language, pathname)}${search}${hash}`)
}

export function I18nProvider({ children, language }: { children: ReactNode; language: Language }) {
  useEffect(() => {
    document.documentElement.lang = language
  }, [language])

  const value = useMemo<I18n>(
    () => ({
      formatDate: date => (date ? new Date(date).toLocaleDateString(language === 'fr' ? 'fr-FR' : 'en-GB') : '—'),
      formatNumber: number => number.toLocaleString(language === 'fr' ? 'fr-FR' : 'en-GB'),
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
