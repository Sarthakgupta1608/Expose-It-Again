import axios, {
  AxiosError,
  AxiosInstance,
  AxiosRequestConfig,
  InternalAxiosRequestConfig,
} from "axios";

interface RetryableRequestConfig extends InternalAxiosRequestConfig {
  _retry?: boolean;
}

const api: AxiosInstance = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api",
  withCredentials: true, // required: access_token / refresh_token are httpOnly cookies
});

// --- Refresh queueing state -------------------------------------------------
// Prevents multiple parallel /auth/refresh calls when several requests
// fail at the same time because the access token just expired.

let isRefreshing = false;
let failedQueue: {
  resolve: (value?: unknown) => void;
  reject: (reason?: unknown) => void;
}[] = [];

const processQueue = (error: unknown | null) => {
  failedQueue.forEach(({ resolve, reject }) => {
    if (error) reject(error);
    else resolve();
  });
  failedQueue = [];
};

// --- Response interceptor ---------------------------------------------------

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as RetryableRequestConfig | undefined;

    if (!originalRequest) {
      return Promise.reject(error);
    }

    const status = error.response?.status;
    // Only intercept 401 (Unauthorized). 403 means the user is authenticated
    // but lacks permission — that's a real error, not an expired token.
    const isAuthError = status === 401;
    const isRefreshCall = originalRequest.url?.includes("/auth/refresh");

    if (!isAuthError || originalRequest._retry || isRefreshCall) {
      return Promise.reject(error);
    }

    if (isRefreshing) {
      // A refresh is already in flight — queue this request until it resolves
      return new Promise((resolve, reject) => {
        failedQueue.push({ resolve, reject });
      })
        .then(() => api(originalRequest))
        .catch((err) => Promise.reject(err));
    }

    originalRequest._retry = true;
    isRefreshing = true;

    try {
      await api.post("/auth/refresh");
      processQueue(null);
      return api(originalRequest);
    } catch (refreshError) {
      processQueue(refreshError);
      if (typeof window !== "undefined") {
        window.location.href = "/login";
      }
      return Promise.reject(refreshError);
    } finally {
      isRefreshing = false;
    }
  }
);

export default api;
export type { AxiosRequestConfig };
