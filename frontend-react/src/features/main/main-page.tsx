import { type Key, useCallback, useEffect, useMemo, useRef, useState } from 'react'

import {
  App,
  Button,
  Card,
  Dropdown,
  Flex,
  Tag,
  Grid,
} from 'antd'

import type { InputRef } from 'antd/es/input'
import type { ColumnsType } from 'antd/es/table'
import { MoreOutlined } from '@ant-design/icons'

import dayjs from 'dayjs'
import { useParams, useSearchParams } from 'react-router-dom'
import { FileIcon, FolderNavigation, FolderSelectModal, ShareModal } from '@/components/common'
import { PreviewModal } from '@/components/preview'
import { eventBus } from '@/lib/event-bus'
import { sizeToText } from '@/lib/format'
import { VIEW_MODE_STORAGE_KEY } from '@/lib/storage'
import { CATEGORY_OPTIONS } from '@/lib/constants'
import { getFolderInfo, changeFileFolder } from '@/services/file-service'
import type { FileInfoVO, FolderVO } from '@/types'

import { MainToolbar } from './components/main-toolbar'
import { FloatingActionDock } from './components/floating-action-dock'
import { UploadDndOverlay } from './components/upload-dnd-overlay'
import { MainListView, type FileTableRecord } from './components/main-list-view'
import { MainGridView } from './components/main-grid-view'

import { useFileList } from './hooks/use-file-list'
import { useFileDragDrop } from './hooks/use-file-drag-drop'
import { useFileShortcuts } from './hooks/use-file-shortcuts'
import { useFileActions } from './hooks/use-file-actions'
import './main-page.css'

const parsePath = (rawPath: string | null): string[] => {
  if (!rawPath) {
    return []
  }
  return rawPath
    .split('/')
    .map((item) => item.trim())
    .filter(Boolean)
}

export const MainPage = () => {
  const { message } = App.useApp()
  const screens = Grid.useBreakpoint()
  const isMobile = !screens.md

  const { category = 'all' } = useParams<{ category: string }>()
  const [searchParams, setSearchParams] = useSearchParams()

  const [searchKeyword, setSearchKeyword] = useState('')
  const [viewMode, setViewMode] = useState<'list' | 'grid'>(() =>
    localStorage.getItem(VIEW_MODE_STORAGE_KEY) === 'grid' ? 'grid' : 'list'
  )
  const [pageParams, setPageParams] = useState({ pageNo: 1, pageSize: 15 })
  const [selectedRowKeys, setSelectedRowKeys] = useState<Key[]>([])
  const [folders, setFolders] = useState<FolderVO[]>([])
  const [shareFile, setShareFile] = useState<FileInfoVO | null>(null)
  const [previewFile, setPreviewFile] = useState<FileInfoVO | null>(null)
  const searchInputRef = useRef<InputRef>(null)
  const folderRequestIdRef = useRef(0)

  const currentPathIds = parsePath(searchParams.get('path'))
  const currentFolderId = currentPathIds[currentPathIds.length - 1] || '0'
  const effectiveViewMode = isMobile ? 'grid' : viewMode
  const selectedSet = useMemo(() => new Set(selectedRowKeys.map(String)), [selectedRowKeys])

  const categoryAccept = useMemo(() => CATEGORY_OPTIONS.find((item) => item.key === category)?.accept || '*', [category])

  const gotoPathIds = useCallback(
    (ids: string[]) => {
      if (!ids.length) {
        setSearchParams({})
        return
      }
      setSearchParams({ path: ids.join('/') })
    },
    [setSearchParams]
  )

  const openFileOrFolder = useCallback(
    (file: FileInfoVO) => {
      if (file.folderType === 1) {
        gotoPathIds([...currentPathIds, file.fileId])
        return
      }
      if (file.status !== 2) {
        message.warning('文件尚未转码完成，暂不可预览')
        return
      }
      setPreviewFile(file)
    },
    [currentPathIds, gotoPathIds, message]
  )

  const { data: listData, isLoading: loading, refetch: refreshList } = useFileList({
    pageNo: pageParams.pageNo,
    pageSize: pageParams.pageSize,
    fileNameFuzzy: searchKeyword || undefined,
    filePid: category === 'all' ? currentFolderId : undefined,
    category,
  })

  const tableDataList = listData?.list || []
  const tableDataTotal = listData?.totalCount || 0

  useEffect(() => {
    const currentIds = new Set(tableDataList.map((item) => item.fileId))
    setSelectedRowKeys((prev) => prev.filter((id) => currentIds.has(String(id))))
  }, [tableDataList])

  const loadFolderPath = useCallback(async () => {
    const rawPath = searchParams.get('path')
    if (!rawPath) {
      setFolders([])
      return
    }
    const requestId = ++folderRequestIdRef.current
    const data = await getFolderInfo(rawPath)
    if (requestId === folderRequestIdRef.current) {
      setFolders(data || [])
    }
  }, [searchParams])

  const { draggingUpload, dragProps, addFiles } = useFileDragDrop({ currentFolderId })

  const {
    moveModalOpen,
    setMoveModalOpen,
    folderTreeLoading,
    folderTreeData,
    openCreateFolderModal,
    openMoveModal,
    downloadSelectedFiles,
    openBatchShare,
    confirmBatchDelete,
    rowOperations,
    toggleFileSelection,
    handleSingleDelete,
  } = useFileActions({
    currentFolderId,
    selectedRowKeys,
    setSelectedRowKeys,
    tableDataList,
    selectedSet,
    refreshList: async () => { await refreshList() },
    setShareFile,
  })

  useFileShortcuts({
    searchInputRef,
    selectedCount: selectedRowKeys.length,
    onClearSelection: () => setSelectedRowKeys([]),
    onConfirmBatchDelete: () => confirmBatchDelete(),
  })

  // Table columns definition is kept here due to its strong bond with view methods (openFileOrFolder)
  const columns = useMemo<ColumnsType<FileTableRecord>>(
    () => [
      {
        title: '文件名',
        dataIndex: 'fileName',
        key: 'fileName',
        render: (_value, record) => (
          <Flex gap={8} align="center" style={{ minWidth: 280 }}>
            <FileIcon folderType={record.folderType} fileType={record.fileType} fileCategory={record.fileCategory} />
            <a onClick={() => openFileOrFolder(record)}>{record.fileName}</a>
            {record.status === 0 ? <Tag color="gold">转码中</Tag> : null}
            {record.status === 1 ? <Tag color="red">转码失败</Tag> : null}
          </Flex>
        ),
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
        render: (value: number) => sizeToText(value),
      },
      {
        title: '操作',
        key: 'actions',
        width: 74,
        render: (_value, record) => (
          <Dropdown menu={{ items: rowOperations(record) }} trigger={['click']}>
            <Button className="btn-secondary-soft btn-icon-round" icon={<MoreOutlined />} />
          </Dropdown>
        ),
      },
    ],
    [openFileOrFolder, rowOperations]
  )

  const handleEffectiveSearch = useCallback((value: string) => {
    const nextValue = value.trim()
    setSearchKeyword((prev) => {
      if (prev === nextValue) return prev
      setPageParams((p) => ({ ...p, pageNo: 1 }))
      return nextValue
    })
  }, [])

  useEffect(() => {
    setPageParams((prev) => {
      if (prev.pageNo === 1) { return prev }
      return { ...prev, pageNo: 1 }
    })
    setSelectedRowKeys([])
  }, [category, currentFolderId])

  useEffect(() => {
    void refreshList()
  }, [refreshList])

  useEffect(() => {
    void loadFolderPath()
  }, [loadFolderPath])

  useEffect(() => {
    localStorage.setItem(VIEW_MODE_STORAGE_KEY, viewMode)
  }, [viewMode])

  useEffect(() => eventBus.on('data:reload', () => void refreshList()), [refreshList])

  return (
    <div className="main-page">
      <MainToolbar
        isMobile={isMobile}
        category={category}
        categoryAccept={categoryAccept}
        viewMode={viewMode}
        onViewModeChange={setViewMode}
        onSearchSubmit={handleEffectiveSearch}
        onRefresh={() => void refreshList()}
        onAddFiles={addFiles}
        onCreateFolder={openCreateFolderModal}
      />

      <FolderNavigation
        folders={folders}
        onBackParent={() => gotoPathIds(currentPathIds.slice(0, -1))}
        onJumpRoot={() => gotoPathIds([])}
        onJump={(index) => gotoPathIds(currentPathIds.slice(0, index + 1))}
      />

      <Card
        className={`main-file-panel ${draggingUpload ? 'dragging' : ''}`}
        style={{ position: 'relative', overflow: 'hidden' }}
        bodyStyle={{ padding: effectiveViewMode === 'grid' ? 12 : 0 }}
        {...dragProps}
      >
        <UploadDndOverlay draggingUpload={draggingUpload} />

        {effectiveViewMode === 'list' ? (
          <MainListView
            loading={loading}
            tableDataList={tableDataList}
            tableDataTotal={tableDataTotal}
            columns={columns}
            pageParams={pageParams}
            setPageParams={setPageParams}
            selectedRowKeys={selectedRowKeys}
            setSelectedRowKeys={setSelectedRowKeys as (keys: Key[]) => void}
          />
        ) : (
          <MainGridView
            isMobile={isMobile}
            tableDataList={tableDataList}
            tableDataTotal={tableDataTotal}
            pageParams={pageParams}
            setPageParams={setPageParams}
            selectedSet={selectedSet}
            toggleFileSelection={toggleFileSelection}
            rowOperations={rowOperations}
            openFileOrFolder={openFileOrFolder}
            setShareFile={setShareFile}
            handleSingleDelete={handleSingleDelete}
          />
        )}
      </Card>

      <FloatingActionDock
        selectedCount={selectedRowKeys.length}
        onClearSelection={() => setSelectedRowKeys([])}
        onBatchMove={() => void openMoveModal()}
        onBatchShare={openBatchShare}
        onBatchDownload={() => void downloadSelectedFiles()}
        onBatchDelete={confirmBatchDelete}
      />

      <ShareModal open={Boolean(shareFile)} file={shareFile} onClose={() => setShareFile(null)} />

      <PreviewModal open={Boolean(previewFile)} file={previewFile} mode="user" onClose={() => setPreviewFile(null)} />

      <FolderSelectModal
        open={moveModalOpen}
        loading={folderTreeLoading}
        folders={folderTreeData}
        currentFolderId={currentFolderId}
        onCancel={() => setMoveModalOpen(false)}
        onConfirm={async (folderId) => {
          if (!selectedRowKeys.length) {
            setMoveModalOpen(false)
            return
          }
          const ok = await changeFileFolder(selectedRowKeys.map(String).join(','), folderId)
          if (ok) {
            message.success('移动成功')
            setMoveModalOpen(false)
            setSelectedRowKeys([])
            await refreshList()
          }
        }}
      />
    </div>
  )
}
