export const APP_NAME = 'EasyCloudPan React'

export const CATEGORY_OPTIONS = [
  { key: 'all', label: '全部', accept: '*' },
  { key: 'video', label: '视频', accept: '.mp4,.avi,.rmvb,.mkv,.mov' },
  { key: 'music', label: '音频', accept: '.mp3,.wav,.wma,.mp2,.flac,.midi,.ra,.ape,.aac,.cda' },
  { key: 'image', label: '图片', accept: '.jpeg,.jpg,.png,.gif,.bmp,.dds,.psd,.pdt,.webp,.xmp,.svg,.tiff' },
  { key: 'doc', label: '文档', accept: '.pdf,.doc,.docx,.xls,.xlsx,.txt' },
  { key: 'others', label: '其他', accept: '*' },
]

export const SENSITIVE_SIGN_PATHS = [
  '/file/uploadFile',
  '/file/delFile',
  '/file/changeFileFolder',
  '/file/rename',
  '/file/newFoloder',
  '/share/shareFile',
  '/share/cancelShare',
  '/showShare/saveShare',
  '/recycle/',
  '/admin/',
  '/updatePassword',
  '/updateUserAvatar',
  '/updateNickName',
]

export const SIGN_SKIP_PATHS = [
  '/login',
  '/register',
  '/logout',
  '/refreshToken',
  '/checkCode',
  '/sendEmailCode',
  '/actuator',
  '/swagger-ui',
  '/v3/api-docs',
]

