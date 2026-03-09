import { api } from './axios';

export interface PaymentReadyRequest {
    userId: number;
    amount: number;
    domain: 'SUBSCRIPTION' | 'CONCERT';
    targetId: number;
    agencyId: number;
    reservationIds?: number[];
}

export interface PaymentReadyResponse {
    orderId: string;
    amount: number;
}

export const createPaymentReady = (body: PaymentReadyRequest) => {
    return api.post<PaymentReadyResponse>('/payments/ready', body).then(r => r.data);
};

export const confirmPayment = (payload: { paymentKey: string; orderId: string; amount: number }, userId?: number) => {
    const headers: any = {};
    if (userId) headers['X-User-Id'] = userId;
    return api.post('/payments/confirm', payload, { headers });
};

export const getIdol = (idolId: number) => {
    return api.get(`/idols/${idolId}`).then(r => r.data);
};

export const authorizeBillingKey = (body: { idolId: number; authKey: string; plan: string; customerKey: string }) => {
    return api.post('/subscriptions/billing/authorize', body).then(r => r.data);
}

export const getAgencyRevenue = async (agencyId: number): Promise<any> => {
    const res = await api.get(`/payments/agency/${agencyId}/revenue`);
    return res.data;
};;

// fetch available groups for subscription
export const fetchGroups = () => {
    return api.get('/groups').then(r => r.data);
};

// fetch idols belonging to a specific group
export const fetchGroupIdols = (groupId: number) => {
    return api.get(`/groups/${groupId}/idols`).then(r => r.data);
};

// subscribe to an entire group (free)
export const subscribeGroup = (userId: number, groupId: number) => {
    const headers: any = {};
    if (userId) headers['X-User-Id'] = userId;
    return api.post('/subscriptions/groups', { groupId, autoRenew: true }, { headers }).then(r => r.data);
};
