export default function handler(req, res) {
  const redirect = process.env.DISCORD_REDIRECT_URI;

  console.log("REDIRECT:", redirect);

  const params = new URLSearchParams({
    client_id: process.env.DISCORD_CLIENT_ID,
    response_type: "code",
    redirect_uri: redirect,
    scope: "identify guilds",
  });

  res.redirect(`https://discord.com/oauth2/authorize?${params}`);
}
