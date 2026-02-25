import { type Key, useCallback, useState } from 'react'
import { App } from 'antd'

import type { MenuProps } from 'antd'
import { Input } from 'antd'
import {
    DeleteOutlined,
    DownloadOutlined,
    SwapOutlined,
    ShareAltOutlined,
} from '@ant-design/icons'

import {
    batchDownloadFiles,
    changeFileFolder,
    createDownloadUrl,
    deleteFile,
    getDownloadBaseUrl,
    loadAllFolder,
    newFolder,
    renameFile,
} from '@/services/file-service'
import { downloadBlob, resolveDownloadTarget } from '@/lib/download'
import type { FileInfoVO, FolderVO } from '@/types'

export interface UseFileActionsProps {
    currentFolderId: string
    selectedRowKeys: Key[]
    setSelectedRowKeys: (keys: Key[] | ((prev: Key[]) => Key[])) => void
    tableDataList: FileInfoVO[]
    selectedSet: Set<string>
    refreshList: () => Promise<void>
    setShareFile: (file: FileInfoVO | null) => void
}

export const useFileActions = ({
    currentFolderId,
    selectedRowKeys,
    setSelectedRowKeys,
    tableDataList,
    selectedSet,
    refreshList,
    setShareFile,
}: UseFileActionsProps) => {
    const { message, modal } = App.useApp()

    // Modal states
    const [moveModalOpen, setMoveModalOpen] = useState(false)
    const [folderTreeLoading, setFolderTreeLoading] = useState(false)
    const [folderTreeData, setFolderTreeData] = useState<FolderVO[]>([])

    const openCreateFolderModal = useCallback(() => {
        let folderName = ''
        modal.confirm({
            title: '新建目录',
            content: (
                <Input
                    placeholder="目录名称"
                    maxLength={190}
                    onChange={(event) => {
                        folderName = event.target.value
                    }}
                />
            ),
            onOk: async () => {
                if (!folderName || folderName.includes('/')) {
                    message.warning('目录名称不合法')
                    return Promise.reject()
                }
                const data = await newFolder(currentFolderId, folderName)
                if (data) {
                    message.success('创建成功')
                    await refreshList()
                }
                return Promise.resolve()
            },
        })
    }, [currentFolderId, message, modal, refreshList])

    const openMoveModal = useCallback(
        async (targetIds?: string[]) => {
            const fileIds = targetIds?.length ? targetIds : selectedRowKeys.map(String)
            if (!fileIds.length) {
                message.warning('请先选择文件')
                return
            }
            setSelectedRowKeys(fileIds)
            setMoveModalOpen(true)
            setFolderTreeLoading(true)
            try {
                const foldersData = await loadAllFolder(currentFolderId, fileIds.join(','))
                setFolderTreeData(foldersData || [])
            } finally {
                setFolderTreeLoading(false)
            }
        },
        [currentFolderId, message, selectedRowKeys, setSelectedRowKeys]
    )

    const handleConfirmMove = useCallback(
        async (folderId: string) => {
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
        },
        [message, refreshList, selectedRowKeys, setSelectedRowKeys]
    )


    const downloadSelectedFiles = useCallback(async () => {
        if (!selectedRowKeys.length) {
            message.warning('请先选择要下载的文件')
            return
        }

        const downloadIds = tableDataList
            .filter((item) => selectedSet.has(item.fileId) && item.folderType === 0)
            .map((item) => item.fileId)

        if (!downloadIds.length) {
            message.warning('当前选择中没有可下载文件')
            return
        }

        const blob = await batchDownloadFiles(downloadIds.join(','))
        if (!blob) {
            return
        }

        downloadBlob(blob, `easycloudpan-batch-${Date.now()}.zip`)
        message.success(`已开始下载 ${downloadIds.length} 个文件`)
    }, [message, selectedRowKeys.length, selectedSet, tableDataList])

    const openBatchShare = useCallback(() => {
        if (!selectedRowKeys.length) {
            message.warning('请先选择文件')
            return
        }
        const firstFile = tableDataList.find((item) => item.fileId === String(selectedRowKeys[0]))
        if (!firstFile) {
            message.warning('请选有效文件')
            return
        }
        if (firstFile.folderType === 1) {
            message.warning('目录暂不支持分享，请选择文件')
            return
        }
        setShareFile(firstFile)
    }, [message, selectedRowKeys, setShareFile, tableDataList])

    const confirmBatchDelete = useCallback(() => {
        if (!selectedRowKeys.length) {
            message.warning('请先选择文件')
            return
        }
        modal.confirm({
            title: '批量删除',
            content: `确认删除 ${selectedRowKeys.length} 项吗？`,
            onOk: async () => {
                const ok = await deleteFile(selectedRowKeys.map(String).join(','))
                if (ok) {
                    message.success('已删除')
                    setSelectedRowKeys([])
                    await refreshList()
                }
            },
        })
    }, [message, modal, refreshList, selectedRowKeys, setSelectedRowKeys])

    const rowOperations = useCallback(
        (file: FileInfoVO): NonNullable<MenuProps['items']> => [
            {
                key: 'share',
                label: '分享',
                icon: <ShareAltOutlined />,
                disabled: file.folderType === 1 || file.status !== 2,
                onClick: () => setShareFile(file),
            },
            {
                key: 'download',
                label: '下载',
                icon: <DownloadOutlined />,
                disabled: file.folderType === 1,
                onClick: async () => {
                    const code = await createDownloadUrl(file.fileId)
                    if (code) {
                        window.location.href = resolveDownloadTarget(getDownloadBaseUrl(), code)
                    }
                },
            },
            {
                key: 'rename',
                label: '重命名',
                onClick: () => {
                    let newName = file.fileName
                    modal.confirm({
                        title: '重命名',
                        content: '请输入新名称：',
                        onOk: async () => {
                            if (!newName || newName.includes('/')) {
                                message.warning('文件名不合法')
                                return Promise.reject()
                            }
                            const ok = await renameFile(file.fileId, newName)
                            if (ok) {
                                message.success('已重命名')
                                await refreshList()
                            }
                            return Promise.resolve()
                        },
                    })
                },
            },
            {
                key: 'move',
                label: '移动',
                icon: <SwapOutlined />,
                onClick: async () => {
                    await openMoveModal([file.fileId])
                },
            },
            {
                key: 'delete',
                label: '删除',
                icon: <DeleteOutlined />,
                danger: true,
                onClick: async () => {
                    modal.confirm({
                        title: '删除确认',
                        content: `确认删除 ${file.fileName} 吗？可在回收站恢复。`,
                        onOk: async () => {
                            const ok = await deleteFile(file.fileId)
                            if (ok) {
                                message.success('已删除')
                                await refreshList()
                            }
                        },
                    })
                },
            },
        ],
        [message, modal, openMoveModal, refreshList, setShareFile]
    )

    const toggleFileSelection = useCallback(
        (fileId: string, checked?: boolean) => {
            setSelectedRowKeys((prev) => {
                const nextSet = new Set(prev.map(String))
                const shouldSelect = checked ?? !nextSet.has(fileId)
                if (shouldSelect) {
                    nextSet.add(fileId)
                } else {
                    nextSet.delete(fileId)
                }
                return Array.from(nextSet)
            })
        },
        [setSelectedRowKeys]
    )


    const handleSingleDelete = useCallback(
        async (fileId: string) => {
            const ok = await deleteFile(fileId)
            if (ok) {
                message.success('已删除')
                await refreshList()
            }
        },
        [message, refreshList]
    )


    return {
        // Modal states
        moveModalOpen,
        setMoveModalOpen,
        folderTreeLoading,
        folderTreeData,

        // Actions
        openCreateFolderModal,
        openMoveModal,
        handleConfirmMove,
        downloadSelectedFiles,
        openBatchShare,
        confirmBatchDelete,
        rowOperations,
        toggleFileSelection,
        handleSingleDelete,
    }
}
