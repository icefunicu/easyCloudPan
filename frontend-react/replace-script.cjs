const fs = require('fs');
let code = fs.readFileSync('src/features/main/main-page.tsx', 'utf8');

// Replace imports
code = code.replace(/import \{ type DragEvent.*?from 'antd'/s,
    `import { type DragEvent, type Key, useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import {
  App,
  Button,
  Card,
  Checkbox,
  Dropdown,
  Empty,
  Flex,
  Grid,
  Input,
  Pagination,
  Space,
  Table,
  Tag,
  Typography,
} from 'antd'`);

code = code.replace(new RegExp(`import type \\{ InputRef \\} from 'antd/es/input'[\\r\\n]*`, 'g'), '');

code = code.replace(/DeleteOutlined,[\s\S]*?SwapOutlined,[\s\S]*?\} from '@ant-design\/icons'/s,
    `DeleteOutlined,
  DownloadOutlined,
  MoreOutlined,
  ShareAltOutlined,
} from '@ant-design/icons'`);

code = code.replace(/import \{[\s\S]*?\} from '@\/services\/file-service'/s,
    `import {
  batchDownloadFiles,
  changeFileFolder,
  createDownloadUrl,
  deleteFile,
  getDownloadBaseUrl,
  getFolderInfo,
  loadAllFolder,
  newFolder,
  renameFile,
} from '@/services/file-service'`);

code = code.replace(/import '\.\/main-page\.css'/,
    `import { MainToolbar } from './components/main-toolbar'
import { useFileList } from './hooks/use-file-list'
import './main-page.css'`);

// Replace state
code = code.replace(/const \[loading, setLoading\] = useState\(false\)[\s\S]*?const \[tableData, setTableData\] = useState<[^>]*>\(\{[\s\S]*?\}\)/,
    `const [searchKeywordInput, setSearchKeywordInput] = useState('')
  const [searchKeyword, setSearchKeyword] = useState('')
  const [viewMode, setViewMode] = useState<'list' | 'grid'>(() =>
    localStorage.getItem(VIEW_MODE_STORAGE_KEY) === 'grid' ? 'grid' : 'list'
  )
  const [pageParams, setPageParams] = useState({ pageNo: 1, pageSize: 15 })`);

// Replace refreshList with React Query
code = code.replace(/const refreshList = useCallback\(async \(\) => \{[\s\S]*?\}, \[category, currentFolderId, searchKeyword, tableData\.pageNo, tableData\.pageSize\]\)/,
    `const queryClient = useQueryClient()

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
  }, [tableDataList])`);

// Replace tableData.list references
code = code.replace(/tableData\.list/g, 'tableDataList');
code = code.replace(/tableData\.pageNo/g, 'pageParams.pageNo');
code = code.replace(/tableData\.pageSize/g, 'pageParams.pageSize');
code = code.replace(/tableData\.totalCount/g, 'tableDataTotal');
code = code.replace(/setTableData\(\(prev\) => \(\{ \.\.\.prev, pageNo: 1 \}\)\)/g, 'setPageParams((prev) => ({ ...prev, pageNo: 1 }))');
code = code.replace(/setTableData\(\(prev\) => \(\{ \.\.\.prev, pageNo, pageSize \}\)\)/g, 'setPageParams((prev) => ({ ...prev, pageNo, pageSize }))');
code = code.replace(/setTableData\(\(prev\) => \{[\s\S]*?if \(prev\.pageNo === 1\) \{[\s\S]*?return prev[\s\S]*?\}[\s\S]*?return \{ \.\.\.prev, pageNo: 1 \}[\s\S]*?\}\)/,
    `setPageParams((prev) => {
      if (prev.pageNo === 1) { return prev }
      return { ...prev, pageNo: 1 }
    })`);

// Replace MainToolbar component structure
code = code.replace(/<Flex vertical gap=\{10\} className="main-toolbar">[\s\S]*?<\/Flex>\s*?\{isMobile && selectedRowKeys\.length \? \(/,
    `      <MainToolbar
        isMobile={isMobile}
        category={category}
        categoryAccept={categoryAccept}
        viewMode={viewMode}
        onViewModeChange={setViewMode}
        searchKeywordInput={searchKeywordInput}
        onSearchKeywordInputChange={setSearchKeywordInput}
        onSearchSubmit={(value) => {
          const nextValue = value.trim()
          setSearchKeyword(nextValue)
          setPageParams((prev) => ({ ...prev, pageNo: 1 }))
        }}
        onRefresh={() => void refreshList()}
        onAddFiles={addFiles}
        onCreateFolder={openCreateFolderModal}
        selectedRowKeysLength={selectedRowKeys.length}
        onBatchMove={() => void openMoveModal()}
        onBatchShare={openBatchShare}
        onBatchDownload={() => void downloadSelectedFiles()}
        onBatchDelete={confirmBatchDelete}
      />
      {isMobile && selectedRowKeys.length ? (`);

// Mobile Action Items
code = code.replace(/const mobileActionItems = useMemo<NonNullable<MenuProps\['items'\]>>\(\) => \{[\s\S]*?return items\n  \}, \[.*?\]\)/, '');

fs.writeFileSync('src/features/main/main-page.tsx', code);
console.log('Done replacement!');
