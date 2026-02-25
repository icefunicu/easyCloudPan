import { type Key, useCallback, useEffect, useMemo, useState } from 'react'
import {
  App,
  Avatar,
  Button,
  Card,
  Dropdown,
  Empty,
  Flex,
  Form,
  Grid,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  Typography,
} from 'antd'
import type { MenuProps } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import { MoreOutlined } from '@ant-design/icons'
import { TableSkeleton } from '@/components/common'
import { UserInfoVO } from '@/types'
import { loadUserList, setUserSpace, updateUserSpace, updateUserStatus } from '@/services/admin-service'
import { sizeToText } from '@/lib/format'

const { Text } = Typography

const SEARCH_DEBOUNCE_MS = 260

interface UserRecord extends UserInfoVO {
  key: string
}

export const AdminUserListPage = () => {
  const { message, modal } = App.useApp()
  const screens = Grid.useBreakpoint()
  const isMobile = !screens.md

  const [loading, setLoading] = useState(false)
  const [selectedRowKeys, setSelectedRowKeys] = useState<Key[]>([])
  const [query, setQuery] = useState<{ nickNameFuzzy?: string; status?: number }>({})
  const [searchInput, setSearchInput] = useState('')
  const [tableData, setTableData] = useState<{ list: UserInfoVO[]; pageNo: number; pageSize: number; totalCount: number }>({
    list: [],
    pageNo: 1,
    pageSize: 15,
    totalCount: 0,
  })

  const [spaceModalOpen, setSpaceModalOpen] = useState(false)
  const [spaceLoading, setSpaceLoading] = useState(false)
  const [editingUser, setEditingUser] = useState<UserInfoVO | null>(null)
  const [spaceForm] = Form.useForm<{ operation: 'set' | 'add' | 'subtract'; value: number; unit: 'MB' | 'GB' | 'TB' }>()

  const enabledCount = useMemo(() => tableData.list.filter((item) => item.status === 1).length, [tableData.list])

  const refreshList = useCallback(async () => {
    setLoading(true)
    try {
      const data = await loadUserList({
        pageNo: tableData.pageNo,
        pageSize: tableData.pageSize,
        ...query,
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
  }, [query, tableData.pageNo, tableData.pageSize])

  useEffect(() => {
    void refreshList()
  }, [refreshList])

  useEffect(() => {
    const currentIds = new Set(tableData.list.map((item) => item.userId))
    setSelectedRowKeys((prev) => prev.filter((id) => currentIds.has(String(id))))
  }, [tableData.list])

  useEffect(() => {
    const timer = window.setTimeout(() => {
      const keyword = searchInput.trim()
      if (query.nickNameFuzzy === keyword) {
        return
      }
      setQuery((prev) => ({ ...prev, nickNameFuzzy: keyword || undefined }))
      setTableData((prev) => ({ ...prev, pageNo: 1 }))
    }, SEARCH_DEBOUNCE_MS)
    return () => {
      window.clearTimeout(timer)
    }
  }, [query.nickNameFuzzy, searchInput])

  const doBatchStatus = useCallback(
    (status: number) => {
      if (!selectedRowKeys.length) {
        return
      }
      modal.confirm({
        title: status === 1 ? '批量启用用户' : '批量禁用用户',
        content: `确认操作 ${selectedRowKeys.length} 个用户吗？`,
        onOk: async () => {
          const result = await Promise.all(selectedRowKeys.map((userId) => updateUserStatus(String(userId), status)))
          const successCount = result.filter(Boolean).length
          if (successCount > 0) {
            message.success('状态更新成功')
            setSelectedRowKeys([])
            await refreshList()
            if (successCount !== selectedRowKeys.length) {
              message.warning(`部分用户更新失败（成功 ${successCount}/${selectedRowKeys.length}）`)
            }
            return
          }
          message.error('批量操作失败')
        },
      })
    },
    [message, modal, refreshList, selectedRowKeys]
  )

  const columns = useMemo<ColumnsType<UserRecord>>(
    () => [
      {
        title: '用户ID',
        dataIndex: 'userId',
        key: 'userId',
        width: 180,
      },
      {
        title: '头像',
        key: 'avatar',
        width: 84,
        render: (_value, record) => (
          <Avatar src={record.qqAvatar || `/api/getAvatar/${record.userId}`}>
            {record.nickName?.slice(0, 1)}
          </Avatar>
        ),
      },
      {
        title: '昵称',
        dataIndex: 'nickName',
        key: 'nickName',
      },
      {
        title: '邮箱',
        dataIndex: 'email',
        key: 'email',
      },
      {
        title: '空间',
        key: 'space',
        width: 220,
        render: (_value, record) => `${sizeToText(record.useSpace)} / ${sizeToText(record.totalSpace)}`,
      },
      {
        title: '注册时间',
        dataIndex: 'joinTime',
        key: 'joinTime',
        width: 180,
        render: (value) => dayjs(value).format('YYYY-MM-DD HH:mm'),
      },
      {
        title: '最后登录',
        dataIndex: 'lastLoginTime',
        key: 'lastLoginTime',
        width: 180,
        render: (value) => (value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-'),
      },
      {
        title: '状态',
        dataIndex: 'status',
        key: 'status',
        width: 90,
        render: (value) => (value === 1 ? <Tag color="green">启用</Tag> : <Tag color="red">禁用</Tag>),
      },
      {
        title: '操作',
        key: 'actions',
        width: 200,
        render: (_value, record) => (
          <Space className="table-row-actions">
            <Button
              className="btn-secondary-soft btn-small-polish"
              size="small"
              onClick={async () => {
                const ok = await updateUserStatus(record.userId, record.status === 1 ? 0 : 1)
                if (ok) {
                  message.success('状态更新成功')
                  await refreshList()
                }
              }}
            >
              {record.status === 1 ? '禁用' : '启用'}
            </Button>
            <Button
              className="btn-secondary-soft btn-small-polish"
              size="small"
              onClick={() => {
                setEditingUser(record)
                setSpaceModalOpen(true)
                spaceForm.setFieldsValue({ operation: 'set', value: 5, unit: 'GB' })
              }}
            >
              空间管理
            </Button>
          </Space>
        ),
      },
    ],
    [message, refreshList, spaceForm]
  )

  const mobileMenuItems = useMemo<NonNullable<MenuProps['items']>>(
    () => [
      {
        key: 'enable',
        label: '批量启用',
        disabled: selectedRowKeys.length === 0,
        onClick: () => doBatchStatus(1),
      },
      {
        key: 'disable',
        label: '批量禁用',
        danger: true,
        disabled: selectedRowKeys.length === 0,
        onClick: () => doBatchStatus(0),
      },
    ],
    [doBatchStatus, selectedRowKeys.length]
  )

  return (
    <div className="page-shell">
      <div className="page-toolbar">
        <div className="page-title-group">
          <h3 className="page-title">用户管理</h3>
          <p className="page-subtitle">支持状态批量变更与用户配额精细管理</p>
        </div>
        <div className="page-chip-row">
          <span className="page-chip">
            总用户 <strong>{tableData.totalCount}</strong>
          </span>
          <span className="page-chip">
            启用 <strong>{enabledCount}</strong>
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
              placeholder="按昵称搜索"
              style={{ width: isMobile ? '100%' : 240 }}
              value={searchInput}
              onChange={(event) => setSearchInput(event.target.value)}
            />
            <Select
              allowClear
              placeholder="状态"
              style={{ width: 120 }}
              onChange={(value) => {
                setQuery((prev) => ({ ...prev, status: value }))
                setTableData((prev) => ({ ...prev, pageNo: 1 }))
              }}
              options={[
                { label: '启用', value: 1 },
                { label: '禁用', value: 0 },
              ]}
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
              <div className="action-rack">
                <Button className="btn-secondary-soft btn-toolbar" onClick={() => doBatchStatus(1)} disabled={selectedRowKeys.length === 0}>
                  批量启用
                </Button>
                <Button className="btn-danger-soft btn-toolbar" onClick={() => doBatchStatus(0)} disabled={selectedRowKeys.length === 0}>
                  批量禁用
                </Button>
              </div>
            )}
          </div>
        </Flex>

        {loading && tableData.list.length === 0 ? (
          <TableSkeleton rows={7} />
        ) : (
          <Table<UserRecord>
            rowKey="userId"
            loading={loading}
            dataSource={tableData.list.map((item) => ({ ...item, key: item.userId }))}
            columns={columns}
            scroll={{ x: 'max-content' }}
            rowSelection={{ selectedRowKeys, onChange: setSelectedRowKeys }}
            locale={{ emptyText: <Empty description="暂无用户数据" /> }}
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

      <Modal
        title="空间管理"
        open={spaceModalOpen}
        confirmLoading={spaceLoading}
        onCancel={() => setSpaceModalOpen(false)}
        onOk={async () => {
          const values = await spaceForm.validateFields()
          if (!editingUser) {
            return
          }
          setSpaceLoading(true)

          let valueMB = Number(values.value)
          if (values.unit === 'GB') valueMB *= 1024
          if (values.unit === 'TB') valueMB *= 1024 * 1024

          let ok = false
          if (values.operation === 'set') {
            ok = await setUserSpace(editingUser.userId, Math.floor(valueMB))
          } else {
            const delta = values.operation === 'add' ? Math.floor(valueMB) : -Math.floor(valueMB)
            ok = await updateUserSpace(editingUser.userId, delta)
          }

          setSpaceLoading(false)
          if (ok) {
            message.success('空间更新成功')
            setSpaceModalOpen(false)
            await refreshList()
          }
        }}
      >
        {editingUser ? (
          <div style={{ marginBottom: 12 }}>
            <Text>用户：{editingUser.nickName}</Text>
            <br />
            <Text type="secondary">
              当前空间：{sizeToText(editingUser.useSpace)} / {sizeToText(editingUser.totalSpace)}
            </Text>
          </div>
        ) : null}

        <Form form={spaceForm} layout="vertical" initialValues={{ operation: 'set', unit: 'GB', value: 5 }}>
          <Form.Item label="操作" name="operation">
            <Select
              options={[
                { label: '设置总空间', value: 'set' },
                { label: '增加空间', value: 'add' },
                { label: '减少空间', value: 'subtract' },
              ]}
            />
          </Form.Item>

          <Form.Item label="大小" required>
            <Input.Group compact>
              <Form.Item name="value" noStyle rules={[{ required: true, message: '请输入大小' }, { type: 'number', min: 0.01 }]}>
                <InputNumber style={{ width: '65%' }} min={0.01} precision={2} controls={false} placeholder="请输入数值" />
              </Form.Item>
              <Form.Item name="unit" noStyle>
                <Select
                  style={{ width: '35%' }}
                  options={[
                    { label: 'MB', value: 'MB' },
                    { label: 'GB', value: 'GB' },
                    { label: 'TB', value: 'TB' },
                  ]}
                />
              </Form.Item>
            </Input.Group>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
