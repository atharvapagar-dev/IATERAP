import api from './api';

export const getLrus = () => api.get('/lrus');

export const getLruById = (id) =>
    api.get(`/lrus/${id}`);

export const createLru = (data) =>
    api.post('/lrus', data);

export const updateLru = (id, data) =>
    api.put(`/lrus/${id}`, data);

export const deleteLru = (id) =>
    api.delete(`/lrus/${id}`);
