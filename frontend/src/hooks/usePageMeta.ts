import { useEffect } from 'react'

const SITE_NAME = 'Open Vessel Photos'

/**
 * Updates the title and description while navigating in the app. The server sets them, with the canonical URL and
 * structured data, on the first load of each page (see PagesController).
 */
export function usePageMeta(title: string | undefined, description?: string) {
  useEffect(() => {
    if (!title) {
      return
    }
    document.title = title.includes(SITE_NAME) ? title : `${title} | ${SITE_NAME}`
    if (description) {
      document.querySelector('meta[name="description"]')?.setAttribute('content', description)
    }
  }, [title, description])
}
