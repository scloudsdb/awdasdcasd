---
name: testing-iptv-player
description: Test the Universal IPTV Player app end-to-end. Use when verifying channel list, search, settings, Shaka Player playback, or M3U parser changes.
---

# Testing Universal IPTV Player

## Dev Server Setup

```bash
cd /home/ubuntu/universal-iptv
npm install
npm run dev -- --host 0.0.0.0 --port 5173
```

The dev server runs on localhost (port 5173 or next available). No authentication required.

## Key UI Paths

- **Channel List**: Sidebar on the left, shows all channels in grid or list view
- **Search**: Text input at top of sidebar — filters channels by name (case-insensitive)
- **Group Filter**: Dropdown below search — filters by group (e.g., "INDONESIA")
- **Settings**: Gear icon in top-right header corner
  - Upload Playlist: Accepts .m3u, .m3u8, .bin, .txt files
  - Load from URL: Paste URL input with GitHub auto-redirect
  - Display: Grid/List view toggle
  - Stream Status: Auto-Detect All Streams button
  - Info: Shows "Powered by Shaka Player" and channel count
- **Video Player**: Main area — shows placeholder until channel selected, then loading/playing/error state
- **Channel Selection**: Click any channel card to trigger Shaka Player load

## What to Test

1. **Channel count**: Should match the number of channels in `src/data/defaultPlaylist.ts`
2. **Search filtering**: Type a channel name, verify count drops and only matching channels show
3. **Channel selection**: Click a card, verify player transitions from placeholder to loading/error state
4. **Error handling**: Verify error message and Retry button appear on failed streams
5. **Settings panel**: Open via gear icon, verify all sections present with correct channel count
6. **View mode**: Toggle Grid/List in Settings, verify layout changes
7. **Badge display**: Verify HLS/DASH/CK badges on channel cards match URL type and DRM config

## Known Limitations

- **CORS restrictions**: Streams will fail to play from localhost/browser due to CORS. This is expected — the streams require specific HTTP headers (referrer, user-agent) that browsers restrict. Test that Shaka Player *attempts* to load (shows loading spinner then error), not that it plays successfully.
- **Widevine badges**: Channels with `com.widevine.alpha` DRM might not show WV badges due to M3U parser `pendingProps` reset timing. This is a known minor issue.
- **Shaka Player chunk size**: Build produces a large chunk (~1MB) due to Shaka Player library. This is expected.

## Deployed Version

The app may be deployed at a devinapps.com subdomain. Check the PR description for the current deployment URL.

## Build & Lint

```bash
npm run build    # Vite production build
npm run lint     # ESLint check
```

## Devin Secrets Needed

None — this app runs entirely client-side with no authentication required.
