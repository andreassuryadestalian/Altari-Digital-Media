# Church Multimedia System - Architecture & Design

## 1. System Architecture
The system follows a distributed modular architecture, currently running on a single Android device but designed for future separation:
- **Android Controller**: The operator's dashboard, managing the state, library, and user commands.
- **Presentation Server**: A central state manager that handles session synchronization (via Kotlin StateFlow locally, prepared for WebSockets).
- **Presentation Engine**: The renderer logic that translates `PresentationContent` (Lyrics, Video, Camera, PPT) into visual `PresentationFrame`s.
- **Display Renderer**: Handles output to Android's `Presentation` API for external displays (HDMI, Cast, DisplayPort).

## 2. Component Diagram
```
[Android Controller UI (Compose)]
       | (Commands: GO, NEXT, BLACK)
       v
[Presentation Server (State Manager)]
       | (StateFlow: CurrentSlide, Status)
       v
[Presentation Engine (Renderer Core)]
       | -> [LyricsRenderer]
       | -> [VideoEngine (Media3)]
       | -> [CameraEngine (CameraX)]
       | -> [PowerPointRenderer (Service)]
       v
[Display Renderer (External / Local UI)]
```

## 3. Data Flow
1. Operator selects a song from the **Library (Room DB)**.
2. Song is loaded into the **Preview State**.
3. Operator presses **GO**.
4. Controller sends `go(content)` command to **Presentation Server**.
5. Server updates `PresentationState` (`status = LYRICS`, `currentContent = Song`).
6. **Presentation Engine** collects the state, delegates rendering to `LyricsRenderer`.
7. **Display Renderer** renders the composable to the external display.

## 4. Network Flow (Remote Setup)
*Local mode (Current):* Flow/Coroutines between layers.
*Network mode (Future):*
- **REST API**: For stateless operations (e.g., `GET /library`, `POST /command/next`).
- **WebSocket**: For real-time state synchronization (`Event: SLIDE_CHANGED`).
- **Authentication**: Simple PIN-based pairing generating a session token.

## 5. Project Structure
```
app/src/main/java/com/example/
├── core/           # Common utilities, DI, constants
├── model/          # PresentationContent, DisplayProfile, Event models
├── database/       # Room entities, DAOs, Database setup
├── network/        # REST client, WebSocket client (Placeholders for remote)
├── ui/             # Theme, colors, typography, common composables
├── presentation/   # PresentationServer, PresentationEngine, StateFlows
└── features/       # Feature modules
    ├── dashboard/  # Main controller layout
    ├── lyrics/     # Lyrics import, parsing, rendering
    ├── playlist/   # Playlist management
    ├── media/      # Video/Image loading
    └── camera/     # CameraX source adapters
```

## 6. Database Schema (Room)
- **MediaEntity**: `id`, `type` (SONG, VIDEO, IMAGE), `title`, `content` (JSON or path), `createdAt`.
- **PlaylistEntity**: `id`, `name`, `date`.
- **PlaylistItemEntity**: `id`, `playlistId`, `mediaId`, `order`.

## 7. UI Wireframe
```
---------------------------------------------------------
| [Service] [Display: Connected] [Settings]             |
---------------------------------------------------------
| Library   | Preview              | Playlist           |
| - Songs   | [ Content View ]     | - Opening          |
| - Bible   |                      | - Song 1           |
| - PPT     |----------------------| - Sermon           |
| - Video   | Program              |                    |
| - Images  | [ Live Output ]      |                    |
---------------------------------------------------------
| [PREVIOUS]  [NEXT]  [ GO ]  [ BLACK ]  [ CLEAR ]      |
---------------------------------------------------------
```

## 8. Technology Stack
- **Language**: Kotlin
- **UI**: Jetpack Compose (Material 3, Dark Theme optimized)
- **Architecture**: MVVM, Clean Architecture, Coroutines, StateFlow
- **Database**: Room, DataStore
- **Media**: AndroidX Media3 (ExoPlayer) for Video, Coil for Images
- **Camera**: AndroidX CameraX (prepared for CameraSourceAdapter extension)
- **Dependency Injection**: ViewModel Factory / Manual DI (To ensure stability in initial AI build without Hilt plugin overhead)
- **External Display**: Android `Presentation` API (`android.app.Presentation`)

## 9. Technical Risks
1. **PowerPoint Rendering**: Android cannot natively render full `.ppt/.pptx` files accurately with animations.
   *Mitigation*: Implement a remote conversion service (e.g., LibreOffice headless server) that converts PPT to images/PDF, then render those images locally.
2. **Multi-Display limitations**: Not all Android devices support multiple physical outputs simultaneously.
   *Mitigation*: Fallback to a single external display (Screen Mirroring / Cast) or mock secondary displays in software.
3. **Low Latency Video/Camera**: Decoding video and camera streams on the same device as the UI can drop frames.
   *Mitigation*: Use hardware acceleration, separate Coroutine dispatchers for rendering.

## 10. Development Roadmap
- **Phase 1 (Current)**: Local Controller + Engine on single device, UI scaffolding, Room DB, Lyrics rendering, basic Media loading.
- **Phase 2**: External Display integration (Android Presentation API), Video/Camera Engine.
- **Phase 3**: Network Server implementation (Ktor REST/WebSocket).
- **Phase 4**: PPT Conversion Service API, Multi-device sync.
