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
