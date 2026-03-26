# G2-Link — Complete Launch Guide

> Offline mesh communication. Works without internet, SIM, or cell signal.
> Total annual cost: $12 (domain only).

---

## Step 1 — Setup GitHub (10 minutes)

1. Go to **github.com** → Create account (free)
2. Create new repository → Name it `g2link` → Set to **Public**
3. Upload all project files from this zip
4. Go to **Settings → Pages** → Source: GitHub Actions ✓
5. Go to **Settings → Actions → General** → Allow all actions ✓

Your website will be live at:
`https://G2-links.github.io/g2link`

---

## Step 2 — Get Your Domain (optional, $12/yr)

Buy `g2link.app` or `g2-link.app` from:
- **Namecheap** — ~$12/yr
- **Cloudflare Registrar** — at cost, no markup

Then in GitHub Pages settings → add your custom domain.

---

## Step 3 — Set Up Cloudflare (free, 5 minutes)

1. Go to **cloudflare.com** → Create free account
2. Add your domain → Follow DNS setup steps
3. Enable **Proxy** (orange cloud) on your DNS records
4. **Analytics** tab → instantly see visitors, countries, devices

Free tier gives you:
- Unlimited bandwidth
- Global CDN (200+ cities)
- DDoS protection
- SSL certificate
- Visitor analytics

---

## Step 4 — Update These Placeholders

Before building, replace in all files:

| Placeholder | Replace With |
|---|---|
| `your-github-username` | G2-links ✓ already set |
| `g2link` | Your repo name if different |
| `YOUR_TOKEN_HERE` | Cloudflare Analytics token (optional) |

Files to update:
- `app/build.gradle.kts` — GITHUB_OWNER, GITHUB_REPO
- `website/index.html` — GITHUB_OWNER, GITHUB_REPO (lines 260–261)
- `sharing/ApkShareManager.kt` — GITHUB_DOWNLOAD_URL
- `AndroidManifest.xml` — already done

---

## Step 5 — Build Your First APK

### Option A: Android Studio (recommended first time)
1. Open Android Studio → Open Project → select G2Link folder
2. Wait for Gradle sync (~2 min first time)
3. Build → Build Bundle(s)/APK(s) → Build APK(s)
4. APK at: `app/build/outputs/apk/debug/app-debug.apk`

### Option B: Command Line
```bash
# Requires JDK 17 installed
./gradlew assembleDebug
```

### Option C: GitHub Actions (zero local setup)
1. Push code to GitHub
2. Go to Actions tab → Run workflow manually
3. Download APK from the Artifacts section

---

## Step 6 — Create Your First Release

```bash
# Tag your release — this triggers automatic APK build + website deploy
git tag v1.0.0
git push origin v1.0.0
```

GitHub Actions automatically:
1. Builds the release APK
2. Creates a GitHub Release with the APK attached
3. Deploys your website to GitHub Pages
4. APK download link: `github.com/G2-links/g2link/releases/latest`

---

## Step 7 — Track Downloads

**GitHub (built-in, zero setup):**
- Go to your release → each APK file shows exact download count
- `github.com/G2-links/g2link/releases`

**Cloudflare Analytics (zero setup):**
- Dashboard → Analytics → Web Analytics
- Shows visitors, countries, devices, page views
- No scripts needed — works at DNS level

**In the website:**
- The page auto-fetches download counts from GitHub API
- Displayed live on the homepage

---

## How Users Get Updates

### Automatic in-app notification
When you push a new tag (v1.1.0, v1.2.0 etc.), the app checks GitHub API
on launch and shows a banner: "G2-Link v1.1 available — tap to update"

### Mesh propagation (works offline)
You can inject an update notice packet into any mesh — it spreads
device-to-device to all users within range over hours/days.

### Website update bar
The website detects new releases and shows a banner to returning visitors.

---

## Release Checklist (each update)

```
□ Update versionCode and versionName in app/build.gradle.kts
□ Test on physical device
□ git add . && git commit -m "Release v1.x.x"
□ git tag v1.x.x
□ git push && git push --tags
□ GitHub Actions builds + releases automatically (~3 min)
□ All users notified via in-app update check
```

---

## Cost Summary

| Item | Cost |
|---|---|
| GitHub (code + APK hosting + CI/CD) | Free forever |
| GitHub Pages (website hosting) | Free forever |
| Cloudflare (CDN + analytics + SSL) | Free forever |
| Google Play Developer Account (optional) | $25 one-time |
| Domain (g2link.app) | ~$12/year |
| **Total year 1** | **$12–37** |
| **Every year after** | **$12** |
| **At 10 million users** | **$12/year** |

---

*G2-Link. Built for the moment when everything else fails.*
