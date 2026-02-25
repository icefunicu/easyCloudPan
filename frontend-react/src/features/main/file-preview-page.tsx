import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Empty, Spin } from 'antd'
import { getFileInfoById } from '@/services/file-service'
import type { FileInfoVO } from '@/types'
import { PreviewModal } from '@/components/preview'

export const FilePreviewPage = () => {
  const navigate = useNavigate()
  const { fileId = '' } = useParams<{ fileId: string }>()
  const [loading, setLoading] = useState(true)
  const [file, setFile] = useState<FileInfoVO | null>(null)

  useEffect(() => {
    const run = async () => {
      setLoading(true)
      const data = await getFileInfoById(fileId)
      setFile(data)
      setLoading(false)
    }

    void run()
  }, [fileId])

  if (loading) {
    return (
      <div className="app-page" style={{ display: 'grid', placeItems: 'center' }}>
        <Spin />
      </div>
    )
  }

  if (!file) {
    return (
      <div className="app-page" style={{ display: 'grid', placeItems: 'center' }}>
        <Empty description="文件不存在或已删除" />
      </div>
    )
  }

  return (
    <PreviewModal
      open
      file={file}
      mode="user"
      onClose={() => {
        navigate('/main/all')
      }}
    />
  )
}

