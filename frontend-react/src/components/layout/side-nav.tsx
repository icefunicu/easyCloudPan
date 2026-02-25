import { Menu } from 'antd'
import type { MenuProps } from 'antd'
import { CloudOutlined, DeleteOutlined, FolderOpenOutlined, ShareAltOutlined, SettingOutlined } from '@ant-design/icons'
import { useMemo } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useAuthStore } from '@/store/auth-store'
import { useUiStore } from '@/store/ui-store'

const MAIN_CHILDREN = [
  { key: '/main/all', label: '全部' },
  { key: '/main/video', label: '视频' },
  { key: '/main/music', label: '音频' },
  { key: '/main/image', label: '图片' },
  { key: '/main/doc', label: '文档' },
  { key: '/main/others', label: '其他' },
]

export const SideNav = () => {
  const navigate = useNavigate()
  const location = useLocation()
  const user = useAuthStore((state) => state.user)
  const setMobileMenuOpen = useUiStore((state) => state.setMobileMenuOpen)

  const items = useMemo<MenuProps['items']>(() => {
    const base: MenuProps['items'] = [
      {
        key: 'main-group',
        label: '首页',
        icon: <CloudOutlined />,
        children: MAIN_CHILDREN,
      },
      {
        key: '/myshare',
        label: '我的分享',
        icon: <ShareAltOutlined />,
      },
      {
        key: '/recycle',
        label: '回收站',
        icon: <DeleteOutlined />,
      },
    ]

    if (user?.admin) {
      base.push({
        key: 'admin-group',
        label: '管理端',
        icon: <SettingOutlined />,
        children: [
          { key: '/settings/fileList', label: '用户文件' },
          { key: '/settings/userList', label: '用户管理' },
          { key: '/settings/sysSetting', label: '系统设置' },
        ],
      })
    }

    return base
  }, [user?.admin])

  const selectedKeys = useMemo(() => {
    if (location.pathname.startsWith('/main/')) {
      return [location.pathname]
    }
    return [location.pathname]
  }, [location.pathname])

  return (
    <div className="side-nav-wrap">
      <Menu
        mode="inline"
        selectedKeys={selectedKeys}
        defaultOpenKeys={['main-group', 'admin-group']}
        items={items}
        expandIcon={<FolderOpenOutlined style={{ fontSize: 12 }} />}
        onClick={({ key }) => {
          navigate(String(key))
          setMobileMenuOpen(false)
        }}
      />
    </div>
  )
}

