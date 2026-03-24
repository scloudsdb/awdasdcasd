require("dotenv").config();
const express = require("express");
const axios = require("axios");
const jwt = require("jsonwebtoken");
const cors = require("cors");

const app = express();
app.use(cors());
app.use(express.json());

// ================= CONFIG =================
const CLIENT_ID = process.env.CLIENT_ID;
const CLIENT_SECRET = process.env.CLIENT_SECRET;
const REDIRECT_URI = process.env.REDIRECT_URI;
const GUILD_ID = process.env.GUILD_ID;
const JWT_SECRET = process.env.JWT_SECRET;

// ================= TRANSLATE =================
function t(lang, id, en) {
  return lang === "id" ? id : en;
}

// ================= LOGIN =================
app.get("/login", (req, res) => {

  const lang = req.query.lang || "en";

  const url =
    `https://discord.com/api/oauth2/authorize` +
    `?client_id=${CLIENT_ID}` +
    `&redirect_uri=${encodeURIComponent(REDIRECT_URI)}` +
    `&response_type=code` +
    `&scope=identify guilds`;

  res.redirect(url);
});

// ================= CALLBACK =================
app.get("/callback", async (req, res) => {
    console.log("QUERY:", req.query);

  const code = req.query.code;
  const lang = req.query.lang || "en";

  try {

    // 🔑 GET ACCESS TOKEN
    const tokenRes = await axios.post(
      "https://discord.com/api/oauth2/token",
      new URLSearchParams({
        client_id: CLIENT_ID,
        client_secret: CLIENT_SECRET,
        grant_type: "authorization_code",
        code,
        redirect_uri: REDIRECT_URI,
      }),
      { headers: { "Content-Type": "application/x-www-form-urlencoded" } }
    );

    const accessToken = tokenRes.data.access_token;

    // 👤 GET USER
    const userRes = await axios.get(
      "https://discord.com/api/users/@me",
      { headers: { Authorization: `Bearer ${accessToken}` } }
    );

    const user = userRes.data;

    // 🏠 GET GUILDS
    const guildsRes = await axios.get(
      "https://discord.com/api/users/@me/guilds",
      { headers: { Authorization: `Bearer ${accessToken}` } }
    );

    const guilds = guildsRes.data;

    const isInGuild = guilds.some(g => g.id === GUILD_ID);

    // ❌ NOT JOIN SERVER
    if (!isInGuild) {
      return res.send(`
        <h2>❌ ${t(lang,
          "Kamu harus join server Discord dulu!",
          "You must join the Discord server first!"
        )}</h2>
      `);
    }

    // 🔐 GENERATE TOKEN
    const token = jwt.sign(
      {
        id: user.id,
        username: user.username
      },
      JWT_SECRET,
      { expiresIn: "7d" }
    );

    // ✅ SUCCESS PAGE
    res.send(`
      <h2>✅ ${t(lang,
        "Login Berhasil!",
        "Login Success!"
      )}</h2>

      <p>${t(lang,
        "Copy token ini ke aplikasi:",
        "Copy this token into the app:"
      )}</p>

      <textarea style="width:400px;height:120px">${token}</textarea>
    `);

  } catch (err) {
    console.error(err);

    res.send(`
      <h2>❌ ${t(lang,
        "Terjadi error saat login",
        "Login error occurred"
      )}</h2>
    `);
  }
});

// ================= VERIFY =================
app.get("/verify", (req, res) => {

  const token = req.headers.authorization;

  if (!token) {
    return res.json({ valid: false });
  }

  try {

    const decoded = jwt.verify(token, JWT_SECRET);

    res.json({
      valid: true,
      user: decoded
    });

  } catch {
    res.json({ valid: false });
  }

});

// ================= START =================
app.listen(3000, () => {
  console.log("🚀 Server running on port 3000");
});