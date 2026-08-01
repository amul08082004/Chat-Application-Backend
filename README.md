# Chat App Backend (Spring Boot)

1-to-1 chat, group chat, online/offline presence, and sent → delivered → seen
message receipts, secured with JWT auth. Runs out of the box with H2 (no DB setup needed).

## Run it

**1. Have MySQL running.** Easiest way if you don't already have it:

```bash
docker run --name chatapp-mysql -e MYSQL_ROOT_PASSWORD=root -p 3306:3306 -d mysql:8
```

The app creates the `chatapp` database and all tables automatically on first boot
(`createDatabaseIfNotExist=true` + `ddl-auto: update`) — no manual schema/migration step needed.

**2. Set your credentials.** Defaults in `application.yml` are `root` / `root` on
`localhost:3306`. Either edit that file directly, or override via env vars:

```bash
export SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/chatapp?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
export SPRING_DATASOURCE_USERNAME=root
export SPRING_DATASOURCE_PASSWORD=root
```

**3. Run:**

```bash
mvn spring-boot:run
```

App starts on `http://localhost:8080`.

## How the pieces fit together

- **Auth**: `POST /api/auth/register` and `/api/auth/login` return a JWT. Send it as
  `Authorization: Bearer <token>` on every REST call, and as a STOMP header on CONNECT.
- **Rooms**: a `ChatRoom` is either `ONE_TO_ONE` (exactly 2 members) or `GROUP` (named, N members).
  1-1 rooms are get-or-created idempotently, so the client can just call the endpoint
  every time it opens a DM without worrying about duplicates.
- **Real-time transport**: STOMP over WebSocket (with SockJS fallback) at `/ws`.
- **Presence**: driven by WebSocket session connect/disconnect. A user is "online" as
  long as they have a live socket; disconnect stamps `lastSeen` and broadcasts offline.
- **Receipts (sent/delivered/seen)**: every message gets one `MessageReceipt` row per
  recipient. This is what makes it work correctly for *group* chats too — the sender
  can see the message is only "delivered" until every member has seen it, exactly like
  WhatsApp double-ticks. The REST/DTO layer also exposes an aggregate status (SENT /
  DELIVERED / SEEN) for a simple single-ticks UI if you don't need per-user breakdown.

## REST API

All endpoints except `/api/auth/**` require `Authorization: Bearer <jwt>`.

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/auth/register` | `{username, email, password, fullName}` → `{token, user}` |
| POST | `/api/auth/login` | `{username, password}` → `{token, user}` |
| GET  | `/api/users` | list all other users + online status |
| GET  | `/api/users/me` | current user |
| GET  | `/api/rooms` | rooms you belong to (1-1 and group) |
| POST | `/api/rooms/one-to-one/{otherUserId}` | get-or-create a DM |
| POST | `/api/rooms/group` | `{name, memberIds: [...]}` create a group |
| POST | `/api/rooms/{roomId}/members/{userId}` | add someone to a group |
| GET  | `/api/rooms/{roomId}` | room details |
| GET  | `/api/rooms/{roomId}/messages?page=0&size=30` | paginated history, newest first (also marks messages seen on page 0) |

## WebSocket (STOMP) API

Connect: `new SockJS('http://localhost:8080/ws')`, then STOMP CONNECT with header
`Authorization: Bearer <jwt>`.

**Subscribe:**
- `/topic/room.{roomId}` — new messages in that room
- `/topic/room.{roomId}.receipts` — delivery/seen updates for that room
- `/topic/room.{roomId}.presence` — online/offline changes for members of that room
- `/topic/presence` — global presence feed (handy for a contacts list)
- `/user/queue/errors` — errors addressed only to you (e.g. "not a member of this room")

**Send:**
- `/app/chat.send` → `{roomId, content}`
- `/app/chat.delivered` → `{messageId}` (client acks a message it just received live)
- `/app/chat.seen` → `{roomId}` (mark everything in an opened chat as seen) or `{messageId}` for one message


## Notes / production hardening ideas

- Move the JWT secret and DB credentials to environment variables, don't ship them in `application.yml`.
- Add refresh tokens if you want shorter-lived access tokens.
- Add pagination/rate limiting on `/api/users` for large user bases.
- Consider Redis pub/sub (`spring-boot-starter-data-redis` + a Redis-backed STOMP relay)
  once you need more than one app instance, since the in-memory broker used here only
  works for a single node.
- File/image attachments: extend `Message` with a `type` + `attachmentUrl`, wire up
  an upload endpoint (S3/local disk) — not included here to keep scope focused on chat mechanics.
