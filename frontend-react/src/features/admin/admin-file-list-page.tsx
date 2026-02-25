import { type Key, useCallback, useEffect, useMemo, useState } from 'react'
import { App, Button, Card, Dropdown, Empty, Flex, Grid, Input, Space, Table } from 'antd'
import type { MenuProps } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { DeleteOutlined, DownloadOutlined, MoreOutlined, ReloadOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import {
  createAdminDownloadUrl,
  deleteAdminFile,
  getAdminDownloadBaseUrl,
  getAdminFolderInfo,
  loadAdminFileList,
} from '@/services/admin-service'
import type { FileInfoVO, FolderVO } from '@/types'
import { sizeToText } from '@/lib/format'
import { resolveDownloadTarget } from '@/lib/download'
import { FileIcon, FolderNavigation, TableSkeleton } from '@/components/common'
import { PreviewModal } from '@/components/preview'
import { useSearchParams } from 'react-router-dom'

const SEARCH_DEBOUNCE_MS = 280

interface FileRecord extends FileInfoVO {
  key: string
}

const parsePath = (rawPath: string | null): string[] => {
  if (!rawPath) {
    return []
  }
  return rawPath
    .split('/')
    .map((item) => item.trim())
    .filter(Boolean)
}

export const AdminFileListPage = () => {
  const { modal, message } = App.useApp()
  const screens = Grid.useBreakpoint()
  const isMobile = !screens.md
  const [searchParams, setSearchParams] = useSearchParams()

  const [loading, setLoading] = useState(false)
  const [keywordInput, setKeywordInput] = useState('')
  const [keyword, setKeyword] = useState('')
  const [folders, setFolders] = useState<FolderVO[]>([])
  const [selectedRowKeys, setSelectedRowKeys] = useState<Key[]>([])
  const [previewFile, setPreviewFile] = useState<FileInfoVO | null>(null)

  const [tableData, setTableData] = useState<{ list: FileInfoVO[]; pageNo: number; pageSize: number; totalCount: number }>({
    list: [],
    pageNo: 1,
    pageSize: 15,
    totalCount: 0,
  })

  const pathIds = parsePath(searchParams.get('path'))
  const currentFolderId = pathIds[pathIds.length - 1] || '0'

  const gotoPath = useCallback(
    (ids: string[]) => {
      if (!ids.length) {
        setSearchParams({})
        return
      }
      setSearchParams({ path: ids.join('/') })
    },
    [setSearchParams]
  )

  const refreshList = useCallback(async () => {
    setLoading(true)
    try {
      const data = await loadAdminFileList({
        pageNo: tableData.pageNo,
        pageSize: tableData.pageSize,
        fileNameFuzzy: keyword || undefined,
        filePid: currentFolderId,
      })
      if (data) {
        setTableData({
          list: data.list,
          pageNo: data.pageNo,
          pageSize: data.pageSize,
          totalCount: data.totalCount,
        })
      }
    } finally {
      setLoading(false)
    }
  }, [currentFolderId, keyword, tableData.pageNo, tableData.pageSize])

  const refreshFolderPath = useCallback(async () => {
    const rawPath = searchParams.get('path')
    if (!rawPath) {
      setFolders([])
      return
    }
    const data = await getAdminFolderInfo(rawPath)
    setFolders(data || [])
  }, [searchParams])

  useEffect(() => {
    void refreshList()
  }, [refreshList])

  useEffect(() => {
    void refreshFolderPath()
  }, [refreshFolderPath])

  useEffect(() => {
    const currentIds = new Set(tableData.list.map((item) => `${item.userId}_${item.fileId}`))
    setSelectedRowKeys((prev) => prev.filter((id) => currentIds.has(String(id))))
  }, [tableData.list])

  useEffect(() => {
    const timer = window.setTimeout(() => {
      const nextKeyword = keywordInput.trim()
      if (nextKeyword === keyword) {
        return
      }
      setKeyword(nextKeyword)
      setTableData((prev) => ({ ...prev, pageNo: 1 }))
    }, SEARCH_DEBOUNCE_MS)
    return () => {
      window.clearTimeout(timer)
    }
  }, [keyword, keywordInput])

  const columns = useMemo<ColumnsType<FileRecord>>(
    () => [
      {
        title: '文件',
        dataIndex: 'fileName',
        key: 'fileName',
        render: (_value, record) => (
          <Flex align="center" gap={8}>
            <FileIcon folderType={record.folderType} fileType={record.fileType} fileCategory={record.fileCategory} />
            <a
              onClick={() => {
                if (record.folderType === 1) {
                  gotoPath([...pathIds, record.fileId])
                  return
                }
                if (record.status !== 2) {
                  message.warning('文件未转码完成')
                  return
                }
                setPreviewFile(record)
              }}
            >
              {record.fileName}
            </a>
          </Flex>
        ),
      },
      {
        title: '所属用户',
        dataIndex: 'nickName',
        key: 'nickName',
        width: 160,
      },
      {
        title: '更新时间',
        dataIndex: 'lastUpdateTime',
        key: 'lastUpdateTime',
        width: 190,
        render: (value) => dayjs(value).format('YYYY-MM-DD HH:mm:ss'),
      },
      {
        title: '大小',
        dataIndex: 'fileSize',
        key: 'fileSize',
        width: 120,
        render: (value) => sizeToText(value),
      },
      {
        title: '操作',
        key: 'action',
        width: 160,
        render: (_value, record) => (
          <Space className="table-row-actions">
            <Button
              className="btn-secondary-soft btn-small-polish btn-icon-round"
              size="small"
              icon={<DownloadOutlined />}
              disabled={record.folderType === 1}
              onClick={async () => {
                const code = await createAdminDownloadUrl(record.userId || '', record.fileId)
                if (code) {
                  window.location.href = resolveDownloadTarget(getAdminDownloadBaseUrl(), code)
                }
              }}
            />
            <Button
              className="btn-danger-soft btn-small-polish btn-icon-round"
              size="small"
              icon={<DeleteOutlined />}
              onClick={() => {
                modal.confirm({
                  title: '删除文件',
                  content: `确认彻底删除 ${record.fileName} 吗？`,
                  onOk: async () => {
                    const ok = await deleteAdminFile(`${record.userId}_${record.fileId}`)
                    if (ok) {
                      message.success('删除成功')
                      await refreshList()
                    }
                  },
                })
              }}
            />
          </Space>
        ),
      },
    ],
    [gotoPath, message, modal, pathIds, refreshList]
  )

  const confirmBatchDelete = useCallback(() => {
    if (!selectedRowKeys.length) {
      return
    }
    modal.confirm({
      title: '批量删除',
      content: `确认删除 ${selectedRowKeys.length} 项吗？`,
      onOk: async () => {
        const ok = await deleteAdminFile(selectedRowKeys.map(String).join(','))
        if (ok) {
          message.success('删除成功')
          setSelectedRowKeys([])
          await refreshList()
        }
      },
    })
  }, [message, modal, refreshList, selectedRowKeys])

  const mobileMenuItems = useMemo<NonNullable<MenuProps['items']>>(
    () => [
      {
        key: 'refresh',
        label: '刷新列表',
        icon: <ReloadOutlined />,
        onClick: () => void refreshList(),
      },
      {
        key: 'delete',
        label: '批量删除',
        icon: <DeleteOutlined />,
        danger: true,
        disabled: selectedRowKeys.length === 0,
        onClick: confirmBatchDelete,
      },
    ],
    [confirmBatchDelete, refreshList, selectedRowKeys.length]
  )

  return (
    <div className="page-shell">
      <div className="page-toolbar">
        <div className="page-title-group">
          <h3 className="page-title">用户文件管理</h3>
          <p className="page-subtitle">全局查看用户文件，支持路径钻取和快速审计处理</p>
        </div>
        <div className="page-chip-row">
          <span className="page-chip">
            当前页 <strong>{tableData.list.length}</strong>
          </span>
          <span className="page-chip">
            总文件 <strong>{tableData.totalCount}</strong>
          </span>
          <span className="page-chip">
            已选 <strong>{selectedRowKeys.length}</strong>
          </span>
        </div>
      </div>

      <Card className="glass-card table-card">
        <Flex justify="space-between" wrap="wrap" gap={10} style={{ marginBottom: 10 }}>
          <Space>
            <Input
              allowClear
              placeholder="搜索文件"
              style={{ width: isMobile ? '100%' : 260 }}
              value={keywordInput}
              onChange={(event) => setKeywordInput(event.target.value)}
            />
            {!isMobile ? (
              <Button className="btn-secondary-soft btn-refresh btn-toolbar" icon={<ReloadOutlined />} onClick={() => void refreshList()}>
                刷新
              </Button>
            ) : null}
          </Space>

          <div className="page-toolbar-actions">
            {isMobile ? (
              <Dropdown menu={{ items: mobileMenuItems }} trigger={['click']}>
                <Button className="btn-secondary-soft btn-toolbar" icon={<MoreOutlined />}>
                  操作
                </Button>
              </Dropdown>
            ) : (
              <Button className="btn-danger-soft btn-toolbar" icon={<DeleteOutlined />} disabled={selectedRowKeys.length === 0} onClick={confirmBatchDelete}>
                批量删除
              </Button>
            )}
          </div>
        </Flex>

        <FolderNavigation
          folders={folders}
          onBackParent={() => gotoPath(pathIds.slice(0, -1))}
          onJumpRoot={() => gotoPath([])}
          onJump={(index) => gotoPath(pathIds.slice(0, index + 1))}
        />

        {loading && tableData.list.length === 0 ? (
          <TableSkeleton rows={7} />
        ) : (
          <Table<FileRecord>
            style={{ marginTop: 12 }}
            rowKey={(record) => `${record.userId}_${record.fileId}`}
            loading={loading}
            dataSource={tableData.list.map((item) => ({ ...item, key: `${item.userId}_${item.fileId}` }))}
            columns={columns}
            scroll={{ x: 'max-content' }}
            rowSelection={{ selectedRowKeys, onChange: setSelectedRowKeys }}
            locale={{ emptyText: <Empty description="当前目录暂无文件" /> }}
            pagination={{
              current: tableData.pageNo,
              pageSize: tableData.pageSize,
              total: tableData.totalCount,
              showSizeChanger: !isMobile,
              onChange: (pageNo, pageSize) => {
                setTableData((prev) => ({ ...prev, pageNo, pageSize }))
              },
            }}
          />
        )}
      </Card>

      <PreviewModal open={Boolean(previewFile)} file={previewFile} mode="admin" onClose={() => setPreviewFile(null)} />
    </div>
  )
}
