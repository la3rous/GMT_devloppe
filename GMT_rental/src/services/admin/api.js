import { authenticatedApi as adminApi } from "../api";

adminApi.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401
      && !error.config?.preservePageOnUnauthorized
      && window.location.pathname !== "/admin/login") {
      localStorage.removeItem("adminToken");
      localStorage.removeItem("token");
      localStorage.removeItem("user");
      window.location.assign("/admin/login");
    }
    return Promise.reject(error);
  },
);

export const adminAuthApi = {
  login: (credentials) => adminApi.post("/auth/login", credentials),
};

export const adminApiService = {
  dashboard: () => adminApi.get("/admin/dashboard/stats"),
  dashboardAnalytics: (params) => adminApi.get("/admin/dashboard/analytics", { params }),
  vehicles: (params) => adminApi.get("/admin/vehicules", { params }),
  vehicle: (id) => adminApi.get(`/admin/vehicules/${id}`),
  createVehicle: (data) => adminApi.post("/admin/vehicules", data),
  updateVehicle: (id, data) => adminApi.put(`/admin/vehicules/${id}`, data),
  updateVehicleStatus: (id, statut) => adminApi.patch(`/admin/vehicules/${id}/statut`, null, { params: { statut } }),
  categories: () => adminApi.get("/admin/categories"),
  createCategory: (data) => adminApi.post("/admin/categories", data),
  updateCategory: (id, data) => adminApi.put(`/admin/categories/${id}`, data),
  deleteCategory: (id) => adminApi.delete(`/admin/categories/${id}`),
  quoteRequests: (params) => adminApi.get("/admin/demandes", { params }),
  quoteRequest: (id) => adminApi.get(`/admin/demandes/${id}`),
  updateQuoteRequestStatus: (id, statut) => adminApi.patch(`/admin/demandes/${id}/statut`, null, { params: { statut } }),
  notifications: () => adminApi.get("/admin/notifications"),
  unreadNotificationCount: () => adminApi.get("/admin/notifications/unread-count"),
  markNotificationAsRead: (id) => adminApi.patch(`/admin/notifications/${id}/read`),
  markAllNotificationsAsRead: () => adminApi.patch("/admin/notifications/read-all"),
  users: () => adminApi.get("/admin/users"),

  createUser: (data) =>
  adminApi.post("/admin/users", data),

  updateUser: (id, data) =>
  adminApi.put(`/admin/users/${id}`, data),
  deactivateUser: (id) => adminApi.delete(`/admin/users/${id}`),
  setUserActive: (id, actif) => adminApi.patch(`/admin/users/${id}/actif`, null, { params: { actif } }),
  resendUserVerification: (userId) => adminApi.post(`/admin/users/${userId}/resend-verification`),
  userRentals: (userId) => adminApi.get(`/admin/users/${userId}/rentals`, { preservePageOnUnauthorized: true }),
  createUserRental: (userId, data) => adminApi.post(`/admin/users/${userId}/rentals`, data, { preservePageOnUnauthorized: true }),
  settings: () => adminApi.get("/admin/settings"),
  saveSetting: (data) => adminApi.post("/admin/settings", data),
  profile: () => adminApi.get("/admin/account/profile"),
  updateProfile: (data) => adminApi.put("/admin/account/profile", data),
  sendProfileEmailCode: (email) => adminApi.post("/admin/account/email/send-code", { email }),
  verifyProfileEmail: (email, code) => adminApi.post("/admin/account/email/verify", { email, code }),
  changePassword: (data) => adminApi.put("/admin/account/password", data),
  preferences: () => adminApi.get("/admin/account/preferences"),
  updatePreferences: (data) => adminApi.put("/admin/account/preferences", data),
  testEmail: (email) => adminApi.post("/admin/account/email-test", { email }),
  logout: () => adminApi.post("/admin/account/logout"),
  activities: (params) => adminApi.get("/admin/activity", { params }),
  exportActivities: (params) => adminApi.get("/admin/activity/export", { params, responseType: "blob" }),
  mySupportRequests: () => adminApi.get("/admin/support/mine"),
  submitSupportRequest: (data) => adminApi.post("/admin/support", data),
};

export { verificationApi } from "../api";

export default adminApi;
