import { ElMessage } from 'element-plus'

const showMessage = (msg: string, callback: (() => void) | undefined, type: string) => {
    ElMessage({
        type: type,
        message: msg,
        duration: 2000,
        onClose: () => {
            if (callback) {
                callback();
            }
        }
    })
}

const message = {
    error: (msg: string, callback?: () => void) => {
        showMessage(msg, callback, "error");
    },
    success: (msg: string, callback?: () => void) => {
        showMessage(msg, callback, "success");
    },
    warning: (msg: string, callback?: () => void) => {
        showMessage(msg, callback, "warning");
    },
}

export default message;