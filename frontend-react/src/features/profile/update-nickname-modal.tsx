import { useEffect } from 'react'
import { Form, Input, Modal, message } from 'antd'
import { useAuthStore } from '@/store/auth-store'
import { updateNickName } from '@/services/account-service'

interface UpdateNickNameModalProps {
  open: boolean
  onClose: () => void
}

interface FormValue {
  nickName: string
}

export const UpdateNickNameModal = ({ open, onClose }: UpdateNickNameModalProps) => {
  const [form] = Form.useForm<FormValue>()
  const user = useAuthStore((state) => state.user)
  const patchUser = useAuthStore((state) => state.patchUser)

  useEffect(() => {
    if (open) {
      form.setFieldsValue({ nickName: user?.nickName || '' })
    }
  }, [form, open, user?.nickName])

  return (
    <Modal
      title="修改昵称"
      open={open}
      onCancel={onClose}
      onOk={async () => {
        const values = await form.validateFields()
        const ok = await updateNickName(values.nickName)
        if (ok) {
          patchUser({ nickName: values.nickName })
          message.success('昵称已更新')
          onClose()
        }
      }}
    >
      <Form form={form} layout="vertical">
        <Form.Item label="昵称" name="nickName" rules={[{ required: true, message: '请输入昵称' }, { max: 20, message: '长度不能超过20' }]}> 
          <Input maxLength={20} placeholder="请输入昵称" />
        </Form.Item>
      </Form>
    </Modal>
  )
}

