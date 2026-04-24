# TILT ARENA (AMP.G08) - Project Guide

Tài liệu onboarding nhanh cho codebase hiện tại, tập trung vào kiến trúc thực tế đang chạy: Auth, Room/Lobby, Matchmaking, Gameplay realtime, Result và Leaderboard.

## 1) Thông tin chung
- Tên dự án: `TILT ARENA`
- Package chính: `com.example.ampg08`
- Ngôn ngữ: Java
- Kiến trúc tổng quát: Activity + Service Manager (Firebase) + Custom Game Engine (`GameView`)
- Backend: Firebase Authentication, Cloud Firestore, Firebase Storage

## 2) Entry Point va Navigation
- App khởi động từ `app/src/main/java/com/example/ampg08/SplashActivity.java` (khai báo trong `app/src/main/AndroidManifest.xml`).
- `SplashActivity` kiểm tra trạng thái đăng nhập bằng `FirebaseAuthManager.isLoggedIn()`:
  - Đã đăng nhập -> `HomeActivity`
  - Chưa đăng nhập -> `LoginActivity`
- Tất cả màn hình kế thừa `BaseActivity` để dùng cấu hình full-screen/edge-to-edge thống nhất.

## 3) Package va File Responsibilities

### 3.1 `com.example.ampg08` (UI/Flow)
- `BaseActivity.java`: base class cho fullscreen + ẩn system bars.
- `SplashActivity.java`: splash animation + route vào Auth/Home.
- `LoginActivity.java`: đăng nhập email/password + Google Sign-In.
- `RegisterActivity.java`: đăng ký tài khoản và tạo `users/{uid}`.
- `HomeActivity.java`: menu trung tâm sau đăng nhập.
- `PlaySetupActivity.java`: chọn mode/map/player/time trước khi tạo trận.
- `CreateRoomActivity.java`: host tạo room online.
- `JoinRoomActivity.java`: nhập room code và join phòng.
- `LobbyActivity.java`: trạng thái ready/start trước khi vào game.
- `MatchmakingActivity.java`: quick match 2 người qua matchmaking pool.
- `GameActivity.java`: điều phối sensor + `GameView`, chuyển Result khi finish.
- `ResultActivity.java`: hiển thị kết quả và ghi match/leaderboard (host writer).
- `LeaderboardActivity.java`: top leaderboard toàn cục.
- `ScoreActivity.java`: leaderboard theo tab (all/friends/mine).
- `ProfileActivity.java`: cập nhật displayName/avatar.
- `SettingsActivity.java`: sensitivity/vibration settings.
- `MainActivity.java`: menu cũ (legacy, không phải launch activity).

### 3.2 `com.example.ampg08.firebase`
- `FirebaseAuthManager.java`: wrapper cho Firebase Auth + Google auth.
- `FirestoreManager.java`: data access layer chính cho Firestore.
- `RoomService.java`: wrapper mỏng cho sync vị trí/finish.
- `LeaderboardService.java`: wrapper mỏng cho leaderboard APIs.

### 3.3 `com.example.ampg08.view`
- `GameView.java`: game loop 60 FPS, physics, render, sync remote player, skill, finish detection.
- `ParticleView.java`: hiệu ứng nền particle cho UI.

### 3.4 `com.example.ampg08.game`
- `MazeGenerator.java`: sinh maze theo seed (DFS).
- `CollisionDetector.java`: xử lý va chạm bóng-tường.
- `MazeRenderer.java`: vẽ maze/goal phong cách neon.
- `Ball.java`: vật lý bóng (tilt, boost, freeze).
- `SkillController.java`: cooldown/boost/freeze timings.

### 3.5 `com.example.ampg08.sync`
- `PositionSyncManager.java`: đẩy vị trí local player lên Firestore mỗi 100ms.

### 3.6 `com.example.ampg08.model`
- `User.java`: hồ sơ user và stats tổng.
- `Room.java`: metadata phòng (host, status, playerLimit, seed...).
- `PlayerState.java`: state realtime từng người chơi trong room.
- `Match.java`: lịch sử match đã chốt winner.
- `ScoreModel.java`, `MapModel.java`, `MatchResult.java`: model phục vụ UI/luồng phụ.

## 4) End-to-End Flows (Doc nhanh theo nghiệp vụ)

### 4.1 Auth Flow
1. `SplashActivity` route theo `isLoggedIn()`.
2. `LoginActivity`:
   - Email login: `loginWithEmail` -> `HomeActivity`.
   - Google login: lấy `idToken` -> `firebaseAuthWithGoogle` -> đảm bảo có `users/{uid}`.
3. `RegisterActivity`:
   - Tạo auth account -> tạo user profile document -> sign out -> quay lại login với email prefill.

### 4.2 Create/Join Room Flow
1. Host vào `CreateRoomActivity`, tạo `rooms/{roomId}` và `rooms/{roomId}/players/{hostUid}`.
2. Player vào `JoinRoomActivity`, kiểm tra room tồn tại/chưa full/chưa playing.
3. Cả hai vào `LobbyActivity`, listen room + player states.
4. Host bấm Start -> set room status `playing` -> tất cả navigate sang `GameActivity`.

### 4.3 Matchmaking Flow
1. `MatchmakingActivity` gọi join pool.
2. Nếu pool rỗng: user thành `waitingUid`.
3. Nếu đã có người chờ: transaction tạo room mới + trả `roomId` trong pool.
4. Mỗi client detect match, tự tạo `PlayerState` của mình, rồi vào `GameActivity`.

### 4.4 Game -> Result -> Leaderboard Flow
1. `GameActivity` setup `GameView` (offline hoặc online).
2. Trong online mode, `GameView`:
   - sync vị trí qua `updatePlayerPosition`,
   - nhận skill freeze,
   - khi về đích gọi `updatePlayerFinish`.
3. `ResultActivity` listen players trong room, sort theo `finishTime`.
4. Host duy nhất gọi `recordMatchAndLeaderboardOnce(roomId, winner, finishedPlayers)`.
5. Người chơi rời room từ `ResultActivity` (không rời sớm ở `GameActivity`).

## 5) Firestore Data Model (tham chiếu nhanh)

### 5.1 Collections
- `users/{uid}`: `uid`, `displayName`, `avatarUrl`, `totalWins`, `totalMatches`.
- `rooms/{roomId}`: `hostUid`, `mapSeed`, `playerLimit`, `status`, `players[]`, `createdAt`, ...
- `rooms/{roomId}/players/{uid}`: realtime position/ready/finish/freeze flags.
- `matches/{roomId}`: kết quả trận (1 room -> 1 match record).
- `leaderboard/{uid}`: tổng hợp `wins`, `totalMatches`, `displayName`, `lastRoomId`.
- `matchmaking/pool`: waiting + match assignment cho quick match.

### 5.2 Security va Ownership (từ `firestore.rules`)
- Chỉ host được đổi trạng thái room (`waiting` -> `playing` -> `ended`).
- Player chỉ tự join/leave chính mình và bị chặn vượt `playerLimit`.
- `matches/{roomId}` chỉ host room đó được tạo.
- `leaderboard/{uid}` chỉ được ghi khi gắn với `lastRoomId` hợp lệ và writer là host của room.

## 6) Class Interaction Map (UI trigger -> FirestoreManager)

Mục tiêu: nhìn nhanh Activity nào gọi method nào trong `FirestoreManager`, gọi khi nào, để trace bug cực nhanh.

| Activity | UI/Event Trigger | FirestoreManager calls | Muc dich |
|---|---|---|---|
| `LoginActivity` | Login Google thành công | `getUser(uid)`, `createUser(user)` | Đảm bảo có hồ sơ user lần đầu đăng nhập |
| `RegisterActivity` | Bấm `Register` | `createUser(user)` | Tạo profile `users/{uid}` sau khi tạo Auth account |
| `HomeActivity` | `onCreate/onResume` | `getUser(uid)` | Load tên + stats màn Home |
| `CreateRoomActivity` | Bấm `Create` | `createRoom(room)`, `setPlayerState(roomId, hostState)` | Tạo room host + state host |
| `JoinRoomActivity` | Bấm `Join` | `getRoomByCode(roomId)`, `joinRoom(roomId, uid)`, `setPlayerState(roomId, ps)` | Validate room và thêm player vào room |
| `LobbyActivity` | Toggle `Ready` | `setPlayerReady(roomId, uid, ready)` | Cập nhật trạng thái sẵn sàng |
| `LobbyActivity` | Mở lobby | `listenRoom(roomId)`, `listenPlayers(roomId)` | Đồng bộ room status và danh sách người chơi |
| `LobbyActivity` | Host bấm `Start` | `setRoomStatus(roomId, "playing")` | Bắt đầu trận cho cả phòng |
| `MatchmakingActivity` | Mở màn matchmaking | `joinMatchmakingPool(uid, name)` | Vào hàng chờ ghép trận |
| `MatchmakingActivity` | Đang chờ match | `listenMatchmakingPool(...)` | Nhận roomId khi match thành công |
| `MatchmakingActivity` | Bấm `Cancel` / `onDestroy` | `leaveMatchmakingPool(uid)` | Rời hàng chờ nếu chưa vào trận |
| `MatchmakingActivity` | Match thành công | `setPlayerState(roomId, ps)` | Tạo player doc trước khi vào game |
| `GameActivity` | Setup online | `getUser(uid)` | Cập nhật local display name từ user profile |
| `GameActivity` | `onDestroy` (thoát thật, không qua Result) | `leaveRoom(roomId, uid, ...)` | Cleanup player khỏi room |
| `ResultActivity` | Mở màn result online | `getRoomByCode(roomId)`, `listenPlayers(roomId)` | Xác định host writer + render ranking realtime |
| `ResultActivity` | Host ghi kết quả 1 lần | `recordMatchAndLeaderboardOnce(...)` | Lưu match idempotent + cập nhật leaderboard |
| `ResultActivity` | Delay 15s sau khi vào result | `setRoomStatus(roomId, "ended")` | Đóng trận |
| `ResultActivity` | Bấm `Home` / `onDestroy` | `leaveRoom(roomId, uid, ...)` | Rời room an toàn sau result |
| `LeaderboardActivity` | Load màn | `getLeaderboard(...)` | Lấy top leaderboard |
| `ScoreActivity` | Load tab All | `getLeaderboard(...)` | Lấy bảng xếp hạng tổng |
| `ScoreActivity` | Load tab Friends | `getFriendUids(uid)`, `getLeaderboardByUids(uids)` | Lấy ranking bạn bè |
| `ProfileActivity` | Mở màn profile | `getUser(uid)` | Load dữ liệu profile |
| `ProfileActivity` | Save tên | `updateDisplayName(uid, name, ...)` | Cập nhật tên ở users (và sync leaderboard best-effort) |
| `ProfileActivity` | Upload avatar thành công | `updateAvatarUrl(uid, url, ...)` | Cập nhật avatar URL |

Ghi chú: trong gameplay online, `GameView` gọi thêm các APIs realtime quan trọng:
- `listenPlayers(roomId)`
- `updatePlayerPosition(roomId, uid, x, y, mapX, mapY)`
- `updatePlayerFinish(roomId, uid, finishTime)`
- `sendFreezeCommand(roomId, targetUid)` / `clearFreezeFlag(roomId, uid)`

## 7) Reading Order de Onboarding Nhanh
1. `AndroidManifest.xml` -> hiểu launch flow.
2. `SplashActivity` -> `LoginActivity`/`RegisterActivity` -> `HomeActivity`.
3. `CreateRoomActivity` + `JoinRoomActivity` + `LobbyActivity`.
4. `GameActivity` -> `GameView` -> `PositionSyncManager`.
5. `ResultActivity` + `FirestoreManager.recordMatchAndLeaderboardOnce`.
6. `firestore.rules` để hiểu quyền ghi thực tế.

## 8) Build va Dependencies
- Build files: `build.gradle.kts`, `app/build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml`.
- SDK: `minSdk 24`, `targetSdk 36`, Java 11.
- Core libs: AndroidX, Material, Firebase (Auth/Firestore/Storage/Analytics), Google Sign-In, Glide.

---
Neu ban moi vao du an, hay bat dau bang muc 6 (Class Interaction Map), sau do trace thang den `FirestoreManager` va `firestore.rules` de hieu du lieu di theo huong nao va ai duoc phep ghi.
