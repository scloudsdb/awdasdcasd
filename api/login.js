export default function handler(req, res) {
  const redirect = encodeURIComponent(process.env.DISCORD_REDIRECT_URI);

  const url =
    `https://discord.com/oauth2/authorize` +
    `?client_id=${process.env.DISCORD_CLIENT_ID}` +
    `&response_type=code` +
    `&redirect_uri=${redirect}` +
    `&scope=identify guilds`;

  res.redirect(url);
}