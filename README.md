# MindBody — 身心健康管理 App

護理健康程式素養 APP 創意競賽 2026 作品。

Android（Java + Material 3）+ Node.js 後端 + 網頁 Demo。

## 功能

- 每日打卡（情緒分數 + 症狀勾選 + 心情日記）
- AI 分析（NVIDIA NIM + MiniMax M2.7）
- 統計圖表（心情趨勢 / 症狀統計 / 關聯分析）
- 連續打卡記錄
- 問卷評估

## 技術棧

- Android: Java、Material 3、Retrofit、MPAndroidChart、Room
- 後端: Node.js + Express + MySQL
- AI: NVIDIA NIM API + MiniMax M2.7（OpenAI 相容）

## 建置

GitHub Actions 自動建 APK，推送後到 Actions 頁面下載 artifact。

也可本地建置：

```bash
./gradlew assembleDebug
# 產出: app/build/outputs/apk/debug/app-debug.apk
```

## API

後端部署在 `https://anzufish.org/mindbody-api/`，端點：

- `POST /api/auth/register` `/login` — 註冊登入
- `POST /api/checkin` — 送出打卡
- `GET /api/checkin/streak` `/history` — 連續紀錄
- `POST /api/questionnaire` — 送出問卷
- `GET /api/ai/latest` `/analysis` — AI 分析
- `GET /api/stats/mood` `/symptoms` `/correlation` — 統計

## 網頁 Demo

`https://anzufish.org/mindbody/`
