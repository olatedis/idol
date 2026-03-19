import React, { useState, useRef } from "react";
import { api } from "../../../api/axios";
import { useAuthStore } from "../../../stores/authStore";
import { showAlert, showConfirm, showErrorToast, showSuccessToast } from "../../../utils/alert";
import SafeImage from "../../../components/common/SafeImage";

interface UserMyPageDto {
    userId: number; // mapped from id in backend
    id?: number;
    username: string;
    email: string;
    nickname: string;
    role: string;
    provider?: string;
    profileImageUrl?: string; // mapped from profileImage
    profileImage?: string;
    createdAt?: string;
    phone?: string;
    address?: string;
}

interface ProfileTabProps {
    userInfo: UserMyPageDto;
    onRefresh: () => void;
}

const ProfileTab: React.FC<ProfileTabProps> = ({ userInfo, onRefresh }) => {
    // === State ===
    const [isEditing, setIsEditing] = useState(false);
    const [editForm, setEditForm] = useState({
        nickname: userInfo.nickname || "",
        email: userInfo.email || "",
        phone: userInfo.phone || "",
        address: userInfo.address || "",
        stageName: "",
    });

    const [idolInfo, setIdolInfo] = useState<any>(null);

    React.useEffect(() => {
        if (userInfo.role === "IDOL") {
            api.get("/idols/me")
                .then(res => {
                    setIdolInfo(res.data);
                    setEditForm(prev => ({ ...prev, stageName: res.data.stageName || "" }));
                })
                .catch(() => { });
        }
    }, [userInfo.role]);

    const [isPwdModalOpen, setIsPwdModalOpen] = useState(false);
    const [pwdForm, setPwdForm] = useState({ currentPassword: "", newPassword: "" });

    const [isWithdrawModalOpen, setIsWithdrawModalOpen] = useState(false);
    const [withdrawPwd, setWithdrawPwd] = useState("");

    const fileInputRef = useRef<HTMLInputElement>(null);

    // === Handlers ===
    const handleImageUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
        if (!e.target.files || e.target.files.length === 0) return;
        const file = e.target.files[0];

        const formData = new FormData();
        formData.append("file", file);

        try {
            await api.post("/users/me/image", formData, {
                headers: { "Content-Type": "multipart/form-data" }
            });
            showSuccessToast("프로필 이미지가 변경되었습니다.");

            // 페이지를 자동으로 새로고침하여 변경된 이미지를 즉시 반영
            onRefresh();
            window.location.reload();
        } catch (err: any) {
            showErrorToast(err?.response?.data?.message || "이미지 변경 에러가 발생했습니다.");
        }
    };

    const handleUpdateProfile = async () => {
        try {
            await Promise.all([
                api.post("/users/me/update", {
                    nickname: editForm.nickname,
                    email: editForm.email,
                    phone: editForm.phone,
                    address: editForm.address
                }),
                userInfo.role === "IDOL" && editForm.stageName !== idolInfo?.stageName
                    ? api.post("/idols/me/update", { stageName: editForm.stageName })
                    : Promise.resolve()
            ]);

            showSuccessToast("회원 정보가 수정되었습니다.");
            setIsEditing(false);
            onRefresh();
        } catch (err: any) {
            showErrorToast(err?.response?.data?.message || "정보 수정에 실패했습니다.");
        }
    };

    const handleChangePassword = async () => {
        if (pwdForm.newPassword.length < 8) {
            showAlert("경고", "새 비밀번호는 8자 이상이어야 합니다.", "warning");
            return;
        }
        try {
            await api.post("/users/password/change", pwdForm);
            showSuccessToast("비밀번호가 성공적으로 변경되었습니다.");
            setIsPwdModalOpen(false);
            setPwdForm({ currentPassword: "", newPassword: "" });
        } catch (err: any) {
            showErrorToast(err?.response?.data?.message || "비밀번호 변경에 실패했습니다.");
        }
    };

    const handleWithdraw = async () => {
        const ok = await showConfirm("정말 탈퇴하시겠습니까?", "관련 데이터가 모두 삭제되며 복구할 수 없습니다.", "탈퇴");

        if (!ok) return;

        if (!userInfo.provider && !withdrawPwd) {
            showAlert("경고", "비밀번호를 입력해주세요.", "warning");
            return;
        }

        try {
            await api.post("/users/withdraw", { password: userInfo.provider ? "" : withdrawPwd });
            await showSuccessToast("회원 탈퇴가 완료되었습니다. 이용해 주셔서 감사합니다.");
            useAuthStore.getState().logout(); // 세션 정리 (로그아웃)
            window.location.href = "/"; // 메인으로 이동
        } catch (err: any) {
            showErrorToast(err?.response?.data?.message || "회원 탈퇴에 실패했습니다.");
        }
    };

    const displayImageUrl = userInfo.profileImageUrl || userInfo.profileImage;

    return (
        <div className="space-y-8 relative">
            {/* 상단 프로필 이미지 & 기본 정보 */}
            <div className="flex flex-col sm:flex-row items-center sm:items-start space-y-4 sm:space-y-0 sm:space-x-6 bg-white p-6 rounded-2xl border border-gray-100 shadow-sm relative overflow-hidden">
                <div className="absolute top-0 right-0 w-32 h-32 bg-idol/5 rounded-bl-full pointer-events-none" />

                <div className="relative group cursor-pointer" onClick={() => fileInputRef.current?.click()}>
                    <div className="w-24 h-24 rounded-full bg-gray-200 overflow-hidden ring-4 ring-white shadow-md">
                        {displayImageUrl ? (
                            <SafeImage src={displayImageUrl} alt="프로필" className="w-full h-full object-cover" text="User" />
                        ) : (
                            <svg className="w-full h-full text-gray-400 p-4" fill="currentColor" viewBox="0 0 24 24">
                                <path d="M24 20.993V24H0v-2.996A14.977 14.977 0 0112.004 15c4.904 0 9.26 2.354 11.996 5.993zM16.002 8.999a4 4 0 11-8 0 4 4 0 018 0z" />
                            </svg>
                        )}
                    </div>
                    <div className="absolute inset-0 bg-black/40 rounded-full flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
                        <span className="text-white text-xs font-semibold">변경</span>
                    </div>
                </div>
                <input type="file" ref={fileInputRef} className="hidden" accept="image/*" onChange={handleImageUpload} />

                <div className="flex-1 text-center sm:text-left">
                    <h2 className="text-2xl font-bold text-gray-900">{userInfo.nickname}</h2>
                    <p className="text-gray-500 mt-1">{userInfo.email}</p>
                    <div className="mt-3 flex items-center justify-center sm:justify-start space-x-2">
                        <span className="px-3 py-1 bg-idol/10 text-idol text-xs font-bold rounded border border-idol/20">
                            {userInfo.role}
                        </span>
                        <span className="text-xs text-gray-400 border border-gray-200 px-2 py-1 rounded">
                            가입일: {userInfo.createdAt?.split('T')[0] || '-'}
                        </span>
                    </div>
                </div>
                <div className="pt-2 sm:pt-0">
                    <button
                        onClick={() => setIsEditing(!isEditing)}
                        className="px-4 py-2 border border-gray-200 text-gray-600 text-sm font-semibold rounded-lg hover:bg-gray-50 transition-colors"
                    >
                        {isEditing ? "취소" : "정보 수정"}
                    </button>
                </div>
            </div>

            {/* 정보 수정 폼 or 정보 표시 뷰 */}
            <div className="bg-white p-6 rounded-2xl border border-gray-100 shadow-sm">
                <h3 className="text-lg font-bold text-gray-800 mb-6 border-b border-gray-100 pb-2">기본 정보</h3>

                {isEditing ? (
                    <div className="space-y-4">
                        {userInfo.role === "IDOL" && (
                            <div>
                                <label className="block text-sm font-bold text-idol mb-1">활동명 (스테이지 네임)</label>
                                <input
                                    type="text"
                                    className="w-full px-4 py-2 bg-idol/5 border border-idol/20 rounded-lg focus:outline-none focus:ring-2 focus:ring-idol/50 text-idol font-semibold"
                                    value={editForm.stageName}
                                    placeholder="활동명을 입력하세요"
                                    onChange={e => setEditForm({ ...editForm, stageName: e.target.value })}
                                />
                            </div>
                        )}
                        <div>
                            <label className="block text-sm font-semibold text-gray-700 mb-1">닉네임</label>
                            <input
                                type="text"
                                className="w-full px-4 py-2 bg-gray-50 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-idol/50"
                                value={editForm.nickname}
                                onChange={e => setEditForm({ ...editForm, nickname: e.target.value })}
                            />
                        </div>
                        <div>
                            <label className="block text-sm font-semibold text-gray-700 mb-1">이메일</label>
                            <input
                                type="email"
                                className="w-full px-4 py-2 bg-gray-50 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-idol/50"
                                value={editForm.email}
                                onChange={e => setEditForm({ ...editForm, email: e.target.value })}
                            />
                        </div>
                        <div>
                            <label className="block text-sm font-semibold text-gray-700 mb-1">연락처</label>
                            <input
                                type="text"
                                className="w-full px-4 py-2 bg-gray-50 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-idol/50"
                                value={editForm.phone}
                                placeholder="010-0000-0000"
                                onChange={e => setEditForm({ ...editForm, phone: e.target.value })}
                            />
                        </div>
                        <div>
                            <label className="block text-sm font-semibold text-gray-700 mb-1">주소</label>
                            <input
                                type="text"
                                className="w-full px-4 py-2 bg-gray-50 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-idol/50"
                                value={editForm.address}
                                placeholder="배송지 주소를 입력하세요"
                                onChange={e => setEditForm({ ...editForm, address: e.target.value })}
                            />
                        </div>
                        <div className="pt-4 flex justify-end">
                            <button
                                onClick={handleUpdateProfile}
                                className="px-6 py-2 bg-idol text-white font-semibold rounded-lg hover:bg-idol-hover transition-colors shadow-md shadow-idol/20"
                            >
                                저장하기
                            </button>
                        </div>
                    </div>
                ) : (
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-y-6 gap-x-8">
                        {userInfo.role === "IDOL" && idolInfo?.stageName && (
                            <div className="md:col-span-2 p-4 bg-idol/5 border border-idol/10 rounded-xl mb-2">
                                <p className="text-xs text-idol/70 font-bold mb-1">활동명 (스테이지 네임)</p>
                                <p className="text-idol font-bold text-lg">{idolInfo.stageName}</p>
                            </div>
                        )}
                        <div>
                            <p className="text-xs text-gray-400 font-semibold mb-1">아이디</p>
                            <p className="text-gray-900 font-medium">{userInfo.username}</p>
                        </div>
                        <div>
                            <p className="text-xs text-gray-400 font-semibold mb-1">닉네임</p>
                            <p className="text-gray-900 font-medium">{userInfo.nickname}</p>
                        </div>
                        <div>
                            <p className="text-xs text-gray-400 font-semibold mb-1">이메일</p>
                            <p className="text-gray-900 font-medium">{userInfo.email}</p>
                        </div>
                        <div>
                            <p className="text-xs text-gray-400 font-semibold mb-1">연락처</p>
                            <p className="text-gray-900 font-medium">{userInfo.phone || "등록된 연락처가 없습니다."}</p>
                        </div>
                        <div className="md:col-span-2">
                            <p className="text-xs text-gray-400 font-semibold mb-1">주소</p>
                            <p className="text-gray-900 font-medium">{userInfo.address || "등록된 주소가 없습니다."}</p>
                        </div>
                    </div>
                )}
            </div>

            {/* 보안 및 계정 관리 */}
            <div className="bg-white p-6 rounded-2xl border border-gray-100 shadow-sm mt-8">
                <h3 className="text-lg font-bold text-gray-800 mb-4">보안 및 계정 관리</h3>
                <div className="flex flex-col sm:flex-row space-y-3 sm:space-y-0 sm:space-x-4">
                    {!userInfo.provider && (
                        <button
                            onClick={() => setIsPwdModalOpen(true)}
                            className="px-4 py-2.5 border border-gray-300 text-gray-700 font-semibold rounded-lg hover:bg-gray-50 transition-colors"
                        >
                            비밀번호 변경
                        </button>
                    )}
                </div>
            </div>

            {/* 비밀번호 변경 모달 */}
            {isPwdModalOpen && (
                <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
                    <div className="bg-white rounded-2xl max-w-sm w-full p-6 shadow-xl border border-gray-100 relative">
                        <button
                            onClick={() => setIsPwdModalOpen(false)}
                            className="absolute top-4 right-4 text-gray-400 hover:text-gray-600"
                        >
                            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" /></svg>
                        </button>

                        <h3 className="text-xl font-bold text-gray-900 mb-6">비밀번호 변경</h3>
                        <div className="space-y-4">
                            <div>
                                <label className="block text-sm font-semibold text-gray-700 mb-1">현재 비밀번호</label>
                                <input
                                    type="password"
                                    className="w-full px-4 py-2 bg-gray-50 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-idol/50"
                                    value={pwdForm.currentPassword}
                                    onChange={e => setPwdForm({ ...pwdForm, currentPassword: e.target.value })}
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-semibold text-gray-700 mb-1">새 비밀번호</label>
                                <input
                                    type="password"
                                    className="w-full px-4 py-2 bg-gray-50 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-idol/50"
                                    value={pwdForm.newPassword}
                                    placeholder="8자 이상 특수문자 포함 권장"
                                    onChange={e => setPwdForm({ ...pwdForm, newPassword: e.target.value })}
                                />
                            </div>
                            <button
                                onClick={handleChangePassword}
                                className="w-full mt-2 py-3 bg-gray-900 text-white font-semibold rounded-lg hover:bg-black transition-colors"
                            >
                                변경하기
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* 하단 회원 탈퇴 */}
            <div className="flex justify-end pt-8 pb-4">
                <button
                    onClick={() => setIsWithdrawModalOpen(true)}
                    className="text-xs text-gray-400 hover:text-red-500 underline underline-offset-4 transition-colors"
                >
                    회원 탈퇴 구역
                </button>
            </div>

            {/* 회원 탈퇴 모달 */}
            {isWithdrawModalOpen && (
                <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
                    <div className="bg-white rounded-2xl max-w-sm w-full p-6 shadow-xl border border-red-100 relative">
                        <button
                            onClick={() => setIsWithdrawModalOpen(false)}
                            className="absolute top-4 right-4 text-gray-400 hover:text-gray-600"
                        >
                            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" /></svg>
                        </button>

                        <div className="text-center mb-6">
                            <div className="w-12 h-12 bg-red-100 text-red-500 rounded-full flex items-center justify-center mx-auto mb-3">
                                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" /></svg>
                            </div>
                            <h3 className="text-xl font-bold text-gray-900">정말 탈퇴하시겠습니까?</h3>
                            <p className="text-sm text-gray-500 mt-2">탈퇴 시 모든 구독 및 결제 내역이 삭제되며 복구할 수 없습니다.</p>
                        </div>
                        <div className="space-y-4">
                            {!userInfo.provider && (
                                <div>
                                    <label className="block text-sm font-semibold text-gray-700 mb-1">비밀번호 확인</label>
                                    <input
                                        type="password"
                                        className="w-full px-4 py-2 bg-gray-50 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-red-500/50"
                                        value={withdrawPwd}
                                        placeholder="본인 확인을 위해 입력해주세요"
                                        onChange={e => setWithdrawPwd(e.target.value)}
                                    />
                                </div>
                            )}
                            <button
                                onClick={handleWithdraw}
                                className="w-full mt-2 py-3 bg-red-500 text-white font-semibold rounded-lg hover:bg-red-600 transition-colors shadow-md shadow-red-500/20"
                            >
                                네, 탈퇴합니다
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default ProfileTab;
