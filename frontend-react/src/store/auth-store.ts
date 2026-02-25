import { create } from 'zustand'
import { persist, createJSONStorage } from 'zustand/middleware'
import type { SessionWebUserDto } from '@/types'
import { AUTH_STORAGE_KEY } from '@/lib/storage'

export interface AuthState {
  user: SessionWebUserDto | null
  setUser: (user: SessionWebUserDto) => void
  clearUser: () => void
  patchUser: (patch: Partial<SessionWebUserDto>) => void
  getToken: () => string | null
  getTenantId: () => string
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,
      setUser: (user) => set({ user }),
      clearUser: () => set({ user: null }),
      patchUser: (patch) => {
        const current = get().user
        if (!current) {
          return
        }
        set({ user: { ...current, ...patch } })
      },
      getToken: () => get().user?.token ?? null,
      getTenantId: () => get().user?.tenantId || 'default',
    }),
    {
      name: AUTH_STORAGE_KEY,
      storage: createJSONStorage(() => sessionStorage),
      partialize: (state) => ({ user: state.user }),
    }
  )
)

