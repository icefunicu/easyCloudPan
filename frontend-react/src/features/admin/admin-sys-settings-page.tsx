import { useEffect, useState } from 'react'
import { App, Button, Card, Flex, Form, Input, Typography } from 'antd'
import type { SysSettingsDto } from '@/types'
import { getSysSettings, saveSysSettings } from '@/services/admin-service'

const { Text } = Typography

export const AdminSysSettingsPage = () => {
  const { message } = App.useApp()
  const [form] = Form.useForm<SysSettingsDto>()
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    const run = async () => {
      setLoading(true)
      const data = await getSysSettings()
      if (data) {
        form.setFieldsValue(data)
      }
      setLoading(false)
    }

    void run()
  }, [form])

  return (
    <div className="page-shell">
      <div className="page-toolbar">
        <div className="page-title-group">
          <h3 className="page-title">系统设置</h3>
          <p className="page-subtitle">配置注册邮件模板和用户初始容量，提交后立即生效</p>
        </div>
      </div>

      <Card className="glass-card table-card" style={{ maxWidth: 860 }}>
        <Form
          form={form}
          layout="vertical"
          disabled={loading}
          onFinish={async (values) => {
            setSaving(true)
            const ok = await saveSysSettings({
              registerEmailTitle: values.registerEmailTitle,
              registerEmailContent: values.registerEmailContent,
              userInitUseSpace: Number(values.userInitUseSpace),
            })
            setSaving(false)
            if (ok) {
              message.success('设置已保存')
            }
          }}
        >
          <Form.Item
            label="注册邮件标题"
            name="registerEmailTitle"
            extra="示例：欢迎注册 EasyCloudPan"
            rules={[{ required: true, message: '请输入注册邮件标题' }]}
          >
            <Input />
          </Form.Item>

          <Form.Item
            label="注册邮件内容"
            name="registerEmailContent"
            extra="支持纯文本模板内容，用于发送注册验证码邮件"
            rules={[{ required: true, message: '请输入注册邮件内容' }]}
          >
            <Input.TextArea rows={6} />
          </Form.Item>

          <Form.Item
            label="初始空间大小（MB）"
            name="userInitUseSpace"
            extra="新用户注册后的默认总空间配额"
            rules={[{ required: true, message: '请输入初始空间大小' }]}
          >
            <Input type="number" min={1} />
          </Form.Item>

          <Flex justify="space-between" align="center" wrap="wrap" gap={12}>
            <Text type="secondary">配置保存后会影响后续新用户注册流程</Text>
            <Button type="primary" className="btn-primary-lift btn-toolbar" htmlType="submit" loading={saving}>
              保存设置
            </Button>
          </Flex>
        </Form>
      </Card>
    </div>
  )
}
