import { Breadcrumb, Dropdown } from 'antd'
import type { FolderVO } from '@/types'

interface FolderNavigationProps {
  folders: FolderVO[]
  onBackParent: () => void
  onJumpRoot: () => void
  onJump: (index: number) => void
}

export const FolderNavigation = ({ folders, onBackParent, onJumpRoot, onJump }: FolderNavigationProps) => {
  const shortPath = folders.length <= 4

  return (
    <div className="glass-card folder-nav-card">
      <Breadcrumb
        items={[
          ...(folders.length
            ? [
                {
                  title: <a onClick={onBackParent}>返回上一级</a>,
                },
              ]
            : []),
          {
            title: folders.length ? <a onClick={onJumpRoot}>全部文件</a> : <span>全部文件</span>,
          },
          ...(shortPath
            ? folders.map((item, index) => ({
                title:
                  index === folders.length - 1 ? (
                    <span>{item.fileName}</span>
                  ) : (
                    <a onClick={() => onJump(index)}>{item.fileName}</a>
                  ),
              }))
            : [
                {
                  title: (
                    <Dropdown
                      menu={{
                        items: folders.slice(0, -1).map((folder, index) => ({
                          key: String(index),
                          label: folder.fileName,
                        })),
                        onClick: ({ key }) => onJump(Number(key)),
                      }}
                    >
                      <a>...</a>
                    </Dropdown>
                  ),
                },
                {
                  title: <span>{folders[folders.length - 1]?.fileName}</span>,
                },
              ]),
        ]}
      />
    </div>
  )
}

