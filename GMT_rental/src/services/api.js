import axios from "axios";

// One base URL for both public pages and the authenticated admin client.
export const API_BASE_URL = import.meta.env.VITE_API_URL || "http://localhost:8081/api";

export const publicApi = axios.create({ baseURL: API_BASE_URL });
export const authenticatedApi = axios.create({ baseURL: API_BASE_URL });

authenticatedApi.interceptors.request.use((config) => {
  const token = localStorage.getItem("adminToken");
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

export const vehicleApi = {
  getAll: (params) => publicApi.get("/vehicules", { params }),
  getById: (id) => publicApi.get(`/vehicules/${id}`),
};

export const categoryApi = {
  getAll: () => publicApi.get("/categories"),
};

export const quoteRequestApi = {
  create: (data, idempotencyKey) => publicApi.post("/public/demandes", data, { headers: { "Idempotency-Key": idempotencyKey } }),
  checkAvailability: (params) => publicApi.get("/public/demandes/disponibilite", { params }),
};

export const verificationApi = {
  send: (email) => publicApi.post("/auth/verification/send", { email }),
  verify: (email, code) => publicApi.post("/auth/verification/verify", { email, code }),
  resend: (email) => publicApi.post("/auth/verification/resend", { email }),
};

// Keep the existing default export for the account context's legacy register call.
const API = axios.create({ baseURL: API_BASE_URL });
API.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

export default API;
