import axios from "axios";
import jwt from "jsonwebtoken";

export default async function handler(req, res) {
  const code = req.query.code;

  if (!code) {
    return res.send("No code provided");
  }

  try {
    // tukar code → access token
    const tokenRes = await axios.post(
      "https://discord.com/api/oauth2/token",
      new URLSearchParams({
        client_id: process.env.DISCORD_CLIENT_ID,
        client_secret: process.env.DISCORD_CLIENT_SECRET,
        grant_type: "authorization_code",
        code,
        redirect_uri: process.env.DISCORD_REDIRECT_URI,
      }),
      {
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
      }
    );

    const access_token = tokenRes.data.access_token;

    // ambil user info
    const userRes = await axios.get(
      "https://discord.com/api/users/@me",
      {
        headers: {
          Authorization: `Bearer ${access_token}`,
        },
      }
    );

    const user = userRes.data;

    // 🎟️ generate JWT
    const token = jwt.sign(
      {
        id: user.id,
        username: user.username,
      },
      process.env.JWT_SECRET,
      { expiresIn: "7d" }
    );

    return res.send(`
      <h2>Login Success</h2>
      <p>Copy token:</p>
      <textarea style="width:100%;height:120px">${token}</textarea>
    `);

  } catch (err) {
    console.error(err.response?.data || err);
    res.send("Login error");
  }
}