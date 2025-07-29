# GuessTheUtils Fabric 1.21 → Forge 1.8.9 Migration Plan

## Project Overview

**Source Project**: GuessTheUtils - Fabric 1.21 Client Mod  
**Target**: Migration to Forge 1.8.9  

### Current Project Analysis

GuessTheUtils is a client-side utility mod designed specifically for Hypixel's "Guess The Build" game mode, with the following main features:

1. **GameTracker** - Game state tracking and analysis
2. **CustomScoreboard** - Custom scoreboard display  
3. **DisallowedItemHider** - Hide disallowed items in the game
4. **NameAutocomplete** - Player name auto-completion
5. **ChatCooldownTimer** - Chat cooldown timer
6. **BuilderNotification** - Builder status notifications
7. **ShortcutReminder** - Keyboard shortcut reminders
8. **Event System** - Complete game event handling mechanism

## Major Challenges

### 1. Framework Differences
- **Fabric** → **Forge**: Completely different mod loader and API systems
- **Modern Mixins** → **Traditional Hooks**: Forge 1.8.9 lacks modern Mixin support
- **Event System**: Fabric events → Forge events, completely different APIs

### 2. Massive Version Gap (1.8.9 → 1.21)
- **Text System**: `String` → `Text`/`MutableText` components
- **Rendering System**: Immediate mode → Modern rendering pipeline
- **Item System**: Numeric IDs → Namespaced resource identifiers
- **Network System**: Basic packet handling → Modern network API
- **GUI System**: Traditional GUI → Modern interface system

### 3. Incompatible Dependencies
- **YACL** (Config library): No 1.8.9 version exists
- **ModMenu**: No 1.8.9 version exists  
- **Fabric API**: Need to find Forge equivalents

## Detailed Migration Tasks

### Stage 1: Project Foundation Setup (Estimated: 2-3 days)

#### 1.1 Build System Migration
- [ ] Download 1.8.9 Forge MDK
- [ ] Create new Forge 1.8.9 project structure at `gtb_forge` directory
- [ ] Replace `build.gradle.kts` with Forge MDK configuration
- [ ] Set up ForgeGradle 1.2 (1.8.9 compatible version)
- [ ] Configure Java 8 compatibility (1.8.9 requirement)
- [ ] Remove Fabric-related configurations

**Key File Changes:**
```diff
- build.gradle.kts (Fabric configuration)
+ build.gradle (Forge configuration)
- fabric.mod.json
+ mcmod.info
- guesstheutils.mixins.json
+ (Remove, use traditional hooks)
```

#### 1.2 Project Structure Adjustment  
- [ ] Reorganize package structure to fit Forge conventions
- [ ] Create main Mod class extending `Mod`
- [ ] Set up `@Mod` annotation and module ID
- [ ] Create proxy classes (ClientProxy)

### Stage 2: Core System Rewrite (Estimated: 1-2 weeks)

#### 2.1 Main Mod Class Migration
**Source File**: `GuessTheUtils.java` (ClientModInitializer)  
**Target**: Forge Mod main class

- [ ] Replace `ClientModInitializer` → `@Mod` class
- [ ] Rewrite initialization logic `preInit`, `init`, `postInit`
- [ ] Migrate client event registration to Forge event bus
- [ ] Remove Fabric-specific callback registrations

#### 2.2 Event System Refactoring
**Source File**: `GTBEvents.java` (699 lines of complex event system)

**Challenge**: 1.8.9 lacks modern event APIs, requires manual implementation
- [ ] Retain event record classes (Records → Regular classes, Java 8 compatible)
- [ ] Implement custom event publish/subscribe system
- [ ] Rewrite event handling logic to adapt to 1.8.9 API

#### 2.3 Mixin System Replacement
**Current Mixin Files**:
- `InGameHudMixin` - HUD modifications
- `ClientPlayNetworkHandlerMixin` - Network handling
- `ItemGroup_EntriesImplMixin` - Item group modifications
- `ClientCommandSourceMixin` - Command handling

**Migration Strategy**:
- [ ] Use ASM/bytecode injection to replace mixins
- [ ] Utilize Forge hook events (TickEvent, RenderEvent, etc.)
- [ ] Manual reflection access to private fields/methods
- [ ] Implement compatibility layer to simulate mixin behavior

### Stage 3: Core Module Rewrite (Estimated: 2-3 weeks)

#### 3.1 GameTracker Module (483 lines)
- [ ] Migrate game state detection logic
- [ ] Rewrite scoreboard analysis (1.8.9 scoreboard API differs significantly)
- [ ] Adapt player list processing
- [ ] Re-implement event triggering mechanism

#### 3.2 CustomScoreboard Module (442 lines)  
- [ ] Rewrite rendering system (1.8.9 immediate mode rendering)
- [ ] Adapt font and text rendering
- [ ] Re-implement texture loading and rendering
- [ ] Adapt GUI coordinate system differences

#### 3.3 DisallowedItemHider Module (583 lines)
**Main Challenge**: Item system completely different
- [ ] Migrate item list from namespaced IDs to numeric IDs
- [ ] Rewrite creative mode tab modification logic
- [ ] Adapt to 1.8.9 item registration system
- [ ] Re-implement item filtering mechanism

#### 3.4 Other Modules
- [ ] **NameAutocomplete**: Rewrite command completion system
- [ ] **ChatCooldownTimer**: Adapt to 1.8.9 chat API
- [ ] **BuilderNotification**: Rewrite notification system
- [ ] **ShortcutReminder**: Adapt key binding API

### Stage 4: Configuration and Interface System (Estimated: 1 week)

#### 4.1 Configuration System Rebuild
**Problem**: YACL doesn't exist for 1.8.9
- [ ] Implement simple configuration file system (JSON/Properties)
- [ ] Create basic configuration GUI (if needed)
- [ ] Remove ModMenu integration (doesn't exist for 1.8.9)

#### 4.2 GUI Adaptation
- [ ] Rewrite all GUIs using 1.8.9 API
- [ ] Adapt rendering method calls
- [ ] Fix coordinate and scaling system differences

### Stage 5: Resources and Localization (Estimated: 2-3 days)

#### 5.1 Resource File Adaptation  
- [ ] Migrate language file format (modern JSON → 1.8.9 lang)
- [ ] Check texture compatibility
- [ ] Adapt resource pack structure differences

#### 5.2 Localization System
- [ ] Rewrite text component system
- [ ] Adapt color and formatting codes
- [ ] Remove modern text API calls

### Stage 6: Testing and Debugging (Estimated: 1 week)

#### 6.1 Functional Testing
- [ ] Individual module functionality verification
- [ ] Testing on Hypixel server
- [ ] Performance testing and optimization

#### 6.2 Compatibility Testing
- [ ] Compatibility with other 1.8.9 mods
- [ ] Different operating system testing
- [ ] Memory usage optimization

## Technical Details and Considerations

### Key API Mappings

| Fabric 1.21 | Forge 1.8.9 |
|--------------|--------------|
| `ClientModInitializer` | `@Mod` + Proxy |
| `ClientTickEvents` | `TickEvent.ClientTickEvent` |
| `ClientReceiveMessageEvents` | `ClientChatReceivedEvent` |
| `HudRenderCallback` | `RenderGameOverlayEvent` |
| `Text`/`MutableText` | `String` + `ChatComponentText` |
| Mixin injection | ASM/reflection/Forge hooks |

### Unavailable Features
The following features cannot be implemented in 1.8.9 or require significant simplification:
- [ ] Modern text component system
- [ ] Advanced rendering effects  
- [ ] Certain modern GUI elements
- [ ] Complex network packet handling

### Development Environment Requirements
- **Java 8** (1.8.9 requirement)
- **ForgeGradle 1.2**
- **MinecraftForge 1.8.9-11.15.1.2318** (recommended version)
- **MCP 9.19** (code obfuscation mappings)

## Risk Assessment

### High Risk Areas
1. **Mixin system replacement** - May result in feature loss
2. **Rendering system differences** - Requires complete rewrite
3. **Network API changes** - Packet processing logic needs refactoring
4. **Event system compatibility** - Custom implementation complexity is high

### Medium Risk
1. **Item ID migration** - Large workload but manageable
2. **Configuration system rebuild** - Simplified functionality but achievable
3. **Text system adaptation** - Requires careful encoding handling

### Recommendations
1. **Phased development**: Migrate and test modules one by one
2. **Feature simplification**: Some advanced features may need to be removed
3. **Alternative approaches**: Consider migrating to 1.12.2 as an intermediate version
4. **Community support**: Find developers experienced with 1.8.9 Forge development

## Estimated Timeline

**Total**: 6-8 weeks (full-time development)

- Stage 1: 3 days
- Stage 2: 1-2 weeks  
- Stage 3: 2-3 weeks
- Stage 4: 1 week
- Stage 5: 3 days
- Stage 6: 1 week

**Note**: This is an extremely complex migration project. Consider the following alternatives:
1. Migration to 1.12.2 Forge (more modern APIs)
2. Migration to 1.16.5 Forge (better compatibility)
3. Redesign simplified version specifically for 1.8.9

---

*This document will be continuously updated as migration progresses*