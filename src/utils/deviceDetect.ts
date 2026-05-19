import type { DeviceType } from '../types/channel';

export function detectDevice(): DeviceType {
  const ua = navigator.userAgent.toLowerCase();
  const width = window.innerWidth;

  const isTv = ua.includes('smart-tv') || ua.includes('smarttv') ||
    ua.includes('tv browser') || ua.includes('netcast') ||
    ua.includes('webos') || ua.includes('tizen') ||
    ua.includes('crkey') || ua.includes('firetv') ||
    ua.includes('android tv') || ua.includes('bravia') ||
    width >= 1920;

  if (isTv) return 'tv';
  if (width <= 480) return 'mobile';
  if (width <= 1024) return 'tablet';
  return 'desktop';
}

export function getGridColumns(device: DeviceType, sidebarOpen: boolean): number {
  switch (device) {
    case 'mobile': return 1;
    case 'tablet': return sidebarOpen ? 2 : 3;
    case 'desktop': return sidebarOpen ? 3 : 4;
    case 'tv': return sidebarOpen ? 4 : 5;
    default: return 3;
  }
}
