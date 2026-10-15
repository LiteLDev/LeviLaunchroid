#include "table/compression/compressor_factory.h"

#include "port/port_config.h"

#if HAVE_SNAPPY
#include "table/compression/snappy_compressor.h"
#endif

#if HAVE_ZLIB
#include "table/compression/zlib_compressor.h"
#endif

#if HAVE_ZSTD
#include "table/compression/zstd_compressor.h"
#endif

namespace leveldb {

Compressor* CompressorFactory::GetCompressor(CompressionType type) {
  switch (type) {
#if HAVE_SNAPPY
    case kSnappyCompression: {
      static SnappyCompressor snappyCompressor;
      return &snappyCompressor;
    }
#endif
#if HAVE_ZLIB
    case kZlibCompression: {
      static ZlibCompressor zlibCompressor;
      return &zlibCompressor;
    }
    case kZlibRawCompression: {
      static ZlibCompressorRaw zlibCompressorRaw;
      return &zlibCompressorRaw;
    }
#endif
#if HAVE_ZSTD
    case kZstdCompression: {
      static ZstdCompressor zstdCompressor;
      return &zstdCompressor;
    }
#endif
    default:
      return nullptr;
  }
}

std::string CompressorFactory::GetCompressorName(CompressionType type) {
  switch (type) {
    case kNoCompression:
      return "none";
    case kSnappyCompression:
      return "snappy";
    case kZlibCompression:
      return "zlib";
    case kZstdCompression:
      return "zstd";
    case kZlibRawCompression:
      return "zlib_raw";
    default:
      return "unknown";
  }
}
}  // namespace leveldb
