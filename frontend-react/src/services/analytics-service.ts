import { onCLS, onFCP, onINP, onLCP, onTTFB, type Metric } from 'web-vitals'

type ReportMetric = Pick<Metric, 'value' | 'rating' | 'delta' | 'id' | 'navigationType'>

const postMetric = async (metric: {
  name: string
  value: number
  rating: string
  delta: number
  id: string
  navigationType: string
}) => {
  const payload = {
    ...metric,
    page: window.location.pathname,
    timestamp: Date.now(),
    userAgent: navigator.userAgent,
    deviceType: /mobile|android|iphone|ipad/i.test(navigator.userAgent.toLowerCase()) ? 'mobile' : 'desktop',
    connectionType:
      ((navigator as Navigator & { connection?: { effectiveType?: string } }).connection?.effectiveType as string) ||
      'unknown',
  }

  const body = JSON.stringify(payload)
  if (navigator.sendBeacon) {
    navigator.sendBeacon('/api/analytics/web-vitals', new Blob([body], { type: 'application/json' }))
  } else {
    window
      .fetch('/api/analytics/web-vitals', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body,
        keepalive: true,
      })
      .catch(() => undefined)
  }
}

const report = (name: string, metric: ReportMetric) => {
  void postMetric({
    name,
    value: metric.value,
    rating: metric.rating,
    delta: metric.delta,
    id: metric.id,
    navigationType: metric.navigationType,
  })
}

export const initWebVitals = (): void => {
  onLCP((metric) => report('LCP', metric))
  onINP((metric) => report('INP', metric))
  onCLS((metric) => report('CLS', metric))
  onTTFB((metric) => report('TTFB', metric))
  onFCP((metric) => report('FCP', metric))
}
