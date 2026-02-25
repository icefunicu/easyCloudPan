import { Skeleton } from 'antd'

interface TableSkeletonProps {
  rows?: number
}

export const TableSkeleton = ({ rows = 6 }: TableSkeletonProps) => {
  return (
    <div className="table-skeleton" aria-busy="true" aria-live="polite">
      {Array.from({ length: rows }).map((_, index) => (
        <div key={index} className="table-skeleton-row">
          <Skeleton.Button active block size="default" />
        </div>
      ))}
    </div>
  )
}
