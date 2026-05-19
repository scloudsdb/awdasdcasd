import { useEffect, useRef, useCallback, useState } from 'react';
import shaka from 'shaka-player/dist/shaka-player.compiled';
import type { Channel } from '../types/channel';

interface VideoPlayerProps {
  channel: Channel | null;
  onError?: (error: string) => void;
  onStatusChange?: (channelId: string, status: 'online' | 'offline') => void;
}

export default function VideoPlayer({ channel, onError, onStatusChange }: VideoPlayerProps) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const playerRef = useRef<shaka.Player | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const initPlayer = useCallback(async () => {
    if (!videoRef.current) return;

    shaka.polyfill.installAll();

    if (!shaka.Player.isBrowserSupported()) {
      setErrorMsg('Browser not supported for Shaka Player');
      return;
    }

    if (playerRef.current) {
      await playerRef.current.destroy();
    }

    const player = new shaka.Player();
    await player.attach(videoRef.current);
    playerRef.current = player;

    player.addEventListener('error', (event: Event) => {
      const detail = (event as CustomEvent).detail;
      console.error('Shaka error:', detail);
      const msg = detail?.message || 'Playback failed';
      setErrorMsg(`Error: ${msg}`);
      if (channel && onStatusChange) {
        onStatusChange(channel.id, 'offline');
      }
      onError?.(msg);
    });
  }, [channel, onError, onStatusChange]);

  const loadChannel = useCallback(async () => {
    if (!channel || !playerRef.current || !videoRef.current) return;

    setIsLoading(true);
    setErrorMsg(null);

    try {
      const player = playerRef.current;

      player.configure({
        streaming: {
          bufferingGoal: 30,
          rebufferingGoal: 2,
          bufferBehind: 30,
          retryParameters: {
            maxAttempts: 4,
            baseDelay: 1000,
            backoffFactor: 2,
            fuzzFactor: 0.5,
          },
        },
        manifest: {
          retryParameters: {
            maxAttempts: 4,
            baseDelay: 1000,
            backoffFactor: 2,
            fuzzFactor: 0.5,
          },
        },
      });

      if (channel.drmType === 'clearkey' && channel.drmKey) {
        const parts = channel.drmKey.split(':');
        if (parts.length === 2) {
          const keyId = parts[0].trim();
          const key = parts[1].trim();
          player.configure('drm.clearKeys', { [keyId]: key });
        }
      }

      if (channel.userAgent || channel.referrer || channel.origin) {
        player.getNetworkingEngine()?.registerRequestFilter(
          (_type: shaka.net.NetworkingEngine.RequestType, request: shaka.extern.Request) => {
            if (channel.userAgent) {
              request.headers['User-Agent'] = channel.userAgent;
            }
            if (channel.referrer) {
              request.headers['Referer'] = channel.referrer;
            }
            if (channel.origin) {
              request.headers['Origin'] = channel.origin;
            }
          }
        );
      }

      await player.load(channel.url);
      setIsLoading(false);

      if (onStatusChange) {
        onStatusChange(channel.id, 'online');
      }

      videoRef.current.play().catch(() => {});
    } catch (err) {
      setIsLoading(false);
      const msg = err instanceof Error ? err.message : 'Failed to load stream';
      setErrorMsg(msg);
      if (channel && onStatusChange) {
        onStatusChange(channel.id, 'offline');
      }
      onError?.(msg);
    }
  }, [channel, onError, onStatusChange]);

  useEffect(() => {
    initPlayer();
    return () => {
      playerRef.current?.destroy();
    };
  }, [initPlayer]);

  useEffect(() => {
    if (channel && playerRef.current) {
      loadChannel();
    }
  }, [channel, loadChannel]);

  return (
    <div className="video-player-container">
      {!channel && (
        <div className="video-placeholder">
          <div className="placeholder-icon">📺</div>
          <p>Select a channel to start watching</p>
        </div>
      )}
      {isLoading && channel && (
        <div className="video-loading">
          <div className="spinner"></div>
          <p>Loading {channel.name}...</p>
        </div>
      )}
      {errorMsg && (
        <div className="video-error">
          <div className="error-icon">⚠️</div>
          <p>{errorMsg}</p>
          <button onClick={loadChannel} className="retry-btn">Retry</button>
        </div>
      )}
      <video
        ref={videoRef}
        autoPlay
        controls
        playsInline
        className="video-element"
        style={{ display: channel && !errorMsg ? 'block' : 'none' }}
      />
      {channel && !errorMsg && !isLoading && (
        <div className="now-playing">
          {channel.logo && <img src={channel.logo} alt="" className="now-playing-logo" />}
          <div className="now-playing-info">
            <span className="now-playing-name">{channel.name}</span>
            <span className="now-playing-group">{channel.group}</span>
            {channel.drmType && <span className="drm-badge">{channel.drmType.toUpperCase()}</span>}
          </div>
        </div>
      )}
    </div>
  );
}
