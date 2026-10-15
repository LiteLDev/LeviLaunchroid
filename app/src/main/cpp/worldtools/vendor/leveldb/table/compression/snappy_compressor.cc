#include "port/port_config.h"

#if HAVE_SNAPPY

#include <snappy.h>

#include "table/compression/snappy_compressor.h"

namespace leveldb {
void SnappyCompressor::compress(const char* input, size_t length,
                                    ::std::string& output) const {
  output.resize(snappy::MaxCompressedLength(length));
  size_t outlen;
  snappy::RawCompress(input, length, &output[0], &outlen);
  output.resize(outlen);
}

bool SnappyCompressor::decompress(const char* input, size_t length,
                                  std::string& output) const {
  size_t ulength;
  if (!snappy::GetUncompressedLength(input, length, &ulength))
    return false;  // could not decompress

  output.resize(ulength);

  return snappy::RawUncompress(input, length, (char*)output.data());
}

}  // namespace leveldb

#endif