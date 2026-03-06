import React, { useState } from "react";
import AdminReportQueue from "./AdminReportQueue";
import AdminUserSearch from "./AdminUserSearch";

const AdminPage: React.FC = () => {
    const [subTab, setSubTab] = useState<"reports" | "search">("reports");

    return (
        <div className="space-y-6 animate-fade-in">
            {/* 페이지 헤더 */}
            <div className="flex items-center justify-between border-b border-gray-100 pb-4">
                <div>
                    <h2 className="text-2xl font-bold text-gray-900">관리자 대시보드</h2>
                    <p className="text-sm text-gray-500 mt-1">유저 신고 누적 대기열 및 검색 기능을 통해 플랫폼을 관리합니다.</p>
                </div>
                <div className="px-4 py-2 bg-gray-900 text-white text-sm font-semibold rounded-lg shadow-sm">
                    최고 관리자
                </div>
            </div>

            {/* 서브 탭 */}
            <div className="flex space-x-2 border-b border-gray-100">
                <button
                    className={`px-4 py-2 text-sm font-medium transition-colors ${subTab === "reports" ? "text-idol border-b-2 border-idol" : "text-gray-500 hover:text-gray-700"}`}
                    onClick={() => setSubTab("reports")}
                >
                    신고 대기열
                </button>
                <button
                    className={`px-4 py-2 text-sm font-medium transition-colors ${subTab === "search" ? "text-idol border-b-2 border-idol" : "text-gray-500 hover:text-gray-700"}`}
                    onClick={() => setSubTab("search")}
                >
                    유저 검색
                </button>
            </div>

            {/* 컨텐츠 영역 */}
            <div className="pt-2">
                {subTab === "reports" && <AdminReportQueue />}
                {subTab === "search" && <AdminUserSearch />}
            </div>
        </div>
    );
};

export default AdminPage;
