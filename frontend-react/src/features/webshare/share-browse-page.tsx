import { type Key, useCallback, useEffect, useMemo, useState } from 'react'
import { App, Button, Card, Flex, Space, Table, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { DownloadOutlined, SaveOutlined, StopOutlined } from '@ant-design/icons'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import {
  cancelShare,
  createShareDownloadUrl,
  getShareDownloadBaseUrl,
  getShareFolderInfo,
  getShareLoginInfo,
  loadShareFileList,
  saveShare,
} from '@/services/share-service'
import { loadAllFolder } from '@/services/file-service'
import { useAuthStore } from '@/store/auth-store'
import { resolveDownloadTarget } from '@/lib/download'
import type { FileInfoVO, FolderVO, ShareInfoVO } from '@/types'
import { FileIcon, FolderNavigation, FolderSelectModal } from '@/components/common'
import { PreviewModal } from '@/components/preview'
import { sizeToText } from '@/lib/format'

const { Title, Text } = Typography

interface ShareFileRecord extends FileInfoVO {
  key: string
}

const parsePath = (rawPath: string | null): string[] => {
  if (!rawPath) return []
  return rawPath
    .split('/')
    .map((item) => item.trim())
    .filter(Boolean)
}

export const ShareBrowsePage = () => {
  const { message, modal } = App.useApp()
  const navigate = useNavigate()
  const authUser = useAuthStore((state) => state.user)
  const { shareId = '' } = useParams<{ shareId: string }>()
  const [searchParams, setSearchParams] = useSearchParams()

  const [shareInfo, setShareInfo] = useState<ShareInfoVO | null>(null)
  const [loading, setLoading] = useState(false)
  const [folders, setFolders] = useState<FolderVO[]>([])
  const [selectedRowKeys, setSelectedRowKeys] = useState<Key[]>([])
  const [previewFile, setPreviewFile] = useState<FileInfoVO | null>(null)
  const [saveModalOpen, setSaveModalOpen] = useState(false)
  const [folderTreeData, setFolderTreeData] = useState<FileInfoVO[]>([])
  const [folderTreeLoading, setFolderTreeLoading] = useState(false)

  const [tableData, setTableData] = useState<{ list: FileInfoVO[]; pageNo: number; pageSize: number; totalCount: number }>({
    list: [],
    pageNo: 1,
    pageSize: 15,
    totalCount: 0,
  })

  const pathIds = parsePath(searchParams.get('path'))
  const currentFolderId = pathIds[pathIds.length - 1] || '0'

  const gotoPath = (ids: string[]) => {
    if (!ids.length) {
      setSearchParams({})
      return
    }
    setSearchParams({ path: ids.join('/') })
  }

  const refreshShareInfo = useCallback(async () => {
    const info = await getShareLoginInfo(shareId)
    if (info === null) {
      navigate(`/shareCheck/${shareId}`, { replace: true })
      return
    }
    setShareInfo(info)
  }, [navigate, shareId])

  const refreshList = useCallback(async () => {
    setLoading(true)
    const data = await loadShareFileList({
      pageNo: tableData.pageNo,
      pageSize: tableData.pageSize,
      shareId,
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
    setLoading(false)
  }, [currentFolderId, shareId, tableData.pageNo, tableData.pageSize])

  const refreshFolders = useCallback(async () => {
    const rawPath = searchParams.get('path')
    if (!rawPath) {
      setFolders([])
      return
    }
    const data = await getShareFolderInfo(shareId, rawPath)
    setFolders(data || [])
  }, [searchParams, shareId])

  useEffect(() => {
    void refreshShareInfo()
  }, [refreshShareInfo])

  useEffect(() => {
    void refreshList()
  }, [refreshList])

  useEffect(() => {
    void refreshFolders()
  }, [refreshFolders])

  const columns = useMemo<ColumnsType<ShareFileRecord>>(
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
                setPreviewFile(record)
              }}
            >
              {record.fileName}
            </a>
          </Flex>
        ),
      },
      {
        title: '更新时间',
        dataIndex: 'lastUpdateTime',
        key: 'lastUpdateTime',
        width: 190,
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
        width: 180,
        render: (_value, record) => (
          <Space>
            <Button
              size="small"
              icon={<DownloadOutlined />}
              disabled={record.folderType === 1}
              onClick={async () => {
                const code = await createShareDownloadUrl(shareId, record.fileId)
                if (code) {
                  window.location.href = resolveDownloadTarget(getShareDownloadBaseUrl(), code)
                }
              }}
            />
            {shareInfo?.currentUser ? null : (
              <Button
                size="small"
                icon={<SaveOutlined />}
                onClick={() => {
                  setSelectedRowKeys([record.fileId])
                  void openSaveModal()
                }}
              />
            )}
          </Space>
        ),
      },
    ],
    [pathIds, shareId, shareInfo?.currentUser]
  )

  const openSaveModal = async () => {
    if (!authUser) {
      navigate(`/login?redirectUrl=${encodeURIComponent(window.location.pathname + window.location.search)}`)
      return
    }
    setSaveModalOpen(true)
    setFolderTreeLoading(true)
    const foldersData = await loadAllFolder('0')
    setFolderTreeData(foldersData || [])
    setFolderTreeLoading(false)
  }

  return (
    <div>
      <Card className="glass-card">
        <Flex justify="space-between" align="center" wrap="wrap" gap={10}>
          <div>
            <Title level={5} style={{ margin: 0 }}>
              分享文件
            </Title>
            <Text type="secondary">
              分享者：{shareInfo?.nickName || '-'} ｜ 文件：{shareInfo?.fileName || '-'}
            </Text>
          </div>

          <Space>
            {shareInfo?.currentUser ? (
              <Button
                danger
                icon={<StopOutlined />}
                onClick={() => {
                  modal.confirm({
                    title: '取消分享',
                    content: '确认取消该分享吗？',
                    onOk: async () => {
                      const ok = await cancelShare(shareId)
                      if (ok) {
                        message.success('分享已取消')
                        navigate('/main/all')
                      }
                    },
                  })
                }}
              >
                取消分享
              </Button>
            ) : (
              <Button icon={<SaveOutlined />} disabled={!selectedRowKeys.length} onClick={() => void openSaveModal()}>
                保存到我的网盘
              </Button>
            )}
          </Space>
        </Flex>
      </Card>

      <FolderNavigation
        folders={folders}
        onBackParent={() => gotoPath(pathIds.slice(0, -1))}
        onJumpRoot={() => gotoPath([])}
        onJump={(index) => gotoPath(pathIds.slice(0, index + 1))}
      />

      <Card className="glass-card" style={{ marginTop: 12 }}>
        <Table<ShareFileRecord>
          rowKey="fileId"
          loading={loading}
          dataSource={tableData.list.map((item) => ({ ...item, key: item.fileId }))}
          columns={columns}
          scroll={{ x: 'max-content' }}
          rowSelection={{ selectedRowKeys, onChange: setSelectedRowKeys }}
          pagination={{
            current: tableData.pageNo,
            pageSize: tableData.pageSize,
            total: tableData.totalCount,
            onChange: (pageNo, pageSize) => {
              setTableData((prev) => ({ ...prev, pageNo, pageSize }))
            },
          }}
        />
      </Card>

      <PreviewModal
        open={Boolean(previewFile)}
        file={previewFile}
        mode="share"
        shareId={shareId}
        onClose={() => setPreviewFile(null)}
      />

      <FolderSelectModal
        open={saveModalOpen}
        loading={folderTreeLoading}
        folders={folderTreeData}
        onCancel={() => setSaveModalOpen(false)}
        onConfirm={async (folderId) => {
          const ok = await saveShare({
            shareId,
            shareFileIds: selectedRowKeys.map(String).join(','),
            myFolderId: folderId,
          })
          if (ok) {
            message.success('保存成功')
            setSaveModalOpen(false)
          }
        }}
      />
    </div>
  )
}




