import { Button, Empty } from 'antd'
import { useNavigate } from 'react-router-dom'

export const NotFoundPage = () => {
  const navigate = useNavigate()
  return (
    <div className="app-page" style={{ display: 'grid', placeItems: 'center' }}>
      <div>
        <Empty description="页面不存在" />
        <Button type="primary" block onClick={() => navigate('/main/all')}>
          回到首页
        </Button>
      </div>
    </div>
  )
}

