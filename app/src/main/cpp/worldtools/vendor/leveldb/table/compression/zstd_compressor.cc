#include "port/port_config.h"

#if HAVE_ZSTD

#include <zstd.h>

#include "table/compression/zstd_compressor.h"

void leveldb::ZstdCompressor::compress(const char* input, size_t length,
                                           ::std::string& output) const {
  // extend the buffer to the worst case
  auto originalSize = output.size();
  auto capacity = ZSTD_compressBound(length);
  output.resize(originalSize + capacity);

  // and then compress into it
  auto sz = ZSTD_compress((void*)(output.data() + originalSize), capacity,
                          input, length, compressionLevel);

  assert(!ZSTD_isError(sz));

  output.resize(sz + originalSize);
}

bool leveldb::ZstdCompressor::decompress(const char* input, size_t length,
                                         ::std::string& output) const {
  auto decompressedSize = ZSTD_getFrameContentSize(input, length);
  if (ZSTD_isError(decompressedSize)) {
    return false;
  }
  output.resize(decompressedSize);

  auto sz = ZSTD_decompress((void*)output.data(), output.size(), input, length);
  if (ZSTD_isError(sz)) {
    return false;
  }
  output.resize(sz);
  return true;
}

#endif
