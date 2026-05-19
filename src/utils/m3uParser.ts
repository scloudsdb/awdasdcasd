import type { Channel } from '../types/channel';

let idCounter = 0;

function generateId(): string {
  return `ch_${Date.now()}_${idCounter++}`;
}

export function parseM3U(content: string): Channel[] {
  const lines = content.split('\n').map(l => l.trim()).filter(l => l.length > 0);
  const channels: Channel[] = [];

  let currentChannel: Partial<Channel> = {};
  let pendingProps: Record<string, string> = {};

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];

    if (line.startsWith('#EXTM3U')) {
      continue;
    }

    if (line.startsWith('#KODIPROP:')) {
      const prop = line.substring('#KODIPROP:'.length).trim();
      if (prop.startsWith('inputstream.adaptive.license_type=')) {
        const licType = prop.split('=')[1].trim().toLowerCase();
        if (licType === 'clearkey' || licType === 'org.w3.clearkey') {
          pendingProps.drmType = 'clearkey';
        } else if (licType === 'com.widevine.alpha') {
          pendingProps.drmType = 'widevine';
        }
      } else if (prop.startsWith('inputstream.adaptive.license_key=')) {
        const keyVal = prop.substring('inputstream.adaptive.license_key='.length).trim();
        if (keyVal.startsWith('http')) {
          pendingProps.drmLicenseUrl = keyVal;
        } else {
          pendingProps.drmKey = keyVal;
        }
      }
      continue;
    }

    if (line.startsWith('#EXTVLCOPT:')) {
      const opt = line.substring('#EXTVLCOPT:'.length).trim();
      if (opt.startsWith('http-user-agent=')) {
        pendingProps.userAgent = opt.substring('http-user-agent='.length).replace(/^["']|["']$/g, '');
      } else if (opt.startsWith('http-referrer=') || opt.startsWith('http-referer=')) {
        const key = opt.startsWith('http-referrer=') ? 'http-referrer=' : 'http-referer=';
        pendingProps.referrer = opt.substring(key.length).replace(/^["']|["']$/g, '').trim();
      } else if (opt.startsWith('http-origin=')) {
        pendingProps.origin = opt.substring('http-origin='.length).replace(/^["']|["']$/g, '').trim();
      }
      continue;
    }

    if (line.startsWith('#EXTINF:')) {
      const extinf = line.substring('#EXTINF:'.length);
      
      const nameMatch = extinf.lastIndexOf(',');
      const name = nameMatch >= 0 ? extinf.substring(nameMatch + 1).trim() : 'Unknown';
      const attrs = nameMatch >= 0 ? extinf.substring(0, nameMatch) : extinf;

      const groupMatch = attrs.match(/group-title="([^"]*)"/i);
      const logoMatch = attrs.match(/tvg-logo="([^"]*)"/i);
      const tvgIdMatch = attrs.match(/tvg-id="([^"]*)"/i);

      currentChannel = {
        name,
        group: groupMatch ? groupMatch[1] : 'Ungrouped',
        logo: logoMatch ? logoMatch[1] : '',
        tvgId: tvgIdMatch ? tvgIdMatch[1] : undefined,
        ...pendingProps,
      };
      
      if (pendingProps.drmType) {
        currentChannel.drmType = pendingProps.drmType as 'clearkey' | 'widevine';
      }
      if (pendingProps.drmKey) {
        currentChannel.drmKey = pendingProps.drmKey;
      }
      if (pendingProps.drmLicenseUrl) {
        currentChannel.drmLicenseUrl = pendingProps.drmLicenseUrl;
      }
      
      continue;
    }

    if (line.startsWith('#')) {
      if (line.startsWith('#http')) {
        // commented out URL, skip
      }
      continue;
    }

    if (line.match(/^https?:\/\//)) {
      if (currentChannel.name) {
        channels.push({
          id: generateId(),
          name: currentChannel.name || 'Unknown',
          logo: currentChannel.logo || '',
          group: currentChannel.group || 'Ungrouped',
          url: line,
          userAgent: currentChannel.userAgent,
          referrer: currentChannel.referrer,
          origin: currentChannel.origin,
          drmType: currentChannel.drmType,
          drmKey: currentChannel.drmKey,
          drmLicenseUrl: currentChannel.drmLicenseUrl,
          tvgId: currentChannel.tvgId,
          status: 'unknown',
        });
      }
      currentChannel = {};
      pendingProps = {};
    }
  }

  return channels;
}

export async function fetchM3UFromUrl(url: string): Promise<string> {
  // Handle GitHub URLs - convert to raw
  let fetchUrl = url.trim();
  if (fetchUrl.includes('github.com') && !fetchUrl.includes('raw.githubusercontent.com')) {
    fetchUrl = fetchUrl
      .replace('github.com', 'raw.githubusercontent.com')
      .replace('/blob/', '/');
  }

  const response = await fetch(fetchUrl);
  if (!response.ok) {
    throw new Error(`Failed to fetch: ${response.status} ${response.statusText}`);
  }
  return response.text();
}
