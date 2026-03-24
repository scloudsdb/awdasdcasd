import axios from "axios";
import jwt from "jsonwebtoken";

export default async function handler(req, res) {
  const code = req.query.code;

  // ❌ No code
  if (!code) {
    return res.status(400).send(`
      <h2>❌ Error</h2>
      <p>No authorization code provided.</p>
    `);
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

    // 👤 Fetch user info
    const userResponse = await axios.get(
      "https://discord.com/api/users/@me",
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
      }
    );

    const user = userResponse.data;

    // 🎟️ Generate JWT
    const jwtToken = jwt.sign(
      {
        id: user.id,
        username: user.username,
      },
      process.env.JWT_SECRET,
      { expiresIn: "7d" }
    );

    // ✅ Success UI
    return res.send(`
      <html>
        <head>
          <title>Login Success</title>
        </head>
        <body style="font-family:sans-serif;text-align:center;padding:40px">
          <h2>✅ Login Successful</h2>
          <p>Your token:</p>
          <textarea style="width:100%;max-width:600px;height:120px">${jwtToken}</textarea>
          <br/><br/>
          <small>You can now copy this token and use it in the app.</small>
        </body>
      </html>
    `);

  } catch (error) {
    console.error("OAuth Error:", error.response?.data || error.message);

    return res.status(500).send(`
      <h2>❌ Login Failed</h2>
      <p>Something went wrong during authentication.</p>
    `);
  }
}
