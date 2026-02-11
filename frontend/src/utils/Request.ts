import axios, { type InternalAxiosRequestConfig, type AxiosResponse } from 'axios'
import { ElLoading } from 'element-plus'
import router from '@/router'
import Message from '../utils/Message'

const contentTypeForm = 'application/x-www-form-urlencoded;charset=UTF-8'
const contentTypeJson = 'application/json'
// arraybuffer   ArrayBuffer对象
// blob Blob对象
const responseTypeJson = "json"

let loading: any = null;
const instance = axios.create({
    baseURL: '/api',
    timeout: 20 * 1000,
});

// Define custom config
interface CustomAxiosRequestConfig extends InternalAxiosRequestConfig {
    showLoading?: boolean;
    errorCallback?: Function;
    showError?: boolean;
    uploadProgressCallback?: Function;
    dataType?: string;
}

// 请求前拦截器
instance.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
        const customConfig = config as CustomAxiosRequestConfig;
        if (customConfig.showLoading) {
            loading = ElLoading.service({
                lock: true,
                text: '加载中......',
                background: 'rgba(0, 0, 0, 0.7)',
            });
        }
        return config;
    },
    (error: any) => {
        // config is not available here easily typically, but if we need to close loading:
        if (loading) {
            loading.close();
        }
        Message.error("请求发送失败");
        return Promise.reject("请求发送失败");
    }
);
// 请求后拦截器
instance.interceptors.response.use(
    (response: AxiosResponse) => {
        const config = response.config as CustomAxiosRequestConfig;
        const { showLoading, errorCallback, showError = true, responseType } = config;
        if (showLoading && loading) {
            loading.close()
        }
        const responseData = response.data;
        if (responseType == "arraybuffer" || responseType == "blob") {
            return responseData;
        }
        // 正常请求
        if (responseData.code == 200) {
            return responseData;
        } else if (responseData.code == 901) {
            // 登录超时
            router.push("/login?redirectUrl=" + encodeURI(router.currentRoute.value.path));
            return Promise.reject({ showError: false, msg: "登录超时" });
        } else {
            // 其他错误
            if (errorCallback) {
                errorCallback(responseData.info);
            }
            return Promise.reject({ showError: showError, msg: responseData.info });
        }
    },
    (error: any) => {
        if (error.config && error.config.showLoading && loading) {
            loading.close();
        }
        return Promise.reject({ showError: true, msg: "网络异常" })
    }
);

const request = (config: {
    url: string;
    params?: any;
    dataType?: string;
    showLoading?: boolean;
    responseType?: any;
    errorCallback?: Function;
    showError?: boolean;
    uploadProgressCallback?: Function;
}) => {
    const { url, params, dataType, showLoading = true, responseType = responseTypeJson } = config;
    let contentType = contentTypeForm;
    let formData = new FormData(); // 创建form对象
    for (let key in params) {
        formData.append(key, params[key] == undefined ? "" : params[key]);
    }
    if (dataType != null && dataType == 'json') {
        contentType = contentTypeJson;
    }
    let headers = {
        'Content-Type': contentType,
        'X-Requested-with': 'XMLHttpRequest',
    }

    const axiosConfig: CustomAxiosRequestConfig = {
        headers: headers as any,
        responseType: responseType,
        showLoading: showLoading,
        errorCallback: config.errorCallback,
        showError: config.showError,
        onUploadProgress: (event: any) => {
            if (config.uploadProgressCallback) {
                config.uploadProgressCallback(event);
            }
        }
    } as any; // Cast to any to avoid strict strictness on partial methods for now

    return instance.post(url, formData, axiosConfig).catch(error => {
        if (import.meta.env.DEV) {
            console.error('Request error:', error);
        }
        if (error.showError) {
            Message.error(error.msg);
        }
        return null;
    });
};

export default request;