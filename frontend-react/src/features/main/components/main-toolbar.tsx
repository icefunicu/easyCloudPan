import { useEffect, useMemo, useRef, useState } from 'react'
import {
  Button,
  Dropdown,
  Flex,
  Input,
  Segmented,
  Space,
  Upload,
} from 'antd'
import type { InputRef } from 'antd/es/input'
import type { MenuProps } from 'antd'
import {
  FolderAddOutlined,
  MoreOutlined,
  PlusOutlined,
  ReloadOutlined,
} from '@ant-design/icons'

// Props for the MainToolbar component
export interface MainToolbarProps {
  isMobile: boolean
  category: string
  categoryAccept: string
  viewMode: 'list' | 'grid'
  onViewModeChange: (mode: 'list' | 'grid') => void
  onSearchSubmit: (value: string) => void
  onRefresh: () => void
  onAddFiles: (files: File[]) => void
  onCreateFolder: () => void
}

export const MainToolbar = ({
  isMobile,
  category,
  categoryAccept,
  viewMode,
  onViewModeChange,
  onSearchSubmit,
  onRefresh,
  onAddFiles,
  onCreateFolder,
}: MainToolbarProps) => {
  const searchInputRef = useRef<InputRef>(null)
  const [internalSearch, setInternalSearch] = useState('')

  useEffect(() => {
    const timer = window.setTimeout(() => {
      onSearchSubmit(internalSearch)
    }, 280) // SEARCH_DEBOUNCE_MS
    return () => window.clearTimeout(timer)
  }, [internalSearch, onSearchSubmit])

  const mobileActionItems = useMemo<NonNullable<MenuProps['items']>>(() => {
    const items: NonNullable<MenuProps['items']> = [
      {
        key: 'refresh',
        label: '刷新列表',
        icon: <ReloadOutlined />,
        onClick: onRefresh,
      },
    ]
    if (category === 'all') {
      items.push({
        key: 'create-folder',
        label: '新建目录',
        icon: <FolderAddOutlined />,
        onClick: onCreateFolder,
      })
    }
    return items
  }, [category, onCreateFolder, onRefresh])

  // Handle global shortcuts for search
  useEffect(() => {
    const handleShortcuts = (event: KeyboardEvent) => {
      const target = event.target as HTMLElement | null
      const editableTag = target?.tagName === 'INPUT' || target?.tagName === 'TEXTAREA'
      const contentEditable = Boolean(target?.isContentEditable)
      const inEditable = editableTag || contentEditable

      if ((event.key === '/' || (event.key.toLowerCase() === 'k' && (event.metaKey || event.ctrlKey))) && !inEditable) {
        event.preventDefault()
        searchInputRef.current?.focus()
      }
    }
    window.addEventListener('keydown', handleShortcuts)
    return () => {
      window.removeEventListener('keydown', handleShortcuts)
    }
  }, [])

  return (
    <Flex vertical gap={10} className="main-toolbar">
      <Flex justify="space-between" align="center" wrap="wrap" gap={10} className="main-toolbar-row main-toolbar-row-primary">
        <Space wrap className="main-toolbar-search">
          <Input.Search
            ref={searchInputRef}
            allowClear
            placeholder="搜索文件名"
            value={internalSearch}
            onChange={(event) => setInternalSearch(event.target.value)}
            onSearch={(value) => onSearchSubmit(value)}
            className="main-search"
          />

          {!isMobile && (
            <Segmented
              className="main-view-switch"
              value={viewMode}
              onChange={(value) => onViewModeChange(value as 'list' | 'grid')}
              options={[
                { label: '列表', value: 'list' },
                { label: '网格', value: 'grid' },
              ]}
            />
          )}

          {!isMobile && (
            <Button className="btn-secondary-soft btn-refresh btn-toolbar" icon={<ReloadOutlined />} onClick={onRefresh}>
              刷新
            </Button>
          )}
        </Space>

        <Space wrap className="main-toolbar-actions main-toolbar-primary-actions">
          <Upload
            multiple
            beforeUpload={(file) => {
              onAddFiles([file])
              return false
            }}
            showUploadList={false}
            accept={categoryAccept}
          >
            <Button type="primary" className="btn-primary-lift btn-toolbar" icon={<PlusOutlined />}>
              上传文件
            </Button>
          </Upload>

          {isMobile ? (
            <Dropdown menu={{ items: mobileActionItems }} trigger={['click']}>
              <Button className="btn-secondary-soft btn-toolbar" icon={<MoreOutlined />}>
                更多
              </Button>
            </Dropdown>
          ) : category === 'all' ? (
            <Button className="btn-secondary-soft btn-toolbar" icon={<FolderAddOutlined />} onClick={onCreateFolder}>
              新建目录
            </Button>
          ) : null}
        </Space>
      </Flex>

      {!isMobile && (
        <Flex justify="space-between" align="center" wrap="wrap" gap={10} className="main-toolbar-row">
          <div className="main-selection-tip">
            支持拖拽上传、批量操作（快捷键：/ 快速搜索，Esc 清空选择，Delete 批量删除）
          </div>
        </Flex>
      )}
    </Flex>
  )
}
