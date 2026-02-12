import axios, { type InternalAxiosRequestConfig, type AxiosResponse } from 'axios'
import { ElLoading } from 'element-plus'
import router from '@/router'
import Message from '../utils/Message'
import { useUserInfoStore } from '@/stores/userInfoStore'

const contentTypeForm = 'application/x-www-form-urlencoded;charset=UTF-8'
const contentTypeJson = 'application/json'
const responseTypeJson = "json"

let loading: any = null;
let isRefreshing = false;
let refreshSubscribers: ((token: string) => void)[] = [];

const instance = axios.create({
    baseURL: '/api',
    timeout: 20 * 1000,
});

interface CustomAxiosRequestConfig extends InternalAxiosRequestConfig {
    showLoading?: boolean;
    errorCallback?: Function;
    showError?: boolean;
    uploadProgressCallback?: Function;
    dataType?: string;
    skipAuthRefresh?: boolean;
}

function subscribeTokenRefresh(callback: (token: string) => void) {
    refreshSubscribers.push(callback);
}

function onTokenRefreshed(token: string) {
    refreshSubscribers.forEach(callback => callback(token));
    refreshSubscribers = [];
}

async function refreshToken(): Promise<string | null> {
    const userInfoStore = useUserInfoStore();
    const refreshToken = userInfoStore.userInfo?.refreshToken;
    
    if (!refreshToken) {
        return null;
    }
    
    try {
        const response = await axios.post('/api/refreshToken', 
            new URLSearchParams({ refreshToken }).toString(), {
            headers: { 'Content-Type': contentTypeForm }
        });
        
        if (response.data && response.data.code === 200 && response.data.data) {
            const newToken = response.data.data.token;
            const newRefreshToken = response.data.data.refreshToken;
            
            userInfoStore.setUserInfo({
                ...userInfoStore.userInfo,
                token: newToken,
                refreshToken: newRefreshToken || refreshToken
            });
            
            return newToken;
        }
    } catch (error) {
        console.error('Token refresh failed:', error);
    }
    
    return null;
}

instance.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
        const customConfig = config as CustomAxiosRequestConfig;
        
        const userInfoStore = useUserInfoStore();
        const token = userInfoStore.getToken();
        if (token && !customConfig.skipAuthRefresh) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        
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
        if (loading) {
            loading.close();
        }
        Message.error("请求发送失败");
        return Promise.reject("请求发送失败");
    }
);

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
        
        if (responseData.code == 200) {
            return responseData;
        } else if (responseData.code == 901) {
            const customConfig = config as CustomAxiosRequestConfig;
            
            if (customConfig.skipAuthRefresh) {
                const userInfoStore = useUserInfoStore();
                userInfoStore.clearUserInfo();
                router.push("/login?redirectUrl=" + encodeURI(router.currentRoute.value.path));
                return Promise.reject({ showError: false, msg: "登录超时" });
            }
            
            if (!isRefreshing) {
                isRefreshing = true;
                
                return refreshToken().then(newToken => {
                    isRefreshing = false;
                    
                    if (newToken) {
                        onTokenRefreshed(newToken);
                        config.headers.Authorization = `Bearer ${newToken}`;
                        return instance(config);
                    } else {
                        const userInfoStore = useUserInfoStore();
                        userInfoStore.clearUserInfo();
                        router.push("/login?redirectUrl=" + encodeURI(router.currentRoute.value.path));
                        return Promise.reject({ showError: false, msg: "登录超时" });
                    }
                }).catch(error => {
                    isRefreshing = false;
                    const userInfoStore = useUserInfoStore();
                    userInfoStore.clearUserInfo();
                    router.push("/login?redirectUrl=" + encodeURI(router.currentRoute.value.path));
                    return Promise.reject({ showError: false, msg: "登录超时" });
                });
            } else {
                return new Promise((resolve) => {
                    subscribeTokenRefresh((token: string) => {
                        config.headers.Authorization = `Bearer ${token}`;
                        resolve(instance(config));
                    });
                });
            }
        } else {
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
    let formData = new FormData();
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
    } as any;

    return instance.post(url, formData, axiosConfig).catch(error => {
        if (import.meta.env.DEV) {
            console.error('Request error:', error);
        }
        if (error && error.showError) {
            Message.error(error.msg);
        }
        return null;
    });
};

export default request;
