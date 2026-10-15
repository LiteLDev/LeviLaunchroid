# High-Performance Minecraft NBT Library for Morden C++

[![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/GlacieTeam/NBT/build.yml)](https://github.com/GlacieTeam/NBT/actions)
[![GitHub License](https://img.shields.io/github/license/GlacieTeam/NBT)](https://www.mozilla.org/en-US/MPL/2.0/)
[![C++23](https://img.shields.io/badge/C++-23-blue?logo=C%2B%2B&logoColor=41a3ed)](https://en.cppreference.com/w/cpp/compiler_support.html) 

A blazing-fast C++ library for parsing, generating, and manipulating Minecraft Named Binary Tag (NBT) data with benchmarked speeds of 200 MB/s on modern CPUs.

## Supported NBT format 📖

| NBT Format                        | Minecraft Edition      | Support Status     |
| --------------------------------- | ---------------------- | ------------------ |
| Little Endian Binary              | Bedrock Edition        | :white_check_mark: |
| Little Endian Binary with Header  | Bedrock Edition        | :white_check_mark: |
| Big Endian Binary                 | Java Edition           | :white_check_mark: |
| Big Endian Binary with Header     | Java Edition           | :white_check_mark: |
| Bedrock Network (VarInt Encoding) | Bedrock Edition        | :white_check_mark: |
| Formatted String (SNBT)           | Bedrock & Java Edition | :white_check_mark: |

## Features ⚡
- **Ultra-fast parsing** - Achieves 200 MB/s processing speed on modern CPUs
- **Complete NBT support** - All NBT types (Java and Bedrock)
- **Full SNBT support** - Full SNBT support, include new SNBT format since Java Edition 1.21.5
- **Zero-copy design** - Minimal memory allocation during parsing
- **Streaming interface** - Process large NBT files with constant memory footprint
- **Cross-platform** - Windows, Linux, macOS support
- **Endian-aware** - Automatic little/big-endian conversion
- **Zlib/Gzip support** - Built-in compression handling
- **Simple API** - Intuitive interface inspired by standard libraries
- **CFFI** - Pure C API support.

## Quick Start 🚀
```C++
using namespace nbt::literals::integral;

// Parsing NBT from file (Automatically detect NBT format and decompress)
auto nbt1 = nbt::io::parseFromFile("./level.dat");
// You can also specify the NBT format
// auto nbtRead = nbt::io::parseFromFile("./level.dat", NbtFileFormat::LittleEndian);

// Craate a CompoundTag in memory
nbt::CompoundTag nbt2 = {
    {"Byte",      64_b                                   },
    {"Short",     16384_s                                },
    {"Int",       12345678                               },
    {"Long",      5678900123456_l                        },
    {"Float",     3.14159f                               },
    {"Double",    1.23456789                             },
    {"String",    "homo string"                          },
    {"ByteArray", ByteArrayTag({2, 3, 4, 5, 6, 7, 8, 9}) },
    {"List",      ListTag()                              },
    {"Compound",  CompoundTag()                          },
    {"IntArray",  IntArrayTag({212345, 3765432, 4234567})},
    {"LongArray", LongArrayTag({45, 432, 244, 524})      },
};

nbt2.toBinaryNbt();   // to binary nbt format
nbt2.toNetworkNbt();  // to network nbt format

// Save NBT to file
nbt::io::saveToFile(nbt2, NbtFileFormat::LittleEndian);

```

## Install and Using 🔧
### Requirements
- C++23 compatible compiler (GCC 13+, Clang 17+, MSVC 2022+)
- Xmake 3.0.0+

### Build Scripts
```xmake
add_repositories("groupmountain-repo https://github.com/GroupMountain/xmake-repo.git")

add_requires("nbt")
```

## Building from Source 🔧
- Build with Xmake
```bash
xmake --all
```
- If you want to use Cmake build system, you can generate CmakeLists.txt
```bash
xmake project -k cmake
```

## Projects Using This Library 🏆
| Project          | Link                                         |
| ---------------- | -------------------------------------------- |
| ProtocolLib      | <https://github.com/GlacieTeam/ProtocolLib>  |
| AntiXray         | <https://github.com/GlacieTeam/AntiXray>     |
| RapidNBT         | <https://github.com/GlacieTeam/RapidNBT>     |

_If you would like to add your project here, please create a pull request._

## Contributing 🤝
Contributions are welcome! Please follow these steps:

1. Fork the repository and create your feature branch
2. Add tests for any new functionality
3. Submit a pull request with detailed description


## License 📄
This project is licensed under the **Mozilla Public License 2.0 (MPL-2.0)**.  

### Key Requirements:
- **Modifications to this project's files** must be released under MPL-2.0.  
- **Static/dynamic linking with closed-source projects** is allowed (no requirement to disclose your own code).  
- **Patent protection** is explicitly granted to all users.  

For the full license text, see [LICENSE](LICENSE) file or visit [MPL 2.0 Official Page](https://www.mozilla.org/en-US/MPL/2.0/).  

---

### Copyright © 2025 GlacieTeam. All rights reserved.