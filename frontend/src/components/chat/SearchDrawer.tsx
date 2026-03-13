import React from "react";
import type { ChatMessage } from "../../types/chat";

interface SearchDrawerProps {
    isOpen: boolean;
    onClose: () => void;
    keyword: string;
    onKeywordChange: (val: string) => void;
    onSearch: (e?: React.FormEvent) => void;
    isSearching: boolean;
    results: ChatMessage[];
}

const SearchDrawer: React.FC<SearchDrawerProps> = ({
    isOpen,
    onClose,
    keyword,
    onKeywordChange,
    onSearch,
    isSearching,
    results
}) => {
    if (!isOpen) return null;

    return (
        <div className="absolute top-[68px] right-0 sm:right-4 w-full sm:w-80 h-[calc(100%-140px)] bg-white/95 backdrop-blur-md shadow-2xl z-30 flex flex-col transform transition-transform rounded-xl border border-gray-200 overflow-hidden">
            <div className="p-3 border-b bg-gray-50 flex items-center shrink-0">
                <form onSubmit={onSearch} className="flex flex-1 items-center bg-white rounded-lg border px-3 py-1.5 focus-within:ring-1 focus-within:ring-[var(--color-idol)]">
                    <input
                        type="text"
                        placeholder="채팅 내역 검색..."
                        value={keyword}
                        onChange={(e) => onKeywordChange(e.target.value)}
                        className="flex-1 outline-none text-sm bg-transparent"
                    />
                    <button type="submit" disabled={isSearching} className="text-gray-400 hover:text-[var(--color-idol)] p-1">
                        {isSearching ? (
                            <svg className="animate-spin h-4 w-4" fill="none" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path></svg>
                        ) : (
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path></svg>
                        )}
                    </button>
                </form>
                <button onClick={onClose} className="ml-2 text-gray-400 p-1 hover:text-gray-800 rounded-lg hover:bg-gray-200">
                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"></path></svg>
                </button>
            </div>
            <div className="flex-1 overflow-y-auto p-3 space-y-3 custom-scrollbar text-sm">
                {results.length > 0 ? (
                    results.map((res: ChatMessage, idx) => (
                        <div key={idx} className="bg-white border rounded-lg p-2.5 shadow-sm hover:shadow-md transition-shadow">
                            <div className="flex justify-between items-center mb-1">
                                <span className="font-bold text-[var(--color-idol-dark)] text-xs">{res.senderNickname || '알 수 없음'}</span>
                                <span className="text-[10px] text-gray-400">{res.createdAt ? new Date(res.createdAt).toLocaleString('ko-KR', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }) : ''}</span>
                            </div>
                            <p className="text-gray-700 leading-snug break-words" dangerouslySetInnerHTML={{ __html: res.content.replace(new RegExp(keyword, 'gi'), match => `<mark class="bg-yellow-200 rounded px-0.5 text-[var(--color-idol-dark)] font-medium">${match}</mark>`) }}></p>
                        </div>
                    ))
                ) : (
                    <div className="flex flex-col items-center justify-center h-full text-gray-400 space-y-2 opacity-70 pb-10">
                        <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M8 16l2.879-2.879m0 0a3 3 0 104.243-4.242 3 3 0 00-4.243 4.242zM21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                        <span>{isSearching ? '검색 중...' : '검색 결과가 없습니다.'}</span>
                    </div>
                )}
            </div>
        </div>
    );
};

export default React.memo(SearchDrawer);
