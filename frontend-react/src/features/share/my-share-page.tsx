import { type Key, useCallback, useEffect, useMemo, useState } from 'react'
import { App, Button, Card, Dropdown, Empty, Flex, Grid, Input, Space, Table, Tag } from 'antd'
import type { MenuProps } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import { CopyOutlined, DeleteOutlined, MoreOutlined, SearchOutlined } from '@ant-design/icons'
import { cancelShare, getShareUrl, loadShareList } from '@/services/share-service'
import { TableSkeleton } from '@/components/common'
import { copyText } from '@/lib/clipboard'
import type { FileShare } from '@/types'

const SEARCH_DEBOUNCE_MS = 280

interface ShareRecord extends FileShare {
  key: string
}

const validTypeLabel: Record<number, string> = {
  0: '1天',
  1: '7天',
  2: '30天',
  3: '永久',
}

const resolveShareStatus = (record: FileShare): 'valid' | 'expired' => {
  if (record.status === 1 || record.status === 2) {
    return 'expired'
  }
  if (record.validType !== 3 && record.expireTime && dayjs(record.expireTime).isBefore(dayjs())) {
    return 'expired'
  }
  return 'valid'
}

export const MySharePage = () => {
  const { modal, message } = App.useApp()
  const screens = Grid.useBreakpoint()
  const isMobile = !screens.md

  const [loading, setLoading] = useState(false)
  const [selectedRowKeys, setSelectedRowKeys] = useState<Key[]>([])
  const [searchInput, setSearchInput] = useState('')
  const [searchKeyword, setSearchKeyword] = useState('')
  const [tableData, setTableData] = useState<{ list: FileShare[]; pageNo: number; pageSize: number; totalCount: number }>({
    list: [],
    pageNo: 1,
    pageSize: 15,
    totalCount: 0,
  })

  const validCount = useMemo(() => tableData.list.filter((item) => resolveShareStatus(item) === 'valid').length, [tableData.list])

  const refreshList = useCallback(async () => {
    setLoading(true)
    try {
      const data = await loadShareList({
        pageNo: tableData.pageNo,
        pageSize: tableData.pageSize,
        fileNameFuzzy: searchKeyword || undefined,
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
  }, [searchKeyword, tableData.pageNo, tableData.pageSize])

  useEffect(() => {
    void refreshList()
  }, [refreshList])

  useEffect(() => {
    const currentIds = new Set(tableData.list.map((item) => item.shareId))
    setSelectedRowKeys((prev) => prev.filter((id) => currentIds.has(String(id))))
  }, [tableData.list])

  useEffect(() => {
    const timer = window.setTimeout(() => {
      const nextKeyword = searchInput.trim()
      if (nextKeyword === searchKeyword) {
        return
      }
      setSearchKeyword(nextKeyword)
      setTableData((prev) => ({ ...prev, pageNo: 1 }))
    }, SEARCH_DEBOUNCE_MS)
    return () => {
      window.clearTimeout(timer)
    }
  }, [searchInput, searchKeyword])

  const doCancelShare = useCallback(
    async (ids: string[]) => {
      const ok = await cancelShare(ids.join(','))
      if (ok) {
        message.success('分享已取消')
        setSelectedRowKeys([])
        await refreshList()
      }
    },
    [message, refreshList]
  )

  const copyShareLink = useCallback(
    async (record: FileShare) => {
      const urlInfo = await getShareUrl(record.shareId)
      const shareUrl = urlInfo?.shareUrl || `${window.location.origin}/share/${record.shareId}`
      const content = `链接：${shareUrl} 提取码：${urlInfo?.code || record.code}`
      const copied = await copyText(content)
      if (copied) {
        message.success('分享信息已复制')
        return
      }
      message.warning('复制失败，请手动复制')
    },
    [message]
  )

  const confirmBatchCancel = useCallback(() => {
    if (!selectedRowKeys.length) {
      return
    }
    modal.confirm({
      title: '批量取消分享',
      content: `确认取消 ${selectedRowKeys.length} 项分享吗？`,
      onOk: async () => {
        await doCancelShare(selectedRowKeys.map(String))
      },
    })
  }, [doCancelShare, modal, selectedRowKeys])

  const columns = useMemo<ColumnsType<ShareRecord>>(
    () => [
      {
        title: '文件',
        dataIndex: 'fileName',
        key: 'fileName',
      },
      {
        title: '分享时间',
        dataIndex: 'shareTime',
        key: 'shareTime',
        width: 200,
        render: (value) => dayjs(value).format('YYYY-MM-DD HH:mm:ss'),
      },
      {
        title: '过期时间',
        dataIndex: 'expireTime',
        key: 'expireTime',
        width: 200,
        render: (_value, record) => (record.validType === 3 ? '永久有效' : record.expireTime || '-'),
      },
      {
        title: '浏览',
        dataIndex: 'showCount',
        key: 'showCount',
        width: 90,
      },
      {
        title: '有效期',
        dataIndex: 'validType',
        key: 'validType',
        width: 110,
        render: (value) => <Tag>{validTypeLabel[value] || value}</Tag>,
      },
      {
        title: '状态',
        key: 'status',
        width: 100,
        render: (_value, record) => {
          const status = resolveShareStatus(record)
          return status === 'expired' ? <Tag color="red">已失效</Tag> : <Tag color="green">有效</Tag>
        },
      },
      {
        title: '操作',
        key: 'action',
        width: 160,
        render: (_value, record) => (
          <Space className="table-row-actions">
            <Button className="btn-secondary-soft btn-small-polish" size="small" icon={<CopyOutlined />} onClick={() => void copyShareLink(record)}>
              复制
            </Button>
            <Button
              className="btn-danger-soft btn-small-polish"
              size="small"
              icon={<DeleteOutlined />}
              onClick={() => {
                modal.confirm({
                  title: '取消分享',
                  content: `确认取消 ${record.fileName} 的分享吗？`,
                  onOk: async () => {
                    await doCancelShare([record.shareId])
                  },
                })
              }}
            >
              取消
            </Button>
          </Space>
        ),
      },
    ],
    [copyShareLink, doCancelShare, modal]
  )

  const mobileMenuItems = useMemo<NonNullable<MenuProps['items']>>(
    () => [
      {
        key: 'cancel',
        label: '批量取消分享',
        icon: <DeleteOutlined />,
        danger: true,
        disabled: selectedRowKeys.length === 0,
        onClick: confirmBatchCancel,
      },
      {
        key: 'clear',
        label: '清空选择',
        disabled: selectedRowKeys.length === 0,
        onClick: () => setSelectedRowKeys([]),
      },
    ],
    [confirmBatchCancel, selectedRowKeys.length]
  )

  return (
    <div className="page-shell">
      <div className="page-toolbar">
        <div className="page-title-group">
          <h3 className="page-title">我的分享</h3>
          <p className="page-subtitle">统一管理分享链接，支持快速复制与批量失效处理</p>
        </div>
        <div className="page-chip-row">
          <span className="page-chip">
            总分享 <strong>{tableData.totalCount}</strong>
          </span>
          <span className="page-chip">
            有效 <strong>{validCount}</strong>
          </span>
          <span className="page-chip">
            已选 <strong>{selectedRowKeys.length}</strong>
          </span>
        </div>
      </div>

      <Card className="glass-card table-card">
        <Flex justify="space-between" align="center" style={{ marginBottom: 12 }} wrap="wrap" gap={12}>
          <Space wrap>
            <Input
              allowClear
              prefix={<SearchOutlined />}
              placeholder="搜索分享文件"
              style={{ width: isMobile ? '100%' : 260 }}
              value={searchInput}
              onChange={(event) => setSearchInput(event.target.value)}
            />
          </Space>

          <div className="page-toolbar-actions">
            {isMobile ? (
              <Dropdown menu={{ items: mobileMenuItems }} trigger={['click']}>
                <Button className="btn-secondary-soft btn-toolbar" icon={<MoreOutlined />}>
                  操作
                </Button>
              </Dropdown>
            ) : (
              <Button className="btn-danger-soft btn-toolbar" icon={<DeleteOutlined />} disabled={selectedRowKeys.length === 0} onClick={confirmBatchCancel}>
                批量取消
              </Button>
            )}
          </div>
        </Flex>

        {loading && tableData.list.length === 0 ? (
          <TableSkeleton rows={7} />
        ) : (
          <Table<ShareRecord>
            rowKey="shareId"
            loading={loading}
            dataSource={tableData.list.map((item) => ({ ...item, key: item.shareId }))}
            columns={columns}
            scroll={{ x: 'max-content' }}
            rowSelection={{ selectedRowKeys, onChange: setSelectedRowKeys }}
            locale={{ emptyText: <Empty description="暂无分享记录" /> }}
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
    </div>
  )
}
