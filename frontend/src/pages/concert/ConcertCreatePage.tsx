import React, {useState} from "react";
import {useNavigate, useParams} from "react-router-dom";
import {useAuthStore} from "../../stores/authStore";
import Header from "../main/Header";
import {api} from '../../api/axios';
import {showErrorToast, showSuccessToast} from "../../utils/alert";

const ConcertCreatePage: React.FC = () => {
    const {groupId} = useParams<{ groupId?: string }>();
    const navigate = useNavigate();
    const {user} = useAuthStore();


    const [seats, setSeats] = useState<{ grade: string; count: string; price: string }[]>([
        {grade: "VIP", count: "", price: ""},
        {grade: "R", count: "", price: ""},
        {grade: "S", count: "", price: ""},
        {grade: "A", count: "", price: ""},
    ]);

    const [formData, setFormData] = useState({
        title: "",
        description: "",
        venue: "",
        concertDate: "",
        ticketSaleDate: "",
    });

    const [loading, setLoading] = useState(false);

    const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
        const {name, value} = e.target;
        setFormData((prev) => ({...prev, [name]: value}));
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        if (!user || user.role !== "AGENCY") {
            showErrorToast("권한이 없습니다.");
            return;
        }

        if (!formData.title || !formData.venue || !formData.concertDate || !formData.ticketSaleDate) {
            showErrorToast("필수 항목을 입력해주세요.");
            return;
        }

        setLoading(true);
        try {
            const validSeats = seats.filter(s => s.count && s.price).map(s => ({
                grade: s.grade,
                count: parseInt(s.count),
                price: parseInt(s.price),
            }));

            if (validSeats.length === 0) {
                showErrorToast("최소 하나의 좌석 등급을 입력해주세요.");
                return;
            }

            const userId = user?.userId ?? 0;
            const groupIdNum = groupId ? Number(groupId) : null;

            if (userId <= 0) {
                showErrorToast("소속사 정보가 없습니다.");
                return;
            }
            const agencyId = await api.get("/agency/id")
            const payload = {
                agencyId: agencyId.data,
                groupId: groupIdNum,
                title: formData.title,
                description: formData.description || null,
                venue: formData.venue,
                concertDate: formData.concertDate,
                ticketSaleDate: formData.ticketSaleDate,
                seats: validSeats,
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
            <Header/>
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
                            <label className="block text-sm font-medium mb-1">티켓 예매 시작일 *</label>
                            <input
                                type="datetime-local"
                                name="ticketSaleDate"
                                value={formData.ticketSaleDate}
                                onChange={handleChange}
                                className="w-full p-2 border rounded"
                            />
                        </div>

                        <div>
                            <label className="block text-sm font-medium mb-3">좌석 등급 설정 *</label>
                            <div className="space-y-3">
                                {seats.map((seat, index) => (
                                    <div key={seat.grade} className="flex gap-4 items-center">
                                        <span className="w-12 font-medium">{seat.grade}</span>
                                        <div className="flex-1">
                                            <input
                                                type="number"
                                                placeholder="좌석 수"
                                                value={seat.count}
                                                onChange={(e) => {
                                                    const newSeats = [...seats];
                                                    newSeats[index].count = e.target.value;
                                                    setSeats(newSeats);
                                                }}
                                                className="w-full p-2 border rounded"
                                                min="0"
                                            />
                                        </div>
                                        <div className="flex-1">
                                            <input
                                                type="number"
                                                placeholder="가격"
                                                value={seat.price}
                                                onChange={(e) => {
                                                    const newSeats = [...seats];
                                                    newSeats[index].price = e.target.value;
                                                    setSeats(newSeats);
                                                }}
                                                className="w-full p-2 border rounded"
                                                min="0"
                                            />
                                        </div>
                                    </div>
                                ))}
                            </div>
                            <p className="text-xs text-gray-500 mt-2">각 등급별 좌석 수와 가격을 입력하세요. 빈 등급은 등록되지 않습니다.</p>
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