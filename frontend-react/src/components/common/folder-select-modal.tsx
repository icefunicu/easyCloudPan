import { useEffect, useMemo, useState } from 'react'
import { Modal, Tree } from 'antd'
import type { DataNode } from 'antd/es/tree'
import type { FolderVO } from '@/types'

interface FolderSelectModalProps {
  open: boolean
  loading?: boolean
  folders: FolderVO[]
  currentFolderId?: string
  onCancel: () => void
  onConfirm: (folderId: string) => void
}

export const FolderSelectModal = ({
  open,
  loading,
  folders,
  currentFolderId,
  onCancel,
  onConfirm,
}: FolderSelectModalProps) => {
  const [selectedKey, setSelectedKey] = useState<string>('0')

  useEffect(() => {
    if (open) {
      setSelectedKey(currentFolderId || '0')
    }
  }, [currentFolderId, open])

  const treeData = useMemo<DataNode[]>(() => {
    const map = new Map<string, DataNode & { children: DataNode[] }>()

    map.set('0', {
      key: '0',
      title: '根目录',
      children: [],
    })

    folders.forEach((folder) => {
      map.set(folder.fileId, {
        key: folder.fileId,
        title: folder.fileName,
        children: [],
      })
    })

    folders.forEach((folder) => {
      const parent = map.get(folder.filePid || '0') || map.get('0')
      const node = map.get(folder.fileId)
      if (parent && node) {
        parent.children.push(node)
      }
    })

    return [map.get('0')!]
  }, [folders])

  return (
    <Modal
      title="选择目标目录"
      open={open}
      confirmLoading={loading}
      onCancel={onCancel}
      onOk={() => onConfirm(selectedKey || '0')}
    >
      <Tree
        defaultExpandAll
        selectedKeys={[selectedKey]}
        onSelect={(keys) => setSelectedKey(String(keys[0] || '0'))}
        treeData={treeData}
      />
    </Modal>
  )
}

