import { useEffect, useMemo, useState } from 'react'
import {
  AppstoreAddOutlined,
  LockOutlined,
  MenuOutlined,
  PoweroffOutlined,
  ReloadOutlined,
  SettingOutlined,
  UserOutlined,
} from '@ant-design/icons'
import { Avatar, Badge, Button, Dropdown, Flex, Grid, Layout, Progress, Space, Tooltip } from 'antd'
import { motion } from 'framer-motion'
import { useNavigate } from 'react-router-dom'
import { useSpaceMonitor } from '@/features/profile/use-space-monitor'
import { UpdateAvatarModal } from '@/features/profile/update-avatar-modal'
import { UpdateNickNameModal } from '@/features/profile/update-nickname-modal'
import { UpdatePasswordModal } from '@/features/profile/update-password-modal'
import { sizeToText } from '@/lib/format'
import { logout } from '@/services/account-service'
import { useAuthStore } from '@/store/auth-store'
import { useUiStore } from '@/store/ui-store'
import { UploaderDrawer } from '@/components/uploader/uploader-drawer'

const { Header } = Layout

export const AppHeader = () => {
  const screens = Grid.useBreakpoint()
  const compactHeader = !screens.lg
  const navigate = useNavigate()
  const user = useAuthStore((state) => state.user)
  const clearUser = useAuthStore((state) => state.clearUser)
  const setMobileMenuOpen = useUiStore((state) => state.setMobileMenuOpen)
  const uploaderOpen = useUiStore((state) => state.uploaderOpen)
  const setUploaderOpen = useUiStore((state) => state.setUploaderOpen)

  const [avatarModalOpen, setAvatarModalOpen] = useState(false)
  const [nickModalOpen, setNickModalOpen] = useState(false)
  const [passwordModalOpen, setPasswordModalOpen] = useState(false)
  const [activeTaskCount, setActiveTaskCount] = useState(0)
  const [online, setOnline] = useState(() => (typeof navigator === 'undefined' ? true : navigator.onLine))

  const { spaceInfo, usedPercent, loadSpace, refreshing, ensureAutoRefresh, remainSpaceText } = useSpaceMonitor()

  const spaceLevel = useMemo(() => {
    if (usedPercent < 70) return 'normal'
    if (usedPercent < 90) return 'exception'
    return 'active'
  }, [usedPercent])

  const avatarSrc = useMemo(() => {
    if (!user?.userId) {
      return undefined
    }
    const rawAvatar = (user.avatar || '').trim()
    if (/^https?:\/\//i.test(rawAvatar)) {
      return rawAvatar
    }
    if (rawAvatar.startsWith('/')) {
      return rawAvatar
    }
    return `/api/getAvatar/${user.userId}`
  }, [user?.avatar, user?.userId])

  useEffect(() => {
    const handleOnline = () => setOnline(true)
    const handleOffline = () => setOnline(false)
    window.addEventListener('online', handleOnline)
    window.addEventListener('offline', handleOffline)
    return () => {
      window.removeEventListener('online', handleOnline)
      window.removeEventListener('offline', handleOffline)
    }
  }, [])

  useEffect(() => {
    if (activeTaskCount <= 0) {
      return
    }
    const handleBeforeUnload = (event: BeforeUnloadEvent) => {
      event.preventDefault()
      event.returnValue = ''
    }
    window.addEventListener('beforeunload', handleBeforeUnload)
    return () => {
      window.removeEventListener('beforeunload', handleBeforeUnload)
    }
  }, [activeTaskCount])

  const profileMenu = {
    items: [
      {
        key: 'avatar',
        label: '修改头像',
        icon: <UserOutlined />,
        onClick: () => setAvatarModalOpen(true),
      },
      {
        key: 'nickname',
        label: '修改昵称',
        icon: <SettingOutlined />,
        onClick: () => setNickModalOpen(true),
      },
      {
        key: 'password',
        label: '修改密码',
        icon: <LockOutlined />,
        onClick: () => setPasswordModalOpen(true),
      },
      {
        key: 'logout',
        label: '退出登录',
        icon: <PoweroffOutlined />,
        onClick: async () => {
          await logout()
          clearUser()
          navigate('/login')
        },
      },
    ],
  }

  return (
    <>
      <Header className="app-header">
        <Flex align="center" justify="space-between" style={{ width: '100%' }}>
          <Space size={12} align="center">
            <Button
              type="text"
              className="mobile-menu-trigger"
              icon={<MenuOutlined />}
              onClick={() => setMobileMenuOpen(true)}
            />
            <motion.div className="brand-logo" whileHover={{ y: -1 }} onClick={() => navigate('/main/all')}>
              <div className="logo-badge">云</div>
              <div className="logo-name">EasyCloudPan</div>
            </motion.div>
          </Space>

          <Space size={12} align="center">
            <Tooltip title={online ? '网络连接正常' : '网络离线，部分操作将稍后重试'}>
              <div className={`net-chip ${online ? 'online' : 'offline'}`}>
                <span className="dot" />
                <span>{online ? '在线' : '离线'}</span>
              </div>
            </Tooltip>

            <Tooltip
              title={`已用 ${sizeToText(spaceInfo.useSpace)} / 总量 ${sizeToText(spaceInfo.totalSpace)}，剩余 ${remainSpaceText}`}
            >
              <div className={`space-chip level-${spaceLevel}`}>
                <span className="label">空间</span>
                <span className="value">{usedPercent}%</span>
                <Progress percent={usedPercent} size={[74, 4]} showInfo={false} status="active" />
                <Button
                  type="text"
                  size="small"
                  icon={<ReloadOutlined className={refreshing ? 'spin' : ''} />}
                  onClick={() => void loadSpace()}
                />
              </div>
            </Tooltip>

            <Tooltip title={activeTaskCount > 0 ? `进行中的上传任务：${activeTaskCount}` : '打开上传任务面板'}>
              <Badge count={activeTaskCount} size="small" offset={[-4, 4]}>
                <Button
                  type="default"
                  icon={<AppstoreAddOutlined />}
                  className="header-upload-btn btn-secondary-soft"
                  onClick={() => setUploaderOpen(true)}
                >
                  {compactHeader ? '上传' : '上传任务'}
                </Button>
              </Badge>
            </Tooltip>

            <Dropdown menu={profileMenu} trigger={['click']}>
              <motion.div whileHover={{ y: -1 }} className="profile-trigger">
                <Avatar size={36} src={avatarSrc}>
                  {user?.nickName?.slice(0, 1) || 'U'}
                </Avatar>
                <span>{user?.nickName || '未登录'}</span>
              </motion.div>
            </Dropdown>
          </Space>
        </Flex>
      </Header>

      <UploaderDrawer
        open={uploaderOpen}
        onClose={() => setUploaderOpen(false)}
        onActiveTaskCountChange={(count) => {
          setActiveTaskCount(count)
          ensureAutoRefresh(count)
        }}
        onUploadDone={() => {
          void loadSpace()
        }}
      />

      <UpdateAvatarModal open={avatarModalOpen} onClose={() => setAvatarModalOpen(false)} onSuccess={() => void loadSpace()} />
      <UpdateNickNameModal open={nickModalOpen} onClose={() => setNickModalOpen(false)} />
      <UpdatePasswordModal open={passwordModalOpen} onClose={() => setPasswordModalOpen(false)} />
    </>
  )
}
