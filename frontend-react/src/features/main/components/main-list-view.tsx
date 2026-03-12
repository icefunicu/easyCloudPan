import type { Key } from 'react'
import { Empty, Table, Spin } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type { FileInfoVO } from '@/types'

export interface FileTableRecord extends FileInfoVO {
    key: string
}

export interface MainListViewProps {
    loading: boolean
    tableDataList: FileInfoVO[]
    tableDataTotal: number
    columns: ColumnsType<FileTableRecord>
    pageParams: { pageNo: number; pageSize: number }
    setPageParams: React.Dispatch<React.SetStateAction<{ pageNo: number; pageSize: number }>>
    selectedRowKeys: Key[]
    setSelectedRowKeys: (keys: Key[]) => void
}

export const MainListView = ({
    loading,
    tableDataList,
    tableDataTotal,
    columns,
    pageParams,
    setPageParams,
    selectedRowKeys,
    setSelectedRowKeys,
}: MainListViewProps) => {
    // 自定义加载指示器，提供更好的用户体验
    const customLoadingIndicator = loading ? {
        indicator: (
            <div style={{ textAlign: 'center', padding: '40px 0' }}>
                <Spin size="large" tip="正在加载文件列表..." />
            </div>
        ),
        spinning: true
    } : undefined;

    return (
        <Table<FileTableRecord>
            rowKey="fileId"
            loading={customLoadingIndicator}
            dataSource={tableDataList.map((item) => ({ ...item, key: item.fileId }))}
            columns={columns}
            scroll={{ x: 'max-content' }}
            pagination={{
                current: pageParams.pageNo,
                pageSize: pageParams.pageSize,
                total: tableDataTotal,
                onChange: (pageNo, pageSize) => setPageParams((prev) => ({ ...prev, pageNo, pageSize })),
                showSizeChanger: true,
                showQuickJumper: true,
                showTotal: (total) => `共 ${total} 条记录`,
            }}
            rowSelection={{ selectedRowKeys, onChange: setSelectedRowKeys }}
            locale={{
                emptyText: tableDataList.length === 0 && !loading ? (
                    <Empty 
                        description="当前目录暂无文件"
                        image={Empty.PRESENTED_IMAGE_SIMPLE}
                    />
                ) : (
                    <Empty 
                        description="正在加载..."
                        image={Empty.PRESENTED_IMAGE_SIMPLE}
                    />
                ),
            }}
        />
    )
}
