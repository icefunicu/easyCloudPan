import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { getUseSpace } from '@/services/account-service'
import type { UserSpaceDto } from '@/types'
import { percent, sizeToText } from '@/lib/format'

const defaultSpace: UserSpaceDto = {
  useSpace: 0,
  totalSpace: 0,
}

export const useSpaceMonitor = () => {
  const [spaceInfo, setSpaceInfo] = useState<UserSpaceDto>(defaultSpace)
  const [refreshing, setRefreshing] = useState(false)
  const timerRef = useRef<number | null>(null)

  const loadSpace = useCallback(async () => {
    setRefreshing(true)
    const data = await getUseSpace()
    if (data) {
      setSpaceInfo(data)
    }
    setRefreshing(false)
  }, [])

  const ensureAutoRefresh = useCallback((activeTaskCount: number) => {
    if (timerRef.current) {
      window.clearInterval(timerRef.current)
      timerRef.current = null
    }
    if (activeTaskCount > 0) {
      timerRef.current = window.setInterval(() => {
        void loadSpace()
      }, 8000)
    }
  }, [loadSpace])

  useEffect(() => {
    void loadSpace()
    return () => {
      if (timerRef.current) {
        window.clearInterval(timerRef.current)
      }
    }
  }, [loadSpace])

  const usedPercent = useMemo(() => percent(spaceInfo.useSpace, spaceInfo.totalSpace), [spaceInfo])
  const remainSpace = useMemo(() => Math.max(0, (spaceInfo.totalSpace || 0) - (spaceInfo.useSpace || 0)), [spaceInfo])

  return {
    spaceInfo,
    refreshing,
    usedPercent,
    remainSpaceText: sizeToText(remainSpace),
    usedText: sizeToText(spaceInfo.useSpace),
    totalText: sizeToText(spaceInfo.totalSpace),
    loadSpace,
    ensureAutoRefresh,
  }
}

