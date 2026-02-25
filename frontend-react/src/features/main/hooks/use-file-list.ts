import { useQuery } from '@tanstack/react-query'
import { loadDataList, type LoadDataListParams } from '@/services/file-service'
import type { FileInfoVO } from '@/types'

export const useFileList = (
    params: LoadDataListParams,
    options?: { enabled?: boolean; keepPreviousData?: boolean }
) => {
    return useQuery({
        queryKey: ['fileList', params],
        queryFn: async () => {
            const result = await loadDataList(params)
            if (!result) {
                return { list: [] as FileInfoVO[], pageNo: params.pageNo, pageSize: params.pageSize, totalCount: 0 }
            }
            return result
        },
        ...options,
    })
}
