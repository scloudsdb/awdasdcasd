import type { Channel, ViewMode } from '../types/channel';

interface ChannelListProps {
  channels: Channel[];
  selectedChannel: Channel | null;
  onSelectChannel: (channel: Channel) => void;
  viewMode: ViewMode;
  searchQuery: string;
  onSearchChange: (query: string) => void;
  selectedGroup: string;
  groups: string[];
  onGroupChange: (group: string) => void;
}

export default function ChannelList({
  channels,
  selectedChannel,
  onSelectChannel,
  viewMode,
  searchQuery,
  onSearchChange,
  selectedGroup,
  groups,
  onGroupChange,
}: ChannelListProps) {
  const filteredChannels = channels.filter(ch => {
    const matchesSearch = ch.name.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesGroup = selectedGroup === 'All' || ch.group === selectedGroup;
    return matchesSearch && matchesGroup;
  });

  return (
    <div className="channel-list-container">
      <div className="channel-filters">
        <div className="search-box">
          <svg className="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <circle cx="11" cy="11" r="8" />
            <path d="m21 21-4.35-4.35" />
          </svg>
          <input
            type="text"
            placeholder="Search channels..."
            value={searchQuery}
            onChange={e => onSearchChange(e.target.value)}
            className="search-input"
          />
          {searchQuery && (
            <button className="search-clear" onClick={() => onSearchChange('')}>×</button>
          )}
        </div>
        <div className="group-filter">
          <select
            value={selectedGroup}
            onChange={e => onGroupChange(e.target.value)}
            className="group-select"
          >
            <option value="All">All Groups ({channels.length})</option>
            {groups.map(g => (
              <option key={g} value={g}>
                {g} ({channels.filter(c => c.group === g).length})
              </option>
            ))}
          </select>
        </div>
      </div>

      <div className="channel-count">
        {filteredChannels.length} channel{filteredChannels.length !== 1 ? 's' : ''}
      </div>

      <div className={`channel-grid ${viewMode}`}>
        {filteredChannels.map(channel => (
          <div
            key={channel.id}
            className={`channel-card ${selectedChannel?.id === channel.id ? 'active' : ''} ${channel.status || ''}`}
            onClick={() => onSelectChannel(channel)}
            tabIndex={0}
            role="button"
            onKeyDown={e => e.key === 'Enter' && onSelectChannel(channel)}
          >
            <div className="channel-logo-wrapper">
              {channel.logo ? (
                <img
                  src={channel.logo}
                  alt={channel.name}
                  className="channel-logo"
                  loading="lazy"
                  onError={e => {
                    (e.target as HTMLImageElement).style.display = 'none';
                    (e.target as HTMLImageElement).nextElementSibling?.classList.remove('hidden');
                  }}
                />
              ) : null}
              <div className={`channel-logo-fallback ${channel.logo ? 'hidden' : ''}`}>
                {channel.name.charAt(0).toUpperCase()}
              </div>
              <div className={`status-dot ${channel.status || 'unknown'}`} />
            </div>
            <div className="channel-info">
              <span className="channel-name">{channel.name}</span>
              <span className="channel-group-tag">{channel.group}</span>
              <div className="channel-badges">
                {channel.url.endsWith('.mpd') && <span className="badge dash">DASH</span>}
                {channel.url.includes('.m3u8') && <span className="badge hls">HLS</span>}
                {channel.drmType && <span className="badge drm">{channel.drmType === 'clearkey' ? 'CK' : 'WV'}</span>}
              </div>
            </div>
          </div>
        ))}
        {filteredChannels.length === 0 && (
          <div className="no-channels">
            <p>No channels found</p>
          </div>
        )}
      </div>
    </div>
  );
}
