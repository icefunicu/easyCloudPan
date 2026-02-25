import { useEffect, useState } from 'react'
import { Avatar, Button, Modal, Upload } from 'antd'
import type { UploadProps } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { message } from 'antd'
import { useAuthStore } from '@/store/auth-store'
import { updateUserAvatar } from '@/services/account-service'

interface UpdateAvatarModalProps {
  open: boolean
  onClose: () => void
  onSuccess?: () => void
}

export const UpdateAvatarModal = ({ open, onClose, onSuccess }: UpdateAvatarModalProps) => {
  const user = useAuthStore((state) => state.user)
  const patchUser = useAuthStore((state) => state.patchUser)
  const [file, setFile] = useState<File | null>(null)
  const [preview, setPreview] = useState<string>('')
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    if (!open) {
      setFile(null)
      setPreview('')
    }
  }, [open])

  const uploadProps: UploadProps = {
    beforeUpload: (rawFile) => {
      setFile(rawFile)
      setPreview(URL.createObjectURL(rawFile))
      return false
    },
    showUploadList: false,
    maxCount: 1,
  }

  return (
    <Modal
      title="修改头像"
      open={open}
      onCancel={onClose}
      confirmLoading={submitting}
      onOk={async () => {
        if (!file) {
          onClose()
          return
        }
        setSubmitting(true)
        const ok = await updateUserAvatar(file)
        setSubmitting(false)
        if (ok) {
          if (user?.userId) {
            patchUser({ avatar: `/api/getAvatar/${user.userId}?v=${Date.now()}` })
          }
          message.success('头像已更新')
          onClose()
          onSuccess?.()
        }
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: 18 }}>
        <Avatar size={72} src={preview || (user ? `/api/getAvatar/${user.userId}?v=${Date.now()}` : undefined)}>
          {user?.nickName?.slice(0, 1)}
        </Avatar>

        <Upload {...uploadProps} accept="image/*">
          <Button icon={<PlusOutlined />}>选择图片</Button>
        </Upload>
      </div>
    </Modal>
  )
}

