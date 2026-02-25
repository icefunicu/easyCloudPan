import type { Key } from 'react'
import { Empty, Table } from 'antd'
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
    return (
        <Table<FileTableRecord>
            rowKey="fileId"
            loading={loading}
            dataSource={tableDataList.map((item) => ({ ...item, key: item.fileId }))}
            columns={columns}
            scroll={{ x: 'max-content' }}
            pagination={{
                current: pageParams.pageNo,
                pageSize: pageParams.pageSize,
                total: tableDataTotal,
                onChange: (pageNo, pageSize) => setPageParams((prev) => ({ ...prev, pageNo, pageSize })),
            }}
            rowSelection={{ selectedRowKeys, onChange: setSelectedRowKeys }}
            locale={{
                emptyText: <Empty description="当前目录暂无文件" />,
            }}
        />
    )
}
