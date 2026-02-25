
import { Button, Card, Checkbox, Dropdown, Empty, Flex, Pagination, Space, Tag, Typography } from 'antd'
import type { MenuProps } from 'antd'
import { DeleteOutlined, MoreOutlined, ShareAltOutlined } from '@ant-design/icons'
import { FileIcon } from '@/components/common'
import { sizeToText } from '@/lib/format'
import type { FileInfoVO } from '@/types'

const { Text } = Typography

export interface MainGridViewProps {
    isMobile: boolean
    tableDataList: FileInfoVO[]
    tableDataTotal: number
    pageParams: { pageNo: number; pageSize: number }
    setPageParams: React.Dispatch<React.SetStateAction<{ pageNo: number; pageSize: number }>>
    selectedSet: Set<string>
    toggleFileSelection: (fileId: string, checked?: boolean) => void
    rowOperations: (file: FileInfoVO) => NonNullable<MenuProps['items']>
    openFileOrFolder: (file: FileInfoVO) => void
    setShareFile: (file: FileInfoVO) => void
    handleSingleDelete: (fileId: string) => Promise<void>
}

export const MainGridView = ({
    isMobile,
    tableDataList,
    tableDataTotal,
    pageParams,
    setPageParams,
    selectedSet,
    toggleFileSelection,
    rowOperations,
    openFileOrFolder,
    setShareFile,
    handleSingleDelete,
}: MainGridViewProps) => {
    return (
        <>
            <div className="main-grid-view">
                {tableDataList.length === 0 ? (
                    <Empty style={{ gridColumn: '1 / -1' }} description="当前目录暂无文件" />
                ) : (
                    tableDataList.map((item) => (
                        <Card
                            key={item.fileId}
                            size="small"
                            className={`main-grid-item ${selectedSet.has(item.fileId) ? 'selected' : ''}`}
                            bodyStyle={{ padding: 10 }}
                        >
                            <Flex vertical gap={8}>
                                <Flex align="center" justify="space-between">
                                    <Checkbox
                                        checked={selectedSet.has(item.fileId)}
                                        onChange={(event) => toggleFileSelection(item.fileId, event.target.checked)}
                                    />
                                    <Dropdown menu={{ items: rowOperations(item) }} trigger={['click']}>
                                        <Button className="btn-secondary-soft btn-small-polish btn-icon-round" size="small" icon={<MoreOutlined />} />
                                    </Dropdown>
                                </Flex>

                                <Flex align="center" gap={8} className="main-grid-name" onClick={() => openFileOrFolder(item)}>
                                    <FileIcon folderType={item.folderType} fileType={item.fileType} fileCategory={item.fileCategory} />
                                    <Text ellipsis={{ tooltip: item.fileName }}>{item.fileName}</Text>
                                </Flex>

                                <Text type="secondary" style={{ fontSize: 12 }}>
                                    {item.folderType === 1 ? '目录' : sizeToText(item.fileSize)}
                                </Text>

                                <Space wrap className="table-row-actions main-grid-actions">
                                    <Button className="btn-secondary-soft btn-small-polish" size="small" onClick={() => openFileOrFolder(item)}>
                                        打开
                                    </Button>
                                    <Button
                                        className="btn-secondary-soft btn-small-polish btn-icon-round"
                                        size="small"
                                        icon={<ShareAltOutlined />}
                                        disabled={item.folderType === 1 || item.status !== 2}
                                        onClick={() => setShareFile(item)}
                                    />
                                    <Button
                                        className="btn-danger-soft btn-small-polish btn-icon-round"
                                        size="small"
                                        icon={<DeleteOutlined />}
                                        onClick={() => void handleSingleDelete(item.fileId)}
                                    />
                                </Space>

                                {item.status === 0 ? <Tag color="gold">转码中</Tag> : null}
                                {item.status === 1 ? <Tag color="red">转码失败</Tag> : null}
                            </Flex>
                        </Card>
                    ))
                )}
            </div>

            <Flex justify="end" className="main-grid-pagination">
                <Pagination
                    size={isMobile ? 'small' : 'default'}
                    current={pageParams.pageNo}
                    pageSize={pageParams.pageSize}
                    total={tableDataTotal}
                    showSizeChanger={!isMobile}
                    showQuickJumper={!isMobile}
                    onChange={(pageNo, pageSize) => setPageParams((prev) => ({ ...prev, pageNo, pageSize }))}
                />
            </Flex>
        </>
    )
}
