export interface Channel {
  id: string;
  name: string;
  logo: string;
  group: string;
  url: string;
  userAgent?: string;
  referrer?: string;
  origin?: string;
  drmType?: 'clearkey' | 'widevine';
  drmKey?: string;
  drmLicenseUrl?: string;
  status?: 'unknown' | 'checking' | 'online' | 'offline';
  tvgId?: string;
}

export interface ChannelGroup {
  name: string;
  channels: Channel[];
}

export type ViewMode = 'grid' | 'list';
export type DeviceType = 'mobile' | 'tablet' | 'tv' | 'desktop';
