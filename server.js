import express from "express";
import fetch from "node-fetch";

const app = express();

const CLIENT_ID = "ISI_CLIENT_ID";
const CLIENT_SECRET = "ISI_CLIENT_SECRET";
const REDIRECT_URI = "https://your-app.up.railway.app/callback";

const GUILD_ID = "ID_SERVER_DISCORD_LU";

app.get("/", (req, res) => {
  res.send("Backend running");
});

// 🔑 STEP LOGIN
app.get("/login", (req, res) => {
  const url = `https://discord.com/api/oauth2/authorize?client_id=${CLIENT_ID}&redirect_uri=${encodeURIComponent(REDIRECT_URI)}&response_type=code&scope=identify%20guilds`;
  res.redirect(url);
});

// 🔁 CALLBACK
app.get("/callback", async (req, res) => {
  const code = req.query.code;

  const tokenRes = await fetch("https://discord.com/api/oauth2/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      client_id: CLIENT_ID,
      client_secret: CLIENT_SECRET,
      grant_type: "authorization_code",
      code,
      redirect_uri: REDIRECT_URI
    })
  });

  const tokenData = await tokenRes.json();

  const userRes = await fetch("https://discord.com/api/users/@me", {
    headers: { Authorization: `Bearer ${tokenData.access_token}` }
  });

  const user = await userRes.json();

  const guildRes = await fetch("https://discord.com/api/users/@me/guilds", {
    headers: { Authorization: `Bearer ${tokenData.access_token}` }
  });

  const guilds = await guildRes.json();

  const isMember = guilds.some(g => g.id === GUILD_ID);

  if (!isMember) {
    return res.send("❌ Join Discord server dulu bro");
  }

  // ✅ USER VALID
  res.send(`
    <h1>Login berhasil ✅</h1>
    <p>ID: ${user.id}</p>
    <p>Username: ${user.username}</p>
    <script>
      window.location.href = "steamclouds://auth?user=${user.id}";
    </script>
  `);
});

app.listen(3000, () => {
  console.log("Server running");
});