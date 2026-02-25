import { useEffect, useMemo, useState } from 'react'
import { App, Button, Input, Modal, Radio, Space, Typography } from 'antd'
import type { FileInfoVO, ShareValidType } from '@/types'
import { getShareUrl, shareFile } from '@/services/share-service'
import { copyText } from '@/lib/clipboard'

const { Text } = Typography

interface ShareModalProps {
  open: boolean
  file: FileInfoVO | null
  onClose: () => void
}

const validTypeOptions: Array<{ label: string; value: ShareValidType }> = [
  { label: '1天', value: 0 },
  { label: '7天', value: 1 },
  { label: '30天', value: 2 },
  { label: '永久', value: 3 },
]

const buildShareContent = (shareUrl: string, code: string): string => `链接：${shareUrl} 提取码：${code}`

export const ShareModal = ({ open, file, onClose }: ShareModalProps) => {
  const { message } = App.useApp()
  const [validType, setValidType] = useState<ShareValidType>(1)
  const [extractCode, setExtractCode] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [shareResult, setShareResult] = useState<{ shareUrl: string; code: string } | null>(null)

  const normalizedExtractCode = useMemo(() => extractCode.trim(), [extractCode])

  useEffect(() => {
    if (!open) {
      return
    }
    setValidType(1)
    setExtractCode('')
    setShareResult(null)
  }, [open, file?.fileId])

  const handleGenerate = async () => {
    if (!file) {
      return
    }
    if (normalizedExtractCode && !/^[A-Za-z0-9]{4,8}$/.test(normalizedExtractCode)) {
      message.warning('提取码需为 4-8 位字母或数字')
      return
    }

    setSubmitting(true)
    try {
      const created = await shareFile({
        fileId: file.fileId,
        validType,
        code: normalizedExtractCode || undefined,
      })
      if (!created) {
        return
      }
      const urlInfo = await getShareUrl(created.shareId)
      const shareUrl = urlInfo?.shareUrl || `${window.location.origin}/share/${created.shareId}`
      const finalCode = urlInfo?.code || created.code || normalizedExtractCode || ''
      setShareResult({ shareUrl, code: finalCode })
      message.success('分享链接已生成')
    } finally {
      setSubmitting(false)
    }
  }

  const handleCopy = async () => {
    if (!shareResult) {
      return
    }
    const copied = await copyText(buildShareContent(shareResult.shareUrl, shareResult.code))
    if (copied) {
      message.success('分享信息已复制')
      return
    }
    message.warning('复制失败，请手动复制')
  }

  return (
    <Modal
      open={open}
      onCancel={onClose}
      title={shareResult ? '分享链接' : `分享文件：${file?.fileName || ''}`}
      confirmLoading={submitting}
      okText={shareResult ? '复制链接' : '创建分享'}
      cancelText={shareResult ? '关闭' : '取消'}
      onOk={shareResult ? () => void handleCopy() : () => void handleGenerate()}
    >
      {shareResult ? (
        <Space direction="vertical" size={12} style={{ width: '100%' }}>
          <Text type="secondary">分享链接</Text>
          <Input readOnly value={shareResult.shareUrl} />
          <Text type="secondary">提取码</Text>
          <Input readOnly value={shareResult.code} />
          <Button type="default" onClick={() => void handleCopy()}>
            复制完整分享信息
          </Button>
        </Space>
      ) : (
        <Space direction="vertical" size={14} style={{ width: '100%' }}>
          <div>
            <Text type="secondary">有效期</Text>
            <div style={{ marginTop: 8 }}>
              <Radio.Group
                optionType="button"
                buttonStyle="solid"
                value={validType}
                options={validTypeOptions}
                onChange={(event) => setValidType(event.target.value as ShareValidType)}
              />
            </div>
          </div>
          <div>
            <Text type="secondary">提取码（可选）</Text>
            <Input
              value={extractCode}
              maxLength={8}
              placeholder="留空将自动生成"
              onChange={(event) => setExtractCode(event.target.value)}
              style={{ marginTop: 8 }}
            />
          </div>
        </Space>
      )}
    </Modal>
  )
}
