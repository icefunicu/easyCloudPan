import { useEffect, useState } from 'react'
import { App, Button, Card, Form, Input, Typography } from 'antd'
import { useNavigate, useParams } from 'react-router-dom'
import { checkShareCode, getShareInfo } from '@/services/share-service'
import type { ShareInfoVO } from '@/types'

const { Title, Text } = Typography

interface FormValue {
  code: string
}

export const ShareCheckPage = () => {
  const { message } = App.useApp()
  const navigate = useNavigate()
  const { shareId = '' } = useParams<{ shareId: string }>()
  const [form] = Form.useForm<FormValue>()
  const [info, setInfo] = useState<ShareInfoVO | null>(null)

  useEffect(() => {
    const run = async () => {
      const data = await getShareInfo(shareId)
      setInfo(data)
    }
    void run()
  }, [shareId])

  return (
    <div className="auth-page app-page" style={{ display: 'grid', placeItems: 'center' }}>
      <Card className="glass-card" style={{ width: 540 }}>
        <Title level={4}>分享提取验证</Title>
        {info ? (
          <Text type="secondary">
            {info.nickName} 分享了文件：{info.fileName}
          </Text>
        ) : (
          <Text type="secondary">正在加载分享信息...</Text>
        )}

        <Form
          form={form}
          layout="vertical"
          style={{ marginTop: 16 }}
          onFinish={async (values) => {
            const ok = await checkShareCode(shareId, values.code)
            if (ok) {
              navigate(`/share/${shareId}`)
            } else {
              message.error('提取码错误')
            }
          }}
        >
          <Form.Item
            label="提取码"
            name="code"
            rules={[{ required: true, message: '请输入提取码' }, { len: 5, message: '提取码为5位' }]}
          >
            <Input maxLength={5} placeholder="请输入5位提取码" />
          </Form.Item>

          <Button type="primary" htmlType="submit" block>
            提取文件
          </Button>
        </Form>
      </Card>
    </div>
  )
}

