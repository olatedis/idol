import React from "react";

const AgencyPage: React.FC = () => {
    return (
        <div className="space-y-8 animate-fade-in">
            {/* 페이지 헤더 */}
            <div className="flex items-center justify-between border-b border-gray-100 pb-4">
                <div>
                    <h2 className="text-2xl font-bold text-gray-900">소속사 대시보드</h2>
                    <p className="text-sm text-gray-500 mt-1">소속 아티스트와 팬덤, 정산 현황을 관리합니다.</p>
                </div>
                <div className="px-4 py-2 bg-idol/10 text-idol text-sm font-bold rounded-lg border border-idol/20 shadow-sm">
                    AGENCY
                </div>
            </div>
        </div>
    );
};

export default AgencyPage;
