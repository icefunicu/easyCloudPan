declare module 'dplayer' {
  class DPlayer {
    constructor(options: {
      element: HTMLElement
      theme?: string
      screenshot?: boolean
      autoplay?: boolean
      preload?: string
      volume?: number
      video: {
        url: string
        type?: string
        customType?: Record<string, (video: HTMLVideoElement, player: unknown) => void>
      }
    })
    destroy(): void
    on(event: string, handler: () => void): void
  }
  export default DPlayer
}

declare module 'aplayer' {
  class APlayer {
    constructor(options: {
      container: HTMLElement
      audio: {
        url: string
        name?: string
        cover?: string
        artist?: string
      }
    })
    destroy(): void
  }
  export default APlayer
}

declare global {
  interface Hls {
    loadSource(url: string): void
    attachMedia(video: HTMLVideoElement): void
    on(event: string, handler: (event: string, data: { fatal: boolean }) => void): void
    Events: { ERROR: string }
  }

  interface Window {
    Hls: {
      new (options?: { maxBufferLength?: number; maxMaxBufferLength?: number }): Hls
      Events: { ERROR: string }
    }
  }
}
