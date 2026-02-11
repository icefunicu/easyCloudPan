import { ElMessage } from 'element-plus'

const showMessage = (msg: string, callback: Function | undefined, type: any) => {
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
    error: (msg: string, callback?: Function) => {
        showMessage(msg, callback, "error");
    },
    success: (msg: string, callback?: Function) => {
        showMessage(msg, callback, "success");
    },
    warning: (msg: string, callback?: Function) => {
        showMessage(msg, callback, "warning");
    },
}

export default message;