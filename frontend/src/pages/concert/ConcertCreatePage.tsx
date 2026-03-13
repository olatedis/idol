import React, { useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useAuthStore } from "../../stores/authStore";
import Header from "../main/Header";
import { api } from '../../api/axios';
import { showErrorToast, showSuccessToast } from "../../utils/alert";

const ConcertCreatePage: React.FC = () => {
    const { groupId } = useParams<{ groupId?: string }>();
    const navigate = useNavigate();
    const { user } = useAuthStore();

    const [formData, setFormData] = useState({
        title: "",
        description: "",
        venue: "",
        concertDate: "",
        startTime: "",
        price: "",
        totalTickets: "",
    });

    const [loading, setLoading] = useState(false);

    const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
        const { name, value } = e.target;
        setFormData((prev) => ({ ...prev, [name]: value }));
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        if (!user || user.role !== "AGENCY") {
            showErrorToast("권한이 없습니다.");
            return;
        }

        if (!formData.title || !formData.venue || !formData.concertDate) {
            showErrorToast("필수 항목을 입력해주세요.");
            return;
        }

        setLoading(true);
        try {
            const payload = {
                groupId: groupId ? parseInt(groupId) : null,
                title: formData.title,
                description: formData.description || null,
                venue: formData.venue,
                concertDate: formData.concertDate,
                startTime: formData.startTime || null,
                price: formData.price ? parseInt(formData.price) : null,
                totalTickets: formData.totalTickets ? parseInt(formData.totalTickets) : null,
                agencyId: user.agencyId,
            };

            await api.post("/concerts", payload);

            showSuccessToast("콘서트가 등록되었습니다.");
            navigate(-1); // 뒤로 가기
        } catch (error: any) {
            showErrorToast(error?.response?.data?.message || "콘서트 등록 실패");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="min-h-screen bg-idol-bg">
            <Header />
            <main className="pt-[80px] px-6">
                <div className="max-w-2xl mx-auto">
                    <h1 className="text-3xl font-bold mb-6">콘서트 등록</h1>

                    <form onSubmit={handleSubmit} className="bg-white rounded p-6 shadow space-y-4">
                        <div>
                            <label className="block text-sm font-medium mb-1">제목 *</label>
                            <input
                                type="text"
                                name="title"
                                value={formData.title}
                                onChange={handleChange}
                                className="w-full p-2 border rounded"
                                required
                            />
                        </div>

                        <div>
                            <label className="block text-sm font-medium mb-1">설명</label>
                            <textarea
                                name="description"
                                value={formData.description}
                                onChange={handleChange}
                                className="w-full p-2 border rounded"
                                rows={3}
                            />
                        </div>

                        <div>
                            <label className="block text-sm font-medium mb-1">장소 *</label>
                            <input
                                type="text"
                                name="venue"
                                value={formData.venue}
                                onChange={handleChange}
                                className="w-full p-2 border rounded"
                                required
                            />
                        </div>

                        <div>
                            <label className="block text-sm font-medium mb-1">콘서트 날짜 *</label>
                            <input
                                type="datetime-local"
                                name="concertDate"
                                value={formData.concertDate}
                                onChange={handleChange}
                                className="w-full p-2 border rounded"
                                required
                            />
                        </div>

                        <div>
                            <label className="block text-sm font-medium mb-1">시작 시간</label>
                            <input
                                type="time"
                                name="startTime"
                                value={formData.startTime}
                                onChange={handleChange}
                                className="w-full p-2 border rounded"
                            />
                        </div>

                        <div>
                            <label className="block text-sm font-medium mb-1">가격</label>
                            <input
                                type="number"
                                name="price"
                                value={formData.price}
                                onChange={handleChange}
                                className="w-full p-2 border rounded"
                                placeholder="예: 50000"
                            />
                        </div>

                        <div>
                            <label className="block text-sm font-medium mb-1">총 티켓 수</label>
                            <input
                                type="number"
                                name="totalTickets"
                                value={formData.totalTickets}
                                onChange={handleChange}
                                className="w-full p-2 border rounded"
                                placeholder="예: 1000"
                            />
                        </div>

                        <div className="flex gap-4 pt-4">
                            <button
                                type="submit"
                                disabled={loading}
                                className="flex-1 py-2 bg-idol-point text-white rounded hover:opacity-90 disabled:bg-gray-300"
                            >
                                {loading ? "등록 중..." : "등록하기"}
                            </button>
                            <button
                                type="button"
                                onClick={() => navigate(-1)}
                                className="flex-1 py-2 border rounded hover:bg-gray-50"
                            >
                                취소
                            </button>
                        </div>
                    </form>
                </div>
            </main>
        </div>
    );
};

export default ConcertCreatePage;