# BinaryStream
**Part of [Bedrock Procotol Library](https://github.com/GlacieTeam/ProtocolLib).**  
A BinaryStream Library written in C++

## Usage
```C++
using namespace bstream;

// Read
std::string inputBuffer; // Buffer to read

ReadOnlyBinaryStream inputStream(inputBuffer);
uint8_t     byte   = inputStream.getByte();
int         varint = inputStream.getVarInt();
std::string str    = inputStream.getString();
// ... 


// Write
BinaryStream outputStream;
outputStream.writeFloat(3.1415f);
outputStream.writeUnsignedVarInt64(1234567890);
outputStream.writeSignedShort(16384);
//...

std::string outputBuffer = outputStream.getAndReleaseData();
std::string outputBuffer = outputStream.getAndReleaseData();
```

## Install
```xmake
add_repositories("groupmountain-repo https://github.com/GroupMountain/xmake-repo.git")

add_requires("binarystream")
```

## Build
- Build with Xmake
```bash
xmake --all
```
- If you want to use Cmake build system, you can generate CmakeLists.txt
```bash
xmake project -k cmake
```

## License
This project is licensed under the **Mozilla Public License 2.0 (MPL-2.0)**.  

### Key Requirements:
- **Modifications to this project's files** must be released under MPL-2.0.  
- **Static/dynamic linking with closed-source projects** is allowed (no requirement to disclose your own code).  
- **Patent protection** is explicitly granted to all users.  

For the full license text, see [LICENSE](LICENSE) file or visit [MPL 2.0 Official Page](https://www.mozilla.org/en-US/MPL/2.0/).  

---

### Copyright © 2025 GlacieTeam. All rights reserved.