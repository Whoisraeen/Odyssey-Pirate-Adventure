# The Odyssey 🏴‍☠️⚓

**Navigate the Boundless Azure - Build Your Fleet, Command Your Destiny, Shape the Seven Seas**

A revolutionary voxel maritime adventure game built with cutting-edge C++ technology, featuring advanced Vulkan rendering, realistic physics, and an infinite procedurally generated ocean world.

## ✨ Features

### 🌊 Revolutionary Ocean World
- **Infinite Procedural Generation**: Endless ocean with dynamic islands, biomes, and weather
- **Living Ecosystem**: Dynamic tides, ocean currents, marine life, and seasonal changes
- **Advanced Water Physics**: Realistic wave simulation, buoyancy, and fluid dynamics
- **Multiple Biomes**: Tropical atolls, volcanic spires, dense jungles, mangrove swamps, cursed isles, and arctic archipelagos

### 🚢 Advanced Ship Building & Physics
- **Modular Ship Construction**: Build ships block-by-block with realistic physics constraints
- **Realistic Naval Physics**: Authentic buoyancy, drag, lift, and stability calculations
- **Dynamic Weather**: Storms, fog, and wind affect sailing and combat
- **Fleet Management**: Command multiple ships with AI captains

### 🎨 Cutting-Edge Graphics
- **Vulkan Rendering**: Modern GPU-accelerated graphics with cross-platform support
- **Real-Time Ray Tracing**: Global illumination, reflections, and shadows (RTX/RDNA2+)
- **PBR Materials**: Physically-based rendering for realistic lighting
- **Advanced Water Shaders**: Caustics, foam, refraction, and volumetric effects

### ⚔️ Deep Gameplay Systems
- **Naval Combat**: Tactical ship-to-ship battles with physics-based damage
- **Trade & Economy**: Dynamic market system with supply and demand
- **Exploration**: Discover ancient ruins, treasure, and mysterious locations
- **Survival Elements**: Manage crew, supplies, and ship maintenance

## 🛠️ Technical Architecture

### Core Engine
- **Language**: Modern C++20 with CMake build system
- **Graphics**: Vulkan API with MoltenVK for macOS support
- **Physics**: Bullet Physics with custom ship and water simulation
- **Threading**: Modern multi-threaded architecture for performance
- **Memory**: Custom allocators and RAII-based resource management

### Key Systems
- **Voxel Engine**: Chunk-based world with efficient storage and streaming
- **Procedural Generation**: Advanced noise-based terrain and biome generation
- **Entity-Component System**: entt-based ECS for flexible game objects
- **Networking**: Client-server architecture for multiplayer support

## 🚀 Quick Start

### Prerequisites
- **CMake 3.20+**
- **Vulkan SDK** (latest version)
- **Git**
- **C++20 compatible compiler** (GCC 10+, Clang 11+, MSVC 2019+)

### Platform-Specific Requirements

#### Windows
- Visual Studio 2019/2022 or MinGW-w64
- Windows 10/11

#### macOS
- Xcode 12+ or command line tools
- macOS 10.15+ (Catalina)
- MoltenVK (included with Vulkan SDK)

#### Linux
- GCC 10+ or Clang 11+
- Development packages: `libxrandr-dev libxinerama-dev libxcursor-dev libxi-dev`

### Building from Source

1. **Clone the repository**:
   ```bash
   git clone https://github.com/your-username/the-odyssey.git
   cd the-odyssey
   ```

2. **Run the build script**:
   ```bash
   # Quick release build
   ./build.sh
   
   # Debug build with tests
   ./build.sh -d -t
   
   # Clean rebuild
   ./build.sh -c
   ```

3. **Alternative manual build**:
   ```bash
   # Setup vcpkg dependencies
   git clone https://github.com/Microsoft/vcpkg.git
   ./vcpkg/bootstrap-vcpkg.sh  # or .bat on Windows
   
   # Configure and build
   cmake -B build -DCMAKE_TOOLCHAIN_FILE=vcpkg/scripts/buildsystems/vcpkg.cmake
   cmake --build build --parallel
   ```

4. **Run the game**:
   ```bash
   ./build/src/TheOdyssey
   ```

## 🎮 Gameplay

### Getting Started
1. **Create Your Character**: Start as a castaway on a small island
2. **Build Your First Raft**: Gather driftwood and basic materials
3. **Explore the Seas**: Discover new islands and biomes
4. **Upgrade Your Fleet**: Progress from rafts to mighty galleons
5. **Master the Ocean**: Become a legendary pirate captain

### Controls
- **WASD**: Movement/Ship navigation
- **Mouse**: Camera/Free look
- **E**: Interact
- **Tab**: Inventory
- **M**: Map
- **B**: Ship builder
- **I**: Journal/Quests

### Ship Building
- Use the modular ship builder to create custom vessels
- Balance speed, cargo capacity, and combat effectiveness
- Upgrade with better materials and specialized components
- Manage crew assignments and ship maintenance

## 🌍 World Generation

### Biomes
- **Ocean & Deep Ocean**: Vast expanses of deep blue water
- **Tropical Atolls**: Paradise islands with palm trees and coral reefs
- **Volcanic Spires**: Dramatic peaks rich in rare materials
- **Dense Jungles**: Lush islands with hidden temples and treasures
- **Mangrove Swamps**: Mysterious wetlands perfect for hiding
- **Whispering Isles**: Cursed lands shrouded in eternal fog
- **Arctic Archipelagos**: Frozen northern territories with unique challenges

### Dynamic Systems
- **Tidal Mechanics**: Real-time tides reveal hidden areas and affect navigation
- **Weather Patterns**: Seasonal changes and storm systems
- **Ecosystem Simulation**: Fish migrations, predator-prey relationships
- **Geological Activity**: Volcanic eruptions can create or destroy islands

## ⚙️ Configuration

### Graphics Settings
Edit `settings.json` to customize:
- Resolution and display mode
- Ray tracing quality (if supported)
- Water and shadow quality
- Anti-aliasing and post-processing effects

### Gameplay Settings
- Difficulty and survival mechanics
- Auto-save intervals
- Control bindings
- Audio levels

## 🔧 Development

### Building for Development
```bash
# Debug build with all features
./build.sh -d -t --raytracing

# Disable ray tracing for older hardware
./build.sh -d --no-raytracing
```

### Project Structure
```
src/
├── Core/           # Engine foundation (math, memory, platform)
├── Rendering/      # Vulkan graphics and shaders
├── World/          # Voxel engine and world generation
├── Physics/        # Bullet Physics integration
├── Game/           # Gameplay systems and logic
└── Networking/     # Multiplayer support
```

### Contributing
1. Fork the repository
2. Create a feature branch
3. Follow the coding standards (see CONTRIBUTING.md)
4. Submit a pull request

## 📋 System Requirements

### Minimum Requirements
- **OS**: Windows 10, macOS 10.15, or Linux with kernel 4.15+
- **CPU**: Intel i5-8400 / AMD Ryzen 5 2600
- **Memory**: 8 GB RAM
- **Graphics**: GTX 1060 / RX 580 with Vulkan support
- **Storage**: 4 GB available space

### Recommended Requirements
- **OS**: Windows 11, macOS 12+, or recent Linux distribution
- **CPU**: Intel i7-10700K / AMD Ryzen 7 3700X
- **Memory**: 16 GB RAM
- **Graphics**: RTX 3070 / RX 6700 XT (for ray tracing)
- **Storage**: 8 GB available space (SSD recommended)

## 🐛 Troubleshooting

### Common Issues

#### "Vulkan not supported" error
- Install the latest graphics drivers
- Verify Vulkan SDK installation
- Check hardware compatibility

#### Build failures
- Ensure all dependencies are installed
- Try cleaning and rebuilding: `./build.sh -c`
- Check CMake and compiler versions

#### Performance issues
- Lower graphics settings in `settings.json`
- Disable ray tracing on older hardware
- Close other applications to free memory

### Getting Help
- Check the [Wiki](wiki/) for detailed guides
- Report bugs on the [Issues](issues/) page
- Join our [Discord](https://discord.gg/odyssey) for community support

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **Vulkan**: Cross-platform graphics API
- **Bullet Physics**: Realistic physics simulation
- **GLM**: OpenGL Mathematics library
- **GLFW**: Multi-platform window and input handling
- **stb**: Single-file public domain libraries
- **Dear ImGui**: Immediate mode GUI
- **entt**: Fast and reliable entity-component system
- **spdlog**: Fast C++ logging library
- **vcpkg**: C/C++ package manager

## 🚢 Embark on Your Odyssey

The vast ocean awaits, captain. Build your ship, gather your crew, and set sail for adventure in the most ambitious maritime sandbox ever created.

**Fair winds and following seas!** ⛵

---

*The Odyssey - Where every voyage writes its own legend.*