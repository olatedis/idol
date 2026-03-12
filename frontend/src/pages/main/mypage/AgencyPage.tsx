import React, { useEffect, useState } from 'react';
import { getAgencyRevenue } from '../../../api/payment';
import { motion } from 'framer-motion';
import AgencyGroupTab from './AgencyGroupTab';

interface AgencyPageProps {
    agencyId?: number;
}

const AgencyPage: React.FC<AgencyPageProps> = ({ agencyId }) => {
    const [revenueData, setRevenueData] = useState<any>(null);
    const [loading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);
    const [activeTab, setActiveTab] = useState<'revenue' | 'group'>('revenue');

    useEffect(() => {
        if (!agencyId) {
            setError("유효하지 않은 소속사 ID이거나 연동된 소속사가 없습니다.");
            setLoading(false);
            return;
        }

        const fetchRevenue = async () => {
            try {
                const data = await getAgencyRevenue(agencyId);
                setRevenueData(data);
            } catch (err: any) {
                console.error("매출 기록 조회 실패:", err);
                if (err.response?.status === 403) {
                    setError("접근 권한이 없습니다. (소속사 계정으로 로그인해주세요)");
                } else {
                    setError("매출 정보를 불러오는데 실패했습니다.");
                }
            } finally {
                setLoading(false);
            }
        };

        fetchRevenue();
    }, [agencyId]);

    if (loading) {
        return (
            <div className="py-20 flex flex-col items-center justify-center">
                <div className="w-8 h-8 border-4 border-idol-point border-t-transparent rounded-full animate-spin"></div>
                <p className="mt-4 text-gray-500 text-sm font-medium">데이터를 불러오는 중입니다...</p>
            </div>
        );
    }

    if (error) {
        return (
            <div className="py-16 flex flex-col items-center justify-center bg-gray-50/50 rounded-2xl border border-red-50">
                <div className="text-red-400 mb-4">
                    <svg className="w-12 h-12" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                    </svg>
                </div>
                <p className="text-lg font-bold text-gray-800 mb-2">오류가 발생했습니다</p>
                <p className="text-gray-500 text-sm">{error}</p>
            </div>
        );
    }

    const { totalRevenue, concertRevenue, subscriptionRevenue } = revenueData;

    // 계산 로직 (0 division 방지)
    const concertRatio = totalRevenue > 0 ? (concertRevenue / totalRevenue) * 100 : 0;
    const subscriptionRatio = totalRevenue > 0 ? (subscriptionRevenue / totalRevenue) * 100 : 0;

    return (
        <div className="space-y-8 animate-fade-in font-sans">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between border-b border-gray-100 pb-4 gap-4">
                <div>
                    <h2 className="text-2xl font-bold text-gray-900">소속사 관리 페이지</h2>
                    <p className="text-sm text-gray-500 mt-1">소속 아티스트들의 매출 확인 및 그룹 관리를 지원합니다.</p>
                </div>
                <div className="flex gap-2">
                    <button
                        onClick={() => setActiveTab('revenue')}
                        className={`px-4 py-2 text-sm font-bold rounded-lg transition-colors ${
                            activeTab === 'revenue' 
                                ? 'bg-gray-900 text-white shadow-sm' 
                                : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                        }`}
                    >
                        매출 통계
                    </button>
                    <button
                        onClick={() => setActiveTab('group')}
                        className={`px-4 py-2 text-sm font-bold rounded-lg transition-colors ${
                            activeTab === 'group' 
                                ? 'bg-gray-900 text-white shadow-sm' 
                                : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                        }`}
                    >
                        그룹 및 멤버 관리
                    </button>
                </div>
            </div>

            {activeTab === 'group' ? (
                <AgencyGroupTab agencyId={agencyId} />
            ) : (
                <>
                    {/* 메인 총 매출 카드 */}
            <motion.div
                initial={{ opacity: 0, scale: 0.98 }}
                animate={{ opacity: 1, scale: 1 }}
                className="bg-gradient-to-br from-gray-900 to-gray-800 rounded-2xl p-8 shadow-lg relative overflow-hidden"
            >
                {/* 카드 배경 장식 */}
                <div className="absolute top-0 right-0 -mt-10 -mr-10 w-40 h-40 bg-gradient-to-br from-idol-point to-purple-400 rounded-full opacity-20 blur-2xl"></div>
                <div className="absolute bottom-0 left-0 -mb-10 -ml-10 w-32 h-32 bg-gradient-to-tr from-blue-400 to-idol-point rounded-full opacity-20 blur-2xl"></div>

                <div className="relative z-10 flex flex-col">
                    <span className="text-sm font-medium text-white/80 bg-white/10 px-3 py-1 rounded-full inline-block mb-4 self-start backdrop-blur-sm border border-white/10">
                        총 누적 매출액
                    </span>
                    <div className="text-4xl font-black text-white tracking-tight flex items-baseline gap-2 mt-2">
                        {totalRevenue.toLocaleString()} <span className="text-xl font-bold text-white/70">원</span>
                    </div>
                </div>
            </motion.div>

            {/* 차트 및 세부 매출 섹션 */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {/* 콘서트 수익 카드 */}
                <div className="bg-white rounded-2xl p-6 shadow-sm border border-gray-100 flex flex-col">
                    <div className="flex items-center gap-3 mb-4">
                        <div className="w-10 h-10 rounded-xl bg-pink-50 flex items-center justify-center text-pink-500">
                            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 5v2m0 4v2m0 4v2M5 5a2 2 0 00-2 2v3a2 2 0 110 4v3a2 2 0 002 2h14a2 2 0 002-2v-3a2 2 0 110-4V7a2 2 0 00-2-2H5z"></path></svg>
                        </div>
                        <div>
                            <h3 className="text-base font-bold text-gray-800">콘서트 티켓 매출</h3>
                            <p className="text-xs text-gray-400">단독 콘서트 및 행사</p>
                        </div>
                    </div>
                    <div className="mt-auto pt-4 border-t border-gray-50">
                        <div className="text-2xl font-bold text-gray-800 mb-1">
                            {concertRevenue.toLocaleString()} <span className="text-sm text-gray-400">원</span>
                        </div>
                        <div className="w-full bg-gray-100 rounded-full h-1.5 mt-3">
                            <motion.div
                                initial={{ width: 0 }}
                                animate={{ width: `${concertRatio}%` }}
                                transition={{ duration: 1, ease: "easeOut" }}
                                className="bg-gradient-to-r from-pink-400 to-rose-500 h-1.5 rounded-full"
                            ></motion.div>
                        </div>
                        <p className="text-right text-xs font-semibold text-rose-500 mt-1.5">{concertRatio.toFixed(1)}%</p>
                    </div>
                </div>

                {/* 구독 수익 카드 */}
                <div className="bg-white rounded-2xl p-6 shadow-sm border border-gray-100 flex flex-col">
                    <div className="flex items-center gap-3 mb-4">
                        <div className="w-10 h-10 rounded-xl bg-blue-50 flex items-center justify-center text-blue-500">
                            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                        </div>
                        <div>
                            <h3 className="text-base font-bold text-gray-800">멤버십 구독 매출</h3>
                            <p className="text-xs text-gray-400">아이돌 및 그룹 정기 구독</p>
                        </div>
                    </div>
                    <div className="mt-auto pt-4 border-t border-gray-50">
                        <div className="text-2xl font-bold text-gray-800 mb-1">
                            {subscriptionRevenue.toLocaleString()} <span className="text-sm text-gray-400">원</span>
                        </div>
                        <div className="w-full bg-gray-100 rounded-full h-1.5 mt-3">
                            <motion.div
                                initial={{ width: 0 }}
                                animate={{ width: `${subscriptionRatio}%` }}
                                transition={{ duration: 1, ease: "easeOut" }}
                                className="bg-gradient-to-r from-indigo-400 to-blue-500 h-1.5 rounded-full"
                            ></motion.div>
                        </div>
                        <p className="text-right text-xs font-semibold text-blue-500 mt-1.5">{subscriptionRatio.toFixed(1)}%</p>
                    </div>
                </div>
            </div>

            {/* 매출 구조 분포도 */}
            <div className="bg-gray-50 rounded-2xl p-6 border border-gray-100">
                <h3 className="text-sm font-bold text-gray-700 mb-4">전체 매출 비중 분석</h3>
                {totalRevenue > 0 ? (
                    <div className="relative pt-1">
                        <div className="overflow-hidden h-4 mb-3 text-xs flex rounded-full bg-gray-200">
                            <motion.div
                                initial={{ width: 0 }}
                                animate={{ width: `${concertRatio}%` }}
                                transition={{ duration: 1.5, ease: "easeOut" }}
                                className="shadow-none flex flex-col text-center whitespace-nowrap text-white justify-center bg-gradient-to-r from-pink-400 to-rose-500"
                            ></motion.div>
                            <motion.div
                                initial={{ width: 0 }}
                                animate={{ width: `${subscriptionRatio}%` }}
                                transition={{ duration: 1.5, ease: "easeOut" }}
                                className="shadow-none flex flex-col text-center whitespace-nowrap text-white justify-center bg-gradient-to-r from-indigo-400 to-blue-500"
                            ></motion.div>
                        </div>
                        <div className="flex justify-between text-xs font-semibold text-gray-500 px-1">
                            <div className="flex items-center gap-1.5">
                                <span className="w-2 h-2 rounded-full bg-rose-500"></span>
                                <span>콘서트 티켓 ({concertRatio.toFixed(1)}%)</span>
                            </div>
                            <div className="flex items-center gap-1.5">
                                <span>멤버십 ({subscriptionRatio.toFixed(1)}%)</span>
                                <span className="w-2 h-2 rounded-full bg-blue-500"></span>
                            </div>
                        </div>
                    </div>
                ) : (
                    <div className="text-center py-4 text-gray-400 text-sm font-medium">
                        아직 기록된 매출 데이터가 없습니다.
                    </div>
                )}
            </div>
                </>
            )}
        </div>
    );
};

export default AgencyPage;
