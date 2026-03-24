import axios from "axios";
import jwt from "jsonwebtoken";

export default async function handler(req, res) {
  const code = req.query.code;

  // ❌ No code
  if (!code) {
    return res.status(400).send("No authorization code provided");
  }

  try {
    // 🔁 Exchange code → access token
    const tokenResponse = await axios.post(
      "https://discord.com/api/oauth2/token",
      new URLSearchParams({
        client_id: process.env.DISCORD_CLIENT_ID,
        client_secret: process.env.DISCORD_CLIENT_SECRET,
        grant_type: "authorization_code",
        code: code,
        redirect_uri: process.env.DISCORD_REDIRECT_URI,
      }),
      {
        headers: {
          "Content-Type": "application/x-www-form-urlencoded",
        },
      }
    );

    const accessToken = tokenResponse.data.access_token;

    // 👤 Get user info
    const userResponse = await axios.get(
      "https://discord.com/api/users/@me",
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
      }
    );

    const user = userResponse.data;

    // 🏠 Get user guilds
    const guildResponse = await axios.get(
      "https://discord.com/api/users/@me/guilds",
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
      }
    );

    const guilds = guildResponse.data;

    const isMember = guilds.some(
      (g) => g.id === process.env.GUILD_ID
    );

    // ❌ NOT JOIN SERVER
    if (!isMember) {
      return res.send(`
        <html>
          <body style="font-family:sans-serif;text-align:center;padding:40px">
            <h2>❌ Access Denied</h2>
            <p>You must join our Discord server first.</p>
            <a href="https://discord.gg/Qsp6Sbq6wy" target="_blank">
              <button style="padding:10px 20px;font-size:16px">
                Join Server
              </button>
            </a>
          </body>
        </html>
      `);
    }

    // 🎟️ Generate JWT
    const jwtToken = jwt.sign(
      {
        id: user.id,
        username: user.username,
      },
      process.env.JWT_SECRET,
      { expiresIn: "7d" }
    );
      return res.send(`
<html>
  <body style="background:#0f1720;color:white;text-align:center;font-family:sans-serif;padding-top:50px">
    <h3>Logging you in...</h3>

    <script>
      window.location.href = "steamclouds://auth?token=${jwtToken}";
      setTimeout(() => window.close(), 1500);
    </script>

    <p>If nothing happens:</p>
    <a href="steamclouds://auth?token=${jwtToken}">Open App</a>
  </body>
</html>
`);

  } catch (error) {
    console.error("OAuth Error:", error.response?.data || error.message);

    return res.status(500).send("Login failed");
  }
}
