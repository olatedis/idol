import Swal from 'sweetalert2';

// 기본 설정 (커스텀 클래스 적용)
const Toast = Swal.mixin({
    toast: true,
    position: 'top-end',
    showConfirmButton: false,
    timer: 3000,
    timerProgressBar: true,
    didOpen: (toast) => {
        toast.addEventListener('mouseenter', Swal.stopTimer);
        toast.addEventListener('mouseleave', Swal.resumeTimer);
    }
});

// 성공 알림 (토스트)
export const showSuccessToast = (message: string) => {
    Toast.fire({
        icon: 'success',
        title: message,
        background: '#fff',
        color: '#333',
        iconColor: '#FF6B6B' // 아이돌 포인트 컬러 (예시)
    });
};

// 에러 알림 (토스트)
export const showErrorToast = (message: string) => {
    Toast.fire({
        icon: 'error',
        title: message
    });
};

// 일반 알림 (모달)
export const showAlert = (title: string, text: string, icon: 'success' | 'error' | 'warning' | 'info' = 'info') => {
    return Swal.fire({
        title,
        text,
        icon,
        confirmButtonColor: '#FF6B6B', // 아이돌 포인트 컬러
        confirmButtonText: '확인'
    });
};

// 확인 모달 (Yes/No)
export const showConfirm = async (title: string, text: string, confirmText: string = '확인') => {
    const result = await Swal.fire({
        title,
        text,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#FF6B6B',
        cancelButtonColor: '#d33',
        confirmButtonText: confirmText,
        cancelButtonText: '취소'
    });
    return result.isConfirmed;
};
