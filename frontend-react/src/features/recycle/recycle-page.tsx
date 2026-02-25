import { type Key, useCallback, useEffect, useMemo, useState } from 'react'
import { App, Button, Card, Dropdown, Empty, Flex, Grid, Space, Table, Tag, Typography } from 'antd'
import type { MenuProps } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import { DeleteOutlined, MoreOutlined, RollbackOutlined } from '@ant-design/icons'
import { deleteRecycleFile, loadRecycleList, recoverFile } from '@/services/recycle-service'
import type { FileInfoVO } from '@/types'
import { FileIcon, TableSkeleton } from '@/components/common'
import { sizeToText } from '@/lib/format'

const { Text } = Typography

interface RecycleRecord extends FileInfoVO {
  key: string
}

export const RecyclePage = () => {
  const { message, modal } = App.useApp()
  const screens = Grid.useBreakpoint()
  const isMobile = !screens.md

  const [loading, setLoading] = useState(false)
  const [selectedRowKeys, setSelectedRowKeys] = useState<Key[]>([])
  const [tableData, setTableData] = useState<{ list: FileInfoVO[]; pageNo: number; pageSize: number; totalCount: number }>({
    list: [],
    pageNo: 1,
    pageSize: 15,
    totalCount: 0,
  })

  const refreshList = useCallback(async () => {
    setLoading(true)
    try {
      const data = await loadRecycleList({ pageNo: tableData.pageNo, pageSize: tableData.pageSize })
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
  }, [tableData.pageNo, tableData.pageSize])

  useEffect(() => {
    void refreshList()
  }, [refreshList])

  useEffect(() => {
    const currentIds = new Set(tableData.list.map((item) => item.fileId))
    setSelectedRowKeys((prev) => prev.filter((id) => currentIds.has(String(id))))
  }, [tableData.list])

  const columns = useMemo<ColumnsType<RecycleRecord>>(
    () => [
      {
        title: '文件',
        dataIndex: 'fileName',
        key: 'fileName',
        render: (_value, record) => (
          <Flex align="center" gap={8}>
            <FileIcon folderType={record.folderType} fileType={record.fileType} fileCategory={record.fileCategory} />
            <span>{record.fileName}</span>
          </Flex>
        ),
      },
      {
        title: '删除时间',
        dataIndex: 'recoveryTime',
        key: 'recoveryTime',
        width: 200,
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
        title: '状态',
        key: 'status',
        width: 100,
        render: () => <Tag color="red">已删除</Tag>,
      },
      {
        title: '操作',
        key: 'action',
        width: 180,
        render: (_value, record) => (
          <Space className="table-row-actions">
            <Button
              className="btn-secondary-soft btn-small-polish"
              icon={<RollbackOutlined />}
              size="small"
              onClick={async () => {
                const ok = await recoverFile(record.fileId)
                if (ok) {
                  message.success('已恢复')
                  await refreshList()
                }
              }}
            >
              恢复
            </Button>
            <Button
              className="btn-danger-soft btn-small-polish"
              icon={<DeleteOutlined />}
              size="small"
              onClick={async () => {
                const ok = await deleteRecycleFile(record.fileId)
                if (ok) {
                  message.success('已彻底删除')
                  await refreshList()
                }
              }}
            >
              删除
            </Button>
          </Space>
        ),
      },
    ],
    [message, refreshList]
  )

  const doRecoverBatch = useCallback(() => {
    if (!selectedRowKeys.length) {
      return
    }
    modal.confirm({
      title: '批量恢复',
      content: `确认恢复 ${selectedRowKeys.length} 个文件吗？`,
      onOk: async () => {
        const ok = await recoverFile(selectedRowKeys.map(String).join(','))
        if (ok) {
          message.success('恢复成功')
          setSelectedRowKeys([])
          await refreshList()
        }
      },
    })
  }, [message, modal, refreshList, selectedRowKeys])

  const doDeleteBatch = useCallback(() => {
    if (!selectedRowKeys.length) {
      return
    }
    modal.confirm({
      title: '批量彻底删除',
      content: `确认彻底删除 ${selectedRowKeys.length} 个文件吗？该操作不可恢复。`,
      onOk: async () => {
        const ok = await deleteRecycleFile(selectedRowKeys.map(String).join(','))
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
        key: 'recover',
        label: '批量恢复',
        icon: <RollbackOutlined />,
        disabled: selectedRowKeys.length === 0,
        onClick: doRecoverBatch,
      },
      {
        key: 'delete',
        label: '批量彻底删除',
        icon: <DeleteOutlined />,
        danger: true,
        disabled: selectedRowKeys.length === 0,
        onClick: doDeleteBatch,
      },
    ],
    [doDeleteBatch, doRecoverBatch, selectedRowKeys.length]
  )

  return (
    <div className="page-shell">
      <div className="page-toolbar">
        <div className="page-title-group">
          <h3 className="page-title">回收站</h3>
          <p className="page-subtitle">文件可恢复，彻底删除将无法回滚</p>
        </div>
        <div className="page-chip-row">
          <span className="page-chip">
            待处理 <strong>{tableData.totalCount}</strong>
          </span>
          <span className="page-chip">
            已选 <strong>{selectedRowKeys.length}</strong>
          </span>
        </div>
      </div>

      <Card className="glass-card table-card">
        <Flex justify="space-between" style={{ marginBottom: 12 }} wrap="wrap" gap={10}>
          <Text strong>最近删除文件</Text>
          <div className="page-toolbar-actions">
            {isMobile ? (
              <Dropdown menu={{ items: mobileMenuItems }} trigger={['click']}>
                <Button className="btn-secondary-soft btn-toolbar" icon={<MoreOutlined />}>
                  操作
                </Button>
              </Dropdown>
            ) : (
              <div className="action-rack">
                <Button className="btn-secondary-soft btn-toolbar" icon={<RollbackOutlined />} disabled={selectedRowKeys.length === 0} onClick={doRecoverBatch}>
                  批量恢复
                </Button>
                <Button className="btn-danger-soft btn-toolbar" icon={<DeleteOutlined />} disabled={selectedRowKeys.length === 0} onClick={doDeleteBatch}>
                  批量彻底删除
                </Button>
              </div>
            )}
          </div>
        </Flex>

        {loading && tableData.list.length === 0 ? (
          <TableSkeleton rows={7} />
        ) : (
          <Table<RecycleRecord>
            rowKey="fileId"
            loading={loading}
            dataSource={tableData.list.map((item) => ({ ...item, key: item.fileId }))}
            columns={columns}
            scroll={{ x: 'max-content' }}
            rowSelection={{ selectedRowKeys, onChange: setSelectedRowKeys }}
            locale={{ emptyText: <Empty description="回收站为空" /> }}
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
