import React, { useState, useEffect } from 'react';

interface SafeImageProps extends React.ImgHTMLAttributes<HTMLImageElement> {
    fallbackSrc?: string;
    text?: string; // 대체 이미지에 표시할 텍스트
}

/**
 * 이미지 로드 실패 시 자동으로 대체 이미지를 보여주는 컴포넌트
 */
const SafeImage: React.FC<SafeImageProps> = ({ 
    src, 
    fallbackSrc, 
    alt, 
    className, 
    text = "No Image",
    ...props 
}) => {
    const defaultFallback = `https://placehold.jp/24/f0f0f0/888888/400x400.png?text=${encodeURIComponent(text)}`;
    const [imgSrc, setImgSrc] = useState<string | undefined>(src);
    const [hasError, setHasError] = useState(false);

    useEffect(() => {
        setImgSrc(src);
        setHasError(false);
    }, [src]);

    const handleError = () => {
        if (!hasError) {
            setHasError(true);
            setImgSrc(fallbackSrc || defaultFallback);
        }
    };

    return (
        <img
            src={imgSrc || defaultFallback}
            alt={alt}
            className={className}
            onError={handleError}
            {...props}
        />
    );
};

export default SafeImage;
