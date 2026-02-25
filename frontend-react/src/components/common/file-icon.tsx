import {
  FileImageOutlined,
  FileOutlined,
  FilePdfOutlined,
  FileTextOutlined,
  FileWordOutlined,
  FileExcelOutlined,
  VideoCameraOutlined,
  CustomerServiceOutlined,
  FolderOpenOutlined,
  FileZipOutlined,
  CodeOutlined,
} from '@ant-design/icons'

interface FileIconProps {
  folderType: number
  fileType: number
  fileCategory: number
  size?: number
}

export const FileIcon = ({ folderType, fileType, fileCategory, size = 18 }: FileIconProps) => {
  const style = { fontSize: size }
  if (folderType === 1) {
    return <FolderOpenOutlined style={{ ...style, color: '#2f8f6a' }} />
  }
  if (fileCategory === 1) {
    return <VideoCameraOutlined style={{ ...style, color: '#1f6f8b' }} />
  }
  if (fileCategory === 2) {
    return <CustomerServiceOutlined style={{ ...style, color: '#c78a33' }} />
  }
  if (fileCategory === 3) {
    return <FileImageOutlined style={{ ...style, color: '#2f8f6a' }} />
  }
  if (fileType === 4) {
    return <FilePdfOutlined style={{ ...style, color: '#ba3d4b' }} />
  }
  if (fileType === 5) {
    return <FileWordOutlined style={{ ...style, color: '#3262b5' }} />
  }
  if (fileType === 6) {
    return <FileExcelOutlined style={{ ...style, color: '#3d8e53' }} />
  }
  if (fileType === 7) {
    return <FileTextOutlined style={{ ...style, color: '#5a6570' }} />
  }
  if (fileType === 8) {
    return <CodeOutlined style={{ ...style, color: '#5a6570' }} />
  }
  if (fileType === 9) {
    return <FileZipOutlined style={{ ...style, color: '#8b5f2f' }} />
  }
  return <FileOutlined style={{ ...style, color: '#596b79' }} />
}

