/// <reference lib="webworker" />
import SparkMD5 from 'spark-md5'

interface WorkerRequest {
  type: 'compute'
  uid: string
  file: File
  chunkSize: number
}

const workerContext = self as DedicatedWorkerGlobalScope

workerContext.onmessage = async (event: MessageEvent<WorkerRequest>) => {
  const { type, uid, file, chunkSize } = event.data
  if (type !== 'compute') {
    return
  }

  try {
    const spark = new SparkMD5.ArrayBuffer()
    const chunks = Math.ceil(file.size / chunkSize)

    for (let index = 0; index < chunks; index += 1) {
      const start = index * chunkSize
      const end = Math.min(start + chunkSize, file.size)
      const arrayBuffer = await file.slice(start, end).arrayBuffer()
      spark.append(arrayBuffer)
      workerContext.postMessage({
        uid,
        type: 'progress',
        progress: Math.round(((index + 1) / chunks) * 100),
      })
    }

    workerContext.postMessage({ uid, type: 'done', md5: spark.end() })
  } catch (error) {
    workerContext.postMessage({
      uid,
      type: 'error',
      errorMsg: error instanceof Error ? error.message : 'MD5计算失败',
    })
  }
}

export {}

