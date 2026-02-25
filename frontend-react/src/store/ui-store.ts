import { create } from 'zustand'

interface UiState {
  uploaderOpen: boolean
  mobileMenuOpen: boolean
  setUploaderOpen: (open: boolean) => void
  setMobileMenuOpen: (open: boolean) => void
}

export const useUiStore = create<UiState>((set) => ({
  uploaderOpen: false,
  mobileMenuOpen: false,
  setUploaderOpen: (open) => set({ uploaderOpen: open }),
  setMobileMenuOpen: (open) => set({ mobileMenuOpen: open }),
}))

