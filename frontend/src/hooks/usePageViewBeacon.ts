import { useEffect } from 'react'
import { useLocation } from 'react-router-dom'

/**
 * Tells the server a page is shown, client-side navigations included, for the admin's statistics of each website (see
 * PageViewsController). Only the path is sent, without query: no personal data. Fire and forget: a failure, or the
 * rate limit, loses the view and nothing else.
 */
export function usePageViewBeacon({ skip = false }: { skip?: boolean } = {}) {
  const { pathname } = useLocation()

  useEffect(() => {
    if (skip) {
      return
    }
    fetch('/v1/page-views', {
      body: JSON.stringify({ path: pathname }),
      headers: { 'Content-Type': 'application/json' },
      keepalive: true,
      method: 'POST'
    }).catch(() => undefined)
  }, [pathname, skip])
}
