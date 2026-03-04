import React from "react";

const AdminPage: React.FC = () => {
    return (
        <div className="space-y-8 animate-fade-in">
            {/* 페이지 헤더 */}
            <div className="flex items-center justify-between border-b border-gray-100 pb-4">
                <div>
                    <h2 className="text-2xl font-bold text-gray-900">관리자 대시보드</h2>
                    <p className="text-sm text-gray-500 mt-1">플랫폼 전체 현황 및 설정을 관리합니다.</p>
                </div>
                <div className="px-4 py-2 bg-gray-900 text-white text-sm font-semibold rounded-lg shadow-sm">
                    최고 관리자
                </div>
            </div>
        </div>
    );
};

export default AdminPage;
