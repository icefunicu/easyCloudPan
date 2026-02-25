import { useEffect, useMemo, useRef, useState } from 'react'
import { Alert, Button, Modal, Spin, Table, Tabs, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type { FileInfoVO } from '@/types'
import { createDownloadUrl, getDownloadBaseUrl } from '@/services/file-service'
import { createAdminDownloadUrl, getAdminDownloadBaseUrl } from '@/services/admin-service'
import { createShareDownloadUrl, getShareDownloadBaseUrl } from '@/services/share-service'
import { resolveDownloadTarget } from '@/lib/download'
import { useAuthStore } from '@/store/auth-store'

const { Text } = Typography

type PreviewMode = 'user' | 'admin' | 'share'

interface PreviewModalProps {
  open: boolean
  file: FileInfoVO | null
  mode: PreviewMode
  shareId?: string
  onClose: () => void
}

interface ExcelSheetData {
  sheetName: string
  columns: ColumnsType<Record<string, string>>
  dataSource: Record<string, string>[]
}

const extractCoverParts = (cover?: string): { folder: string; name: string } | null => {
  if (!cover) {
    return null
  }
  const normalized = cover.replaceAll('_.', '.')
  const segments = normalized.split('/').filter(Boolean)
  if (segments.length < 2) {
    return null
  }
  return {
    folder: segments[0],
    name: segments.slice(1).join('/'),
  }
}

const withApiPrefix = (path: string): string => (path.startsWith('/api') ? path : `/api${path}`)

const useResourcePath = (file: FileInfoVO | null, mode: PreviewMode, shareId?: string) => {
  return useMemo(() => {
    if (!file) {
      return null
    }

    const coverParts = extractCoverParts(file.fileCover)
    if (mode === 'admin') {
      return {
        filePath: withApiPrefix(`/admin/getFile/${file.userId}/${file.fileId}`),
        videoPath: withApiPrefix(`/admin/ts/getVideoInfo/${file.userId}/${file.fileId}`),
        imagePath: coverParts ? withApiPrefix(`/admin/getImage/${file.userId}/${coverParts.folder}/${coverParts.name}`) : '',
      }
    }

    if (mode === 'share') {
      return {
        filePath: withApiPrefix(`/showShare/getFile/${shareId}/${file.fileId}`),
        videoPath: withApiPrefix(`/showShare/ts/getVideoInfo/${shareId}/${file.fileId}`),
        imagePath: coverParts ? withApiPrefix(`/showShare/getImage/${shareId}/${coverParts.folder}/${coverParts.name}`) : '',
      }
    }

    return {
      filePath: withApiPrefix(`/file/getFile/${file.fileId}`),
      videoPath: withApiPrefix(`/file/ts/getVideoInfo/${file.fileId}`),
      imagePath: coverParts ? withApiPrefix(`/file/getImage/${coverParts.folder}/${coverParts.name}`) : '',
    }
  }, [file, mode, shareId])
}

const loadTextContent = async (path: string, token?: string): Promise<string> => {
  const response = await fetch(path, {
    method: 'GET',
    credentials: 'include',
    headers: token ? { Authorization: `Bearer ${token}` } : undefined,
  })
  if (!response.ok) {
    throw new Error('文本预览加载失败')
  }
  return response.text()
}

const loadExcel = async (path: string, token?: string): Promise<ExcelSheetData[]> => {
  const xlsx = await import('xlsx')
  const response = await fetch(path, {
    method: 'GET',
    credentials: 'include',
    headers: token ? { Authorization: `Bearer ${token}` } : undefined,
  })

  if (!response.ok) {
    throw new Error('Excel 预览加载失败')
  }

  const arrayBuffer = await response.arrayBuffer()
  const workbook = xlsx.read(arrayBuffer, { type: 'array' })
  return workbook.SheetNames.map((sheetName) => {
    const sheet = workbook.Sheets[sheetName]
    const rows = xlsx.utils.sheet_to_json<string[]>(sheet, { header: 1 }) as string[][]
    const header = rows[0] || []
    const bodyRows = rows.slice(1)

    const columns: ColumnsType<Record<string, string>> = header.map((column, index) => ({
      title: column || `列${index + 1}`,
      dataIndex: `c${index}`,
      key: `c${index}`,
      ellipsis: true,
      width: 160,
    }))

    const dataSource = bodyRows.map((row, rowIndex) => {
      const record: Record<string, string> = { key: `${rowIndex}` }
      row.forEach((cell, columnIndex) => {
        record[`c${columnIndex}`] = cell || ''
      })
      return record
    })

    return {
      sheetName,
      columns,
      dataSource,
    }
  })
}

export const PreviewModal = ({ open, file, mode, shareId, onClose }: PreviewModalProps) => {
  const token = useAuthStore((state) => state.user?.token)
  const [loading, setLoading] = useState(false)
  const [textContent, setTextContent] = useState('')
  const [excelSheets, setExcelSheets] = useState<ExcelSheetData[]>([])
  const [error, setError] = useState('')
  const docContainerRef = useRef<HTMLDivElement | null>(null)

  const paths = useResourcePath(file, mode, shareId)

  useEffect(() => {
    if (!open || !file || !paths) {
      setTextContent('')
      setExcelSheets([])
      setError('')
      if (docContainerRef.current) {
        docContainerRef.current.innerHTML = ''
      }
      return
    }

    if (file.fileType !== 7 && file.fileType !== 8 && file.fileType !== 6 && file.fileType !== 5) {
      if (docContainerRef.current) {
        docContainerRef.current.innerHTML = ''
      }
      return
    }

    setLoading(true)
    setError('')

    const run = async () => {
      try {
        if (file.fileType === 6) {
          const sheets = await loadExcel(paths.filePath, token)
          setExcelSheets(sheets)
        } else if (file.fileType === 5) {
          const response = await fetch(paths.filePath, {
            method: 'GET',
            credentials: 'include',
            headers: token ? { Authorization: `Bearer ${token}` } : undefined,
          })
          if (!response.ok) {
            throw new Error('Word 预览加载失败')
          }
          const arrayBuffer = await response.arrayBuffer()
          if (docContainerRef.current) {
            const { renderAsync } = await import('docx-preview')
            docContainerRef.current.innerHTML = ''
            await renderAsync(arrayBuffer, docContainerRef.current, undefined, {
              className: 'docx-preview',
              inWrapper: true,
            })
          }
        } else {
          const content = await loadTextContent(paths.filePath, token)
          setTextContent(content)
        }
      } catch (err) {
        setError(err instanceof Error ? err.message : '预览失败')
      } finally {
        setLoading(false)
      }
    }

    void run()
  }, [file, open, paths, token])

  const downloadCurrentFile = async () => {
    if (!file) {
      return
    }

    let code: string | null = null
    let baseUrl = ''

    if (mode === 'admin') {
      code = await createAdminDownloadUrl(file.userId || '', file.fileId)
      baseUrl = getAdminDownloadBaseUrl()
    } else if (mode === 'share') {
      code = await createShareDownloadUrl(shareId || '', file.fileId)
      baseUrl = getShareDownloadBaseUrl()
    } else {
      code = await createDownloadUrl(file.fileId)
      baseUrl = getDownloadBaseUrl()
    }

    if (!code) {
      return
    }

    window.location.href = resolveDownloadTarget(baseUrl, code)
  }

  return (
    <Modal
      open={open}
      title={file?.fileName || '文件预览'}
      onCancel={onClose}
      width={1080}
      footer={
        <Button type="primary" onClick={downloadCurrentFile}>
          下载文件
        </Button>
      }
      styles={{ body: { minHeight: 480 } }}
    >
      {!file || !paths ? null : (
        <div style={{ minHeight: 420 }}>
          {loading ? (
            <div style={{ display: 'grid', placeItems: 'center', height: 360 }}>
              <Spin />
            </div>
          ) : error ? (
            <Alert type="error" message={error} />
          ) : file.fileCategory === 3 ? (
            <div style={{ textAlign: 'center' }}>
              <img src={paths.imagePath} alt={file.fileName} style={{ maxWidth: '100%', maxHeight: 520 }} />
            </div>
          ) : file.fileCategory === 1 ? (
            <video controls style={{ width: '100%', maxHeight: 520 }} src={paths.videoPath} />
          ) : file.fileCategory === 2 ? (
            <audio controls style={{ width: '100%' }} src={paths.filePath} />
          ) : file.fileType === 4 ? (
            <iframe title={file.fileName} src={paths.filePath} width="100%" height="520" />
          ) : file.fileType === 7 || file.fileType === 8 ? (
            <pre
              style={{
                background: '#0e1720',
                color: '#d5e6f0',
                borderRadius: 12,
                padding: 12,
                maxHeight: 520,
                overflow: 'auto',
              }}
            >
              {textContent}
            </pre>
          ) : file.fileType === 6 ? (
            <Tabs
              items={excelSheets.map((sheet) => ({
                key: sheet.sheetName,
                label: sheet.sheetName,
                children: (
                  <Table
                    size="small"
                    scroll={{ x: 'max-content', y: 420 }}
                    dataSource={sheet.dataSource}
                    columns={sheet.columns}
                    pagination={false}
                  />
                ),
              }))}
            />
          ) : file.fileType === 5 ? (
            <div
              ref={docContainerRef}
              style={{
                borderRadius: 12,
                border: '1px solid rgba(130, 150, 160, 0.3)',
                padding: 12,
                maxHeight: 520,
                overflow: 'auto',
                background: '#ffffff',
              }}
            />
          ) : (
            <Alert
              type="info"
              message="该文件类型暂不支持在线预览"
              description={<Text>你可以直接下载后在本地打开</Text>}
            />
          )}
        </div>
      )}
    </Modal>
  )
}

