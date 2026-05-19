import { useState, useRef } from 'react';
import type { ViewMode } from '../types/channel';

interface SettingsProps {
  isOpen: boolean;
  onClose: () => void;
  onLoadM3U: (content: string) => void;
  viewMode: ViewMode;
  onViewModeChange: (mode: ViewMode) => void;
  channelCount: number;
  onCheckAllStreams: () => void;
}

export default function Settings({
  isOpen,
  onClose,
  onLoadM3U,
  viewMode,
  onViewModeChange,
  channelCount,
  onCheckAllStreams,
}: SettingsProps) {
  const [urlInput, setUrlInput] = useState('');
  const [isLoadingUrl, setIsLoadingUrl] = useState(false);
  const [urlError, setUrlError] = useState('');
  const [uploadStatus, setUploadStatus] = useState('');
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const validExtensions = ['.m3u', '.m3u8', '.bin', '.txt'];
    const ext = '.' + file.name.split('.').pop()?.toLowerCase();
    if (!validExtensions.includes(ext)) {
      setUploadStatus('Unsupported file format. Use .m3u, .m3u8, .bin, or .txt');
      return;
    }

    const reader = new FileReader();
    reader.onload = (event) => {
      const content = event.target?.result as string;
      if (content) {
        onLoadM3U(content);
        setUploadStatus(`Loaded ${file.name} successfully!`);
        setTimeout(() => setUploadStatus(''), 3000);
      }
    };
    reader.onerror = () => {
      setUploadStatus('Error reading file');
    };
    reader.readAsText(file);
  };

  const handleUrlLoad = async () => {
    if (!urlInput.trim()) return;
    
    setIsLoadingUrl(true);
    setUrlError('');

    try {
      let fetchUrl = urlInput.trim();
      
      // GitHub URL conversion
      if (fetchUrl.includes('github.com') && !fetchUrl.includes('raw.githubusercontent.com')) {
        fetchUrl = fetchUrl
          .replace('github.com', 'raw.githubusercontent.com')
          .replace('/blob/', '/');
      }

      const response = await fetch(fetchUrl);
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      
      const content = await response.text();
      onLoadM3U(content);
      setUrlInput('');
      setUrlError('');
    } catch (err) {
      setUrlError(err instanceof Error ? err.message : 'Failed to load URL');
    } finally {
      setIsLoadingUrl(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="settings-overlay" onClick={onClose}>
      <div className="settings-panel" onClick={e => e.stopPropagation()}>
        <div className="settings-header">
          <h2>Settings</h2>
          <button className="close-btn" onClick={onClose}>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="24" height="24">
              <path d="M18 6 6 18M6 6l12 12" />
            </svg>
          </button>
        </div>

        <div className="settings-content">
          {/* Upload Section */}
          <section className="settings-section">
            <h3>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="20" height="20">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                <polyline points="17,8 12,3 7,8" />
                <line x1="12" y1="3" x2="12" y2="15" />
              </svg>
              Upload Playlist
            </h3>
            <p className="section-desc">Upload M3U, M3U8, BIN, or TXT playlist files</p>
            <div className="upload-area" onClick={() => fileInputRef.current?.click()}>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" width="48" height="48">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                <polyline points="17,8 12,3 7,8" />
                <line x1="12" y1="3" x2="12" y2="15" />
              </svg>
              <span>Click to upload or drag & drop</span>
              <span className="upload-formats">.m3u .m3u8 .bin .txt</span>
            </div>
            <input
              ref={fileInputRef}
              type="file"
              accept=".m3u,.m3u8,.bin,.txt"
              onChange={handleFileUpload}
              style={{ display: 'none' }}
            />
            {uploadStatus && <p className="upload-status">{uploadStatus}</p>}
          </section>

          {/* URL Section */}
          <section className="settings-section">
            <h3>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="20" height="20">
                <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71" />
                <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71" />
              </svg>
              Load from URL
            </h3>
            <p className="section-desc">Paste M3U playlist URL (supports GitHub links with auto-redirect)</p>
            <div className="url-input-group">
              <input
                type="url"
                placeholder="https://example.com/playlist.m3u or GitHub URL..."
                value={urlInput}
                onChange={e => setUrlInput(e.target.value)}
                onKeyDown={e => e.key === 'Enter' && handleUrlLoad()}
                className="url-input"
              />
              <button
                onClick={handleUrlLoad}
                disabled={isLoadingUrl || !urlInput.trim()}
                className="load-btn"
              >
                {isLoadingUrl ? (
                  <span className="btn-spinner"></span>
                ) : (
                  'Load'
                )}
              </button>
            </div>
            {urlError && <p className="url-error">{urlError}</p>}
          </section>

          {/* View Settings */}
          <section className="settings-section">
            <h3>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="20" height="20">
                <rect x="3" y="3" width="7" height="7" />
                <rect x="14" y="3" width="7" height="7" />
                <rect x="3" y="14" width="7" height="7" />
                <rect x="14" y="14" width="7" height="7" />
              </svg>
              Display
            </h3>
            <div className="view-toggle">
              <button
                className={`view-btn ${viewMode === 'grid' ? 'active' : ''}`}
                onClick={() => onViewModeChange('grid')}
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="18" height="18">
                  <rect x="3" y="3" width="7" height="7" />
                  <rect x="14" y="3" width="7" height="7" />
                  <rect x="3" y="14" width="7" height="7" />
                  <rect x="14" y="14" width="7" height="7" />
                </svg>
                Grid
              </button>
              <button
                className={`view-btn ${viewMode === 'list' ? 'active' : ''}`}
                onClick={() => onViewModeChange('list')}
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="18" height="18">
                  <line x1="8" y1="6" x2="21" y2="6" />
                  <line x1="8" y1="12" x2="21" y2="12" />
                  <line x1="8" y1="18" x2="21" y2="18" />
                  <line x1="3" y1="6" x2="3.01" y2="6" />
                  <line x1="3" y1="12" x2="3.01" y2="12" />
                  <line x1="3" y1="18" x2="3.01" y2="18" />
                </svg>
                List
              </button>
            </div>
          </section>

          {/* Stream Check */}
          <section className="settings-section">
            <h3>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="20" height="20">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
                <polyline points="22,4 12,14.01 9,11.01" />
              </svg>
              Stream Status
            </h3>
            <p className="section-desc">Check which streams are currently online</p>
            <button className="check-streams-btn" onClick={onCheckAllStreams}>
              Auto-Detect All Streams ({channelCount})
            </button>
          </section>

          {/* Info */}
          <section className="settings-section info-section">
            <div className="app-info">
              <h4>Universal IPTV Player</h4>
              <p>Powered by Shaka Player</p>
              <p>Supports HLS, DASH, ClearKey DRM</p>
              <p className="channel-stat">{channelCount} channels loaded</p>
            </div>
          </section>
        </div>
      </div>
    </div>
  );
}
