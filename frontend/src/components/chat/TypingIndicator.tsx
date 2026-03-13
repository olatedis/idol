import React from "react";

interface TypingIndicatorProps {
    profileImage?: string;
    stageName?: string;
}

const TypingIndicator: React.FC<TypingIndicatorProps> = ({ profileImage, stageName }) => {
    return (
        <div className="flex justify-start shrink-0 transform transition-all">
            <div className="w-8 h-8 rounded-full bg-gray-200 mr-2 overflow-hidden shrink-0 border border-[var(--color-idol)]/20">
                {profileImage ? (
                    <img src={profileImage} alt="profile" className="w-full h-full object-cover" />
                ) : (
                    <div className="w-full h-full flex items-center justify-center bg-[var(--color-idol-point)] text-white font-bold text-xs">
                        {stageName?.substring(0, 1) || "I"}
                    </div>
                )}
            </div>
            <div className="px-4 py-3 rounded-2xl shadow-sm border bg-white border-[var(--color-idol-point)]/40 rounded-tl-sm text-gray-400 flex items-center mb-1">
                <span className="flex space-x-1.5 items-center justify-center h-4">
                    <span className="w-1.5 h-1.5 bg-[var(--color-idol-point)]/60 rounded-full animate-bounce" style={{ animationDelay: '0ms' }}></span>
                    <span className="w-1.5 h-1.5 bg-[var(--color-idol-point)]/60 rounded-full animate-bounce" style={{ animationDelay: '150ms' }}></span>
                    <span className="w-1.5 h-1.5 bg-[var(--color-idol-point)]/60 rounded-full animate-bounce" style={{ animationDelay: '300ms' }}></span>
                </span>
            </div>
        </div>
    );
};

export default React.memo(TypingIndicator);
