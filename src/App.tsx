import { useState, useEffect, useMemo, useCallback } from 'react';
import VideoPlayer from './components/VideoPlayer';
import ChannelList from './components/ChannelList';
import Settings from './components/Settings';
import type { Channel, ViewMode } from './types/channel';
import { parseM3U } from './utils/m3uParser';
import { DEFAULT_M3U } from './data/defaultPlaylist';
import { detectDevice } from './utils/deviceDetect';
import './App.css';

function App() {
  const [channels, setChannels] = useState<Channel[]>([]);
  const [selectedChannel, setSelectedChannel] = useState<Channel | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedGroup, setSelectedGroup] = useState('All');
  const [viewMode, setViewMode] = useState<ViewMode>('grid');
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [deviceType, setDeviceType] = useState(detectDevice());

  useEffect(() => {
    const defaultChannels = parseM3U(DEFAULT_M3U);
    setChannels(defaultChannels);
  }, []);

  useEffect(() => {
    const handleResize = () => setDeviceType(detectDevice());
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  useEffect(() => {
    if (deviceType === 'mobile') {
      setSidebarCollapsed(true);
    }
  }, [deviceType]);

  const groups = useMemo(() => {
    const groupSet = new Set(channels.map(ch => ch.group));
    return Array.from(groupSet).sort();
  }, [channels]);

  const handleLoadM3U = useCallback((content: string) => {
    const parsed = parseM3U(content);
    if (parsed.length > 0) {
      setChannels(prev => {
        const existingUrls = new Set(prev.map(c => c.url));
        const newChannels = parsed.filter(c => !existingUrls.has(c.url));
        return [...prev, ...newChannels];
      });
    }
  }, []);

  const handleStatusChange = useCallback((channelId: string, status: 'online' | 'offline') => {
    setChannels(prev => prev.map(ch =>
      ch.id === channelId ? { ...ch, status } : ch
    ));
  }, []);

  const handleCheckAllStreams = useCallback(async () => {
    setChannels(prev => prev.map(ch => ({ ...ch, status: 'checking' as const })));

    for (const channel of channels) {
      try {
        const controller = new AbortController();
        const timeout = setTimeout(() => controller.abort(), 8000);

        const response = await fetch(channel.url, {
          method: 'HEAD',
          mode: 'no-cors',
          signal: controller.signal,
        });

        clearTimeout(timeout);

        setChannels(prev => prev.map(ch =>
          ch.id === channel.id
            ? { ...ch, status: (response.type === 'opaque' || response.ok) ? 'online' : 'offline' }
            : ch
        ));
      } catch {
        setChannels(prev => prev.map(ch =>
          ch.id === channel.id ? { ...ch, status: 'offline' as const } : ch
        ));
      }
    }
  }, [channels]);

  const handleSelectChannel = useCallback((channel: Channel) => {
    setSelectedChannel(channel);
    if (deviceType === 'mobile') {
      setSidebarCollapsed(true);
    }
  }, [deviceType]);

  return (
    <div className={`app ${deviceType}`}>
      {/* Header */}
      <header className="app-header">
        <div className="header-left">
          <button
            className="menu-toggle"
            onClick={() => setSidebarCollapsed(!sidebarCollapsed)}
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="24" height="24">
              {sidebarCollapsed ? (
                <>
                  <line x1="3" y1="12" x2="21" y2="12" />
                  <line x1="3" y1="6" x2="21" y2="6" />
                  <line x1="3" y1="18" x2="21" y2="18" />
                </>
              ) : (
                <path d="M18 6 6 18M6 6l12 12" />
              )}
            </svg>
          </button>
          <div className="app-brand">
            <svg viewBox="0 0 24 24" fill="currentColor" width="28" height="28">
              <path d="M21 3H3c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h5v2h8v-2h5c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 14H3V5h18v12zM8 15l5-3.5L8 8v7z"/>
            </svg>
            <h1>Universal IPTV</h1>
          </div>
        </div>
        <div className="header-right">
          <span className="device-badge">{deviceType}</span>
          <button className="settings-btn" onClick={() => setSettingsOpen(true)}>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="22" height="22">
              <circle cx="12" cy="12" r="3" />
              <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z" />
            </svg>
          </button>
        </div>
      </header>

      {/* Main Content */}
      <div className="app-content">
        {/* Sidebar */}
        <aside className={`sidebar ${sidebarCollapsed ? 'collapsed' : ''}`}>
          <ChannelList
            channels={channels}
            selectedChannel={selectedChannel}
            onSelectChannel={handleSelectChannel}
            viewMode={viewMode}
            searchQuery={searchQuery}
            onSearchChange={setSearchQuery}
            selectedGroup={selectedGroup}
            groups={groups}
            onGroupChange={setSelectedGroup}
          />
        </aside>

        {/* Player Area */}
        <main className="player-area">
          <VideoPlayer
            channel={selectedChannel}
            onStatusChange={handleStatusChange}
          />
        </main>
      </div>

      {/* Mobile overlay when sidebar is open */}
      {!sidebarCollapsed && deviceType === 'mobile' && (
        <div className="mobile-overlay" onClick={() => setSidebarCollapsed(true)} />
      )}

      {/* Settings Modal */}
      <Settings
        isOpen={settingsOpen}
        onClose={() => setSettingsOpen(false)}
        onLoadM3U={handleLoadM3U}
        viewMode={viewMode}
        onViewModeChange={setViewMode}
        channelCount={channels.length}
        onCheckAllStreams={handleCheckAllStreams}
      />
    </div>
  );
}

export default App;
