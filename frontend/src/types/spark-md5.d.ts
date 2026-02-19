declare module 'spark-md5' {
  interface SparkMD5 {
    append(str: string): SparkMD5
    end(raw?: boolean): string
    reset(): void
    getState(): State
    setState(state: State): SparkMD5
    destroy(): void
  }

  interface State {
    buff: Uint8Array
    length: number
    hash: number[]
  }

  interface ArrayBuffer {
    byteLength: number
  }

  export function hash(str: string, raw?: boolean): string
  export function hash(arr: globalThis.ArrayBuffer, raw?: boolean): string

  export default SparkMD5
}
