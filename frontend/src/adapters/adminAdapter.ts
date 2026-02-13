import type { SysSettingsDto } from '@/types'

export function adaptSysSettings(data: unknown): SysSettingsDto {
  const raw = data as Record<string, unknown>
  return {
    registerEmailTitle: raw.registerEmailTitle as string,
    registerEmailContent: raw.registerEmailContent as string,
    userInitUseSpace: raw.userInitUseSpace as number,
  }
}
