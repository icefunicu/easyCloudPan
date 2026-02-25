type Handler<T = unknown> = (payload: T) => void

class EventBus {
  private target = new EventTarget()

  emit<T>(event: string, payload?: T): void {
    this.target.dispatchEvent(new CustomEvent(event, { detail: payload }))
  }

  on<T>(event: string, handler: Handler<T>): () => void {
    const listener = (evt: Event) => {
      const customEvt = evt as CustomEvent<T>
      handler(customEvt.detail)
    }
    this.target.addEventListener(event, listener)
    return () => this.target.removeEventListener(event, listener)
  }
}

export const eventBus = new EventBus()

