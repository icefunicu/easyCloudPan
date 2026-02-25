import { Flex, Typography } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { AnimatePresence, motion } from 'framer-motion'

export interface UploadDndOverlayProps {
    draggingUpload: boolean
}

export const UploadDndOverlay = ({ draggingUpload }: UploadDndOverlayProps) => {
    return (
        <AnimatePresence>
            {draggingUpload ? (
                <motion.div
                    initial={{ opacity: 0, scale: 0.96, backdropFilter: 'blur(0px)' }}
                    animate={{ opacity: 1, scale: 1, backdropFilter: 'blur(8px)' }}
                    exit={{ opacity: 0, scale: 0.96, backdropFilter: 'blur(0px)' }}
                    transition={{ duration: 0.25, ease: 'easeOut' }}
                    style={{
                        position: 'absolute',
                        inset: 0,
                        zIndex: 3,
                        pointerEvents: 'none',
                        display: 'grid',
                        placeItems: 'center',
                        border: '2px dashed rgba(31, 111, 139, 0.5)',
                        borderRadius: 16,
                        background: 'linear-gradient(135deg, rgba(231, 245, 251, 0.75), rgba(245, 252, 255, 0.85), rgba(224, 238, 245, 0.82))',
                    }}
                >
                    <motion.div
                        initial={{ y: 15 }}
                        animate={{ y: 0 }}
                        exit={{ y: 10 }}
                        transition={{ duration: 0.3, ease: 'backOut' }}
                    >
                        <Flex vertical align="center" gap={12}>
                            <div style={{
                                width: 64,
                                height: 64,
                                borderRadius: '50%',
                                background: 'rgba(31, 111, 139, 0.1)',
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                boxShadow: '0 8px 24px rgba(31, 111, 139, 0.15)'
                            }}>
                                <PlusOutlined style={{ fontSize: 24, color: '#164f68' }} />
                            </div>
                            <Flex vertical align="center" gap={6}>
                                <Typography.Text strong style={{ fontSize: 18, color: '#164f68', letterSpacing: '0.5px' }}>
                                    释放鼠标即可上传
                                </Typography.Text>
                                <Typography.Text type="secondary" style={{ color: 'rgba(22, 79, 104, 0.65)' }}>
                                    支持拖拽多个文件，上传任务会自动进入队列
                                </Typography.Text>
                            </Flex>
                        </Flex>
                    </motion.div>
                </motion.div>
            ) : null}
        </AnimatePresence>
    )
}
