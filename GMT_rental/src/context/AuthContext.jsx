/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, useState } from 'react';
import API from '../services/api';
import { adminAuthApi } from '../services/admin/api';

const AuthContext = createContext();

const readInitialUser = () => {
  try {
    const savedUser = localStorage.getItem('user');
    const token = localStorage.getItem('token');
    if (!savedUser || !token) return null;
    const parsedUser = JSON.parse(savedUser);
    const role = typeof parsedUser.role === 'string' ? parsedUser.role : parsedUser.role?.name || parsedUser.role?.authority || '';
    const isSavedAdmin = ['ADMIN', 'ROLE_ADMIN'].includes(role.toUpperCase());
    if (isSavedAdmin && !localStorage.getItem('adminToken')) {
      localStorage.removeItem('user');
      localStorage.removeItem('token');
      return null;
    }
    return parsedUser;
  } catch {
    localStorage.removeItem('user');
    localStorage.removeItem('token');
    localStorage.removeItem('adminToken');
    return null;
  }
};

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(readInitialUser);
  const loading = false;

  const login = async (email, motDePasse) => {
    const response = await adminAuthApi.login({ email, motDePasse });
    const { token, ...userData } = response.data;
    localStorage.setItem('token', token);
    localStorage.setItem('adminToken', token);
    localStorage.setItem('user', JSON.stringify(userData));
    setUser(userData);
    return userData;
  };

  const register = async (userData) => {
    const response = await API.post('/auth/register', userData);
    const { token, ...newUserData } = response.data;
    localStorage.removeItem('adminToken');
    localStorage.setItem('token', token);
    localStorage.setItem('user', JSON.stringify(newUserData));
    setUser(newUserData);
    return newUserData;
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('adminToken');
    localStorage.removeItem('user');
    setUser(null);
  };

  const updateAuthenticatedUser = (userData, token) => {
    if (token) {
      localStorage.setItem('token', token);
      localStorage.setItem('adminToken', token);
    }
    localStorage.setItem('user', JSON.stringify(userData));
    setUser(userData);
  };

  const isAdmin = () => {
    if (!user) return false;
    const role = typeof user.role === 'string' ? user.role : user.role?.name || user.role?.authority || '';
    return role.toUpperCase() === 'ROLE_ADMIN' || role.toUpperCase() === 'ADMIN';
  };

  return (
    <AuthContext.Provider value={{ user, login, register, logout, updateAuthenticatedUser, isAdmin, loading }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);
