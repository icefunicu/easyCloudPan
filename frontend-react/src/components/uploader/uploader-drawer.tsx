import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Button, Drawer, Flex, List, Progress, Space, Tag, Typography } from 'antd'
import { DeleteOutlined, PauseCircleOutlined, PlayCircleOutlined, ReloadOutlined } from '@ant-design/icons'
import { getUploadedChunks, uploadFileChunk } from '@/services/file-service'
import { eventBus } from '@/lib/event-bus'
import { sizeToText } from '@/lib/format'
import type { UploadResultDto } from '@/types'

const { Text } = Typography

const chunkSize = 10 * 1024 * 1024
const maxConcurrentUploads = 3

interface UploadTask {
  uid: string
  file: File
  fileName: string
  filePid: string
  fileId: string
  md5?: string
  md5Progress: number
  uploadProgress: number
  uploadSize: number
  totalSize: number
  pause: boolean
  chunkIndex: number
  uploading: boolean
  chunkLoadedMap: Record<number, number>
  status:
    | 'init'
    | 'uploading'
    | 'upload_finish'
    | 'upload_seconds'
    | 'fail'
    | 'retrying'
    | 'network_error'
    | 'auth_error'
    | 'server_error'
    | 'emptyfile'
    | 'transferring'
    | 'transfer_done'
    | 'transfer_fail'
  errorMsg?: string
  worker?: Worker
  transferSource?: EventSource
  isResume: boolean
}

interface UploaderDrawerProps {
  open: boolean
  onClose: () => void
  onUploadDone?: () => void
  onActiveTaskCountChange?: (count: number) => void
}

const statusMeta: Record<string, { color: string; text: string }> = {
  init: { color: 'gold', text: '计算MD5中' },
  uploading: { color: 'processing', text: '上传中' },
  upload_finish: { color: 'success', text: '上传完成' },
  upload_seconds: { color: 'success', text: '秒传完成' },
  retrying: { color: 'processing', text: '重试中' },
  fail: { color: 'error', text: '上传失败' },
  network_error: { color: 'error', text: '网络错误' },
  auth_error: { color: 'error', text: '鉴权失败' },
  server_error: { color: 'error', text: '服务端错误' },
  emptyfile: { color: 'default', text: '空文件' },
  transferring: { color: 'warning', text: '转码中' },
  transfer_done: { color: 'success', text: '转码完成' },
  transfer_fail: { color: 'error', text: '转码失败' },
}

const createUploadFileId = (): string => {
  const chars = '0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ'
  let id = ''
  for (let i = 0; i < 12; i += 1) {
    id += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  return id
}

const createTask = (file: File, filePid: string): UploadTask => ({
  uid: `${Date.now()}_${Math.random().toString(36).slice(2, 9)}`,
  file,
  fileName: file.name,
  filePid,
  fileId: createUploadFileId(),
  md5Progress: 0,
  uploadProgress: 0,
  uploadSize: 0,
  totalSize: file.size,
  pause: false,
  chunkIndex: 0,
  uploading: false,
  chunkLoadedMap: {},
  status: file.size === 0 ? 'emptyfile' : 'init',
  isResume: false,
})

export const UploaderDrawer = ({
  open,
  onClose,
  onUploadDone,
  onActiveTaskCountChange,
}: UploaderDrawerProps) => {
  const [tasks, setTasks] = useState<UploadTask[]>([])
  const tasksRef = useRef<UploadTask[]>([])

  useEffect(() => {
    tasksRef.current = tasks
  }, [tasks])

  const getTask = useCallback((uid: string) => tasksRef.current.find((item) => item.uid === uid), [])

  const patchTask = useCallback((uid: string, patch: Partial<UploadTask>) => {
    setTasks((prev) => prev.map((task) => (task.uid === uid ? { ...task, ...patch } : task)))
  }, [])

  const patchTaskBy = useCallback((uid: string, updater: (task: UploadTask) => UploadTask) => {
    setTasks((prev) => prev.map((task) => (task.uid === uid ? updater(task) : task)))
  }, [])

  const removeTask = useCallback((uid: string) => {
    const task = getTask(uid)
    if (task?.worker) {
      task.worker.terminate()
    }
    if (task?.transferSource) {
      task.transferSource.close()
    }
    setTasks((prev) => prev.filter((item) => item.uid !== uid))
  }, [getTask])

  const syncProgress = useCallback((uid: string) => {
    patchTaskBy(uid, (task) => {
      const loaded = Object.values(task.chunkLoadedMap).reduce((sum, value) => sum + value, 0)
      const uploadSize = Math.min(task.totalSize, loaded)
      const uploadProgress = task.totalSize > 0 ? Math.floor((uploadSize / task.totalSize) * 100) : 0
      return {
        ...task,
        uploadSize,
        uploadProgress,
      }
    })
  }, [patchTaskBy])

  const computeMd5 = useCallback(
    async (uid: string): Promise<string | null> => {
      const task = getTask(uid)
      if (!task) {
        return null
      }

      return new Promise((resolve) => {
        const worker = new Worker(new URL('../../workers/md5.worker.ts', import.meta.url), { type: 'module' })
        patchTask(uid, { worker })

        worker.onmessage = (event: MessageEvent<{ uid: string; type: string; progress?: number; md5?: string; errorMsg?: string }>) => {
          if (event.data.uid !== uid) {
            return
          }
          const current = getTask(uid)
          if (!current) {
            worker.terminate()
            resolve(null)
            return
          }

          if (event.data.type === 'progress') {
            patchTask(uid, { md5Progress: event.data.progress || 0 })
            return
          }

          if (event.data.type === 'done' && event.data.md5) {
            patchTask(uid, {
              md5: event.data.md5,
              md5Progress: 100,
              status: 'uploading',
              worker: undefined,
            })
            worker.terminate()
            resolve(event.data.md5)
            return
          }

          patchTask(uid, {
            status: 'fail',
            errorMsg: event.data.errorMsg || 'MD5计算失败',
            worker: undefined,
          })
          worker.terminate()
          resolve(null)
        }

        worker.onerror = () => {
          patchTask(uid, {
            status: 'fail',
            errorMsg: 'MD5计算失败',
            worker: undefined,
          })
          worker.terminate()
          resolve(null)
        }

        worker.postMessage({
          type: 'compute',
          uid,
          file: task.file,
          chunkSize,
        })
      })
    },
    [getTask, patchTask]
  )

  const checkResumeChunks = useCallback(async (uid: string): Promise<void> => {
    const task = getTask(uid)
    if (!task) {
      return
    }

    const uploaded = await getUploadedChunks(task.fileId, task.filePid)
    if (!uploaded || uploaded.length === 0) {
      return
    }

    patchTaskBy(uid, (current) => {
      const chunkLoadedMap = { ...current.chunkLoadedMap }
      const chunks = Math.ceil(current.totalSize / chunkSize)
      uploaded.forEach((chunkIndex) => {
        if (chunkIndex >= 0 && chunkIndex < chunks) {
          const start = chunkIndex * chunkSize
          const end = Math.min(start + chunkSize, current.totalSize)
          chunkLoadedMap[chunkIndex] = end - start
        }
      })
      return {
        ...current,
        isResume: true,
        chunkLoadedMap,
        chunkIndex: Math.max(...uploaded, -1) + 1,
      }
    })
    syncProgress(uid)
  }, [getTask, patchTaskBy, syncProgress])

  const startTransferWatch = useCallback((uid: string, fileId: string) => {
    patchTask(uid, { status: 'transferring' })
    const source = new EventSource(`/api/file/transferStatusSse?fileId=${encodeURIComponent(fileId)}`)

    source.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data) as { status: number }
        if (data.status === 2) {
          patchTask(uid, { status: 'transfer_done' })
          source.close()
          onUploadDone?.()
          eventBus.emit('data:reload')
          return
        }
        if (data.status === 1) {
          patchTask(uid, { status: 'transfer_fail' })
          source.close()
        }
      } catch {
        source.close()
      }
    }

    source.onerror = () => {
      source.close()
    }

    patchTask(uid, { transferSource: source })
  }, [onUploadDone, patchTask])

  const uploadSingleChunk = useCallback(async (uid: string, chunkIndex: number, chunks: number): Promise<UploadResultDto | null> => {
    const task = getTask(uid)
    if (!task || task.pause) {
      return null
    }

    const start = chunkIndex * chunkSize
    const end = Math.min(start + chunkSize, task.totalSize)
    const chunk = task.file.slice(start, end)
    const chunkRealSize = end - start

    const maxRetries = 3
    let retry = 0

    while (retry <= maxRetries) {
      const result = await uploadFileChunk(
        {
          file: chunk,
          fileName: task.fileName,
          fileMd5: task.md5 || '',
          chunkIndex,
          chunks,
          fileId: task.fileId,
          filePid: task.filePid,
        },
        (event) => {
          patchTaskBy(uid, (current) => ({
            ...current,
            chunkLoadedMap: {
              ...current.chunkLoadedMap,
              [chunkIndex]: Math.min(Number(event.loaded || 0), chunkRealSize),
            },
          }))
          syncProgress(uid)
        },
        (msg) => {
          patchTask(uid, { errorMsg: msg })
        }
      )

      if (result) {
        patchTaskBy(uid, (current) => ({
          ...current,
          chunkLoadedMap: {
            ...current.chunkLoadedMap,
            [chunkIndex]: chunkRealSize,
          },
          chunkIndex: Math.max(current.chunkIndex, chunkIndex + 1),
          fileId: result.fileId || current.fileId,
          status: result.status,
          errorMsg: undefined,
        }))
        syncProgress(uid)
        return result
      }

      retry += 1
      if (retry <= maxRetries) {
        patchTask(uid, { status: 'retrying', errorMsg: `上传失败，重试中 (${retry}/${maxRetries})` })
        await new Promise((resolve) => window.setTimeout(resolve, 2 ** (retry - 1) * 1000))
      }
    }

    patchTask(uid, { status: 'fail', errorMsg: '分片上传失败' })
    return null
  }, [getTask, patchTask, patchTaskBy, syncProgress])

  const uploadChunkWindow = useCallback(async (uid: string, from: number, to: number, chunks: number) => {
    let next = from
    const running = new Set<Promise<{ result: UploadResultDto | null }>>()

    const launch = (chunkIndex: number) => {
      const promise = uploadSingleChunk(uid, chunkIndex, chunks).then((result) => ({ result }))
      running.add(promise)
      promise.finally(() => running.delete(promise))
    }

    while (next <= to && running.size < maxConcurrentUploads) {
      launch(next)
      next += 1
    }

    while (running.size > 0) {
      const { result } = await Promise.race(running)
      if (!result) {
        return
      }

      if (result.status === 'upload_seconds') {
        patchTask(uid, { status: 'upload_seconds', uploadProgress: 100, uploadSize: getTask(uid)?.totalSize || 0 })
        onUploadDone?.()
        eventBus.emit('data:reload')
        return
      }

      if (result.status === 'upload_finish') {
        const latestTask = getTask(uid)
        if (latestTask) {
          patchTask(uid, {
            status: 'upload_finish',
            uploadProgress: 100,
            uploadSize: latestTask.totalSize,
          })
          startTransferWatch(uid, latestTask.fileId)
        }
        return
      }

      const latest = getTask(uid)
      if (!latest || latest.pause || latest.status === 'fail') {
        return
      }

      while (next <= to && running.size < maxConcurrentUploads) {
        launch(next)
        next += 1
      }
    }
  }, [getTask, onUploadDone, patchTask, startTransferWatch, uploadSingleChunk])

  const uploadTask = useCallback(async (uid: string, startFrom?: number) => {
    const initialTask = getTask(uid)
    if (!initialTask || initialTask.uploading || initialTask.pause || initialTask.totalSize <= 0) {
      return
    }

    patchTask(uid, { uploading: true, status: 'uploading' })

    try {
      const currentTask = getTask(uid)
      if (!currentTask) {
        return
      }

      const chunks = Math.ceil(currentTask.totalSize / chunkSize)
      let nextChunk = typeof startFrom === 'number' ? startFrom : currentTask.chunkIndex || 0

      if (nextChunk >= chunks) {
        patchTask(uid, { uploading: false })
        return
      }

      if (chunks > 1 && nextChunk === 0) {
        const first = await uploadSingleChunk(uid, 0, chunks)
        if (!first) {
          return
        }
        if (first.status === 'upload_seconds') {
          patchTask(uid, { status: 'upload_seconds', uploadProgress: 100, uploadSize: currentTask.totalSize })
          onUploadDone?.()
          eventBus.emit('data:reload')
          return
        }
        if (first.status === 'upload_finish') {
          startTransferWatch(uid, getTask(uid)?.fileId || currentTask.fileId)
          return
        }
        nextChunk = 1
      }

      const latest = getTask(uid)
      if (!latest || latest.pause) {
        return
      }

      if (chunks === 1) {
        const single = await uploadSingleChunk(uid, 0, 1)
        if (single?.status === 'upload_seconds') {
          patchTask(uid, { status: 'upload_seconds', uploadProgress: 100, uploadSize: latest.totalSize })
          onUploadDone?.()
          eventBus.emit('data:reload')
          return
        }
        if (single?.status === 'upload_finish') {
          startTransferWatch(uid, getTask(uid)?.fileId || latest.fileId)
          return
        }
        return
      }

      const lastChunk = chunks - 1
      if (nextChunk < lastChunk) {
        await uploadChunkWindow(uid, nextChunk, lastChunk - 1, chunks)
      }

      const afterWindow = getTask(uid)
      if (!afterWindow || afterWindow.pause || afterWindow.status === 'fail') {
        return
      }

      if (afterWindow.chunkIndex <= lastChunk) {
        const last = await uploadSingleChunk(uid, lastChunk, chunks)
        if (last?.status === 'upload_seconds') {
          patchTask(uid, { status: 'upload_seconds', uploadProgress: 100, uploadSize: afterWindow.totalSize })
          onUploadDone?.()
          eventBus.emit('data:reload')
          return
        }
        if (last?.status === 'upload_finish') {
          startTransferWatch(uid, getTask(uid)?.fileId || afterWindow.fileId)
        }
      }
    } finally {
      patchTask(uid, { uploading: false })
    }
  }, [getTask, onUploadDone, patchTask, startTransferWatch, uploadChunkWindow, uploadSingleChunk])

  const addFile = useCallback(async (file: File, filePid: string) => {
    const task = createTask(file, filePid)
    setTasks((prev) => [task, ...prev])

    if (file.size === 0) {
      return
    }

    const md5 = await computeMd5(task.uid)
    if (!md5) {
      return
    }

    await checkResumeChunks(task.uid)
    await uploadTask(task.uid)
  }, [checkResumeChunks, computeMd5, uploadTask])

  useEffect(() => {
    const off = eventBus.on<{ file: File; filePid: string }>('upload:add', (payload) => {
      if (!payload?.file) {
        return
      }
      void addFile(payload.file, payload.filePid || '0')
    })
    return off
  }, [addFile])

  const activeTaskCount = useMemo(
    () =>
      tasks.filter((task) => ['init', 'uploading', 'retrying', 'transferring'].includes(task.status)).length,
    [tasks]
  )

  useEffect(() => {
    onActiveTaskCountChange?.(activeTaskCount)
  }, [activeTaskCount, onActiveTaskCountChange])

  const retryAllFailed = () => {
    tasks
      .filter((task) => ['fail', 'network_error', 'auth_error', 'server_error'].includes(task.status))
      .forEach((task) => {
        patchTask(task.uid, { pause: false, status: 'uploading', errorMsg: undefined })
        void uploadTask(task.uid, task.chunkIndex)
      })
  }

  const clearCompleted = () => {
    setTasks((prev) => prev.filter((task) => !['upload_finish', 'upload_seconds', 'transfer_done'].includes(task.status)))
  }

  return (
    <Drawer
      open={open}
      title="上传任务"
      width={720}
      onClose={onClose}
      extra={
        <Space>
          <Button onClick={retryAllFailed} icon={<ReloadOutlined />}>
            重试失败
          </Button>
          <Button onClick={clearCompleted}>清理完成</Button>
        </Space>
      }
    >
      <List
        dataSource={tasks}
        locale={{ emptyText: '暂无上传任务' }}
        renderItem={(task) => {
          const meta = statusMeta[task.status] || statusMeta.fail
          return (
            <List.Item
              actions={[
                task.status === 'uploading' ? (
                  <Button
                    key="pause"
                    icon={<PauseCircleOutlined />}
                    onClick={() => patchTask(task.uid, { pause: true })}
                  />
                ) : (
                  <Button
                    key="resume"
                    icon={<PlayCircleOutlined />}
                    disabled={['upload_finish', 'upload_seconds', 'transfer_done', 'emptyfile'].includes(task.status)}
                    onClick={() => {
                      patchTask(task.uid, { pause: false })
                      void uploadTask(task.uid, task.chunkIndex)
                    }}
                  />
                ),
                <Button key="delete" danger icon={<DeleteOutlined />} onClick={() => removeTask(task.uid)} />,
              ]}
            >
              <Flex vertical style={{ width: '100%' }} gap={6}>
                <Flex justify="space-between" align="center">
                  <Text strong ellipsis={{ tooltip: task.fileName }}>
                    {task.fileName}
                  </Text>
                  <Tag color={meta.color}>{task.status === 'fail' && task.errorMsg ? task.errorMsg : meta.text}</Tag>
                </Flex>

                {task.status === 'init' ? (
                  <Progress percent={task.md5Progress} size="small" status="active" />
                ) : (
                  <Progress percent={task.uploadProgress} size="small" status={task.status === 'fail' ? 'exception' : 'active'} />
                )}

                <Text type="secondary">
                  {sizeToText(task.uploadSize)} / {sizeToText(task.totalSize)}
                  {task.isResume ? ' · 断点续传' : ''}
                </Text>
              </Flex>
            </List.Item>
          )
        }}
      />

      <input
        id="hidden-manual-upload"
        type="file"
        multiple
        style={{ display: 'none' }}
        onChange={(event) => {
          const files = Array.from(event.target.files || [])
          files.forEach((file) => {
            void addFile(file, '0')
          })
          event.currentTarget.value = ''
        }}
      />

      {tasks.length === 0 ? (
        <Button
          type="dashed"
          block
          style={{ marginTop: 16 }}
          onClick={() => {
            document.getElementById('hidden-manual-upload')?.click()
          }}
        >
          添加本地文件
        </Button>
      ) : null}
    </Drawer>
  )
}

