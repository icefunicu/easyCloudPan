import { Form, Input, Modal, message } from 'antd'
import { updatePassword } from '@/services/account-service'
import { passwordError } from '@/lib/validators'

interface UpdatePasswordModalProps {
  open: boolean
  onClose: () => void
}

interface FormValue {
  password: string
  confirmPassword: string
}

export const UpdatePasswordModal = ({ open, onClose }: UpdatePasswordModalProps) => {
  const [form] = Form.useForm<FormValue>()

  return (
    <Modal
      title="修改密码"
      open={open}
      onCancel={onClose}
      onOk={async () => {
        const values = await form.validateFields()
        const ok = await updatePassword(values.password)
        if (ok) {
          message.success('密码已更新')
          form.resetFields()
          onClose()
        }
      }}
      afterOpenChange={(visible) => {
        if (!visible) {
          form.resetFields()
        }
      }}
    >
      <Form form={form} layout="vertical">
        <Form.Item
          name="password"
          label="新密码"
          rules={[
            {
              required: true,
              validator: (_, value: string) => {
                const error = passwordError(value || '')
                return error ? Promise.reject(new Error(error)) : Promise.resolve()
              },
            },
          ]}
        >
          <Input.Password placeholder="请输入新密码" />
        </Form.Item>

        <Form.Item
          name="confirmPassword"
          label="确认密码"
          dependencies={['password']}
          rules={[
            { required: true, message: '请再次输入密码' },
            ({ getFieldValue }) => ({
              validator(_, value) {
                if (!value || getFieldValue('password') === value) {
                  return Promise.resolve()
                }
                return Promise.reject(new Error('两次输入的密码不一致'))
              },
            }),
          ]}
        >
          <Input.Password placeholder="请再次输入密码" />
        </Form.Item>
      </Form>
    </Modal>
  )
}

