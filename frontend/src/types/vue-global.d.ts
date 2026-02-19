import type { DefineComponent } from 'vue'

// Augment Vue's ComponentCustomProperties for global properties
declare module 'vue' {
  export interface ComponentCustomProperties {
    Utils: { size2Str: (limit: number) => string }
    globalInfo: { avatarUrl: string; imageUrl: string }
    Message: {
      success: (msg: string) => void
      error: (msg: string) => void
      warning: (msg: string) => void
    }
    Request: unknown
    Confirm: unknown
  }
}

// Declare Vue SFC modules
declare module '*.vue' {
  const component: DefineComponent<object, object, unknown>
  export default component
}
