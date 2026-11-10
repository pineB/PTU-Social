import { useEffect, useState, type ReactNode } from 'react';
import apiClient from '../api/client';

interface AuthenticatedImageProps {
  src?: string;
  alt: string;
  className?: string;
  fallback?: ReactNode;
}

export default function AuthenticatedImage({ src, alt, className, fallback }: AuthenticatedImageProps) {
  const [objectUrl, setObjectUrl] = useState('');

  useEffect(() => {
    if (!src) {
      setObjectUrl('');
      return;
    }

    const controller = new AbortController();
    let active = true;
    let currentObjectUrl = '';
    setObjectUrl('');
    apiClient.get<Blob>(src, { responseType: 'blob', signal: controller.signal }).then(({ data }) => {
      if (active) {
        currentObjectUrl = URL.createObjectURL(data);
        setObjectUrl(currentObjectUrl);
      }
    }).catch(() => {
      if (active) setObjectUrl('');
    });

    return () => {
      active = false;
      controller.abort();
      if (currentObjectUrl) URL.revokeObjectURL(currentObjectUrl);
    };
  }, [src]);

  return objectUrl
    ? <img src={objectUrl} alt={alt} className={className} />
    : fallback ?? null;
}
