#ifndef STORAGE_LEVELDB_INCLUDE_DB_ZSTD_COMPRESSOR_H_
#define STORAGE_LEVELDB_INCLUDE_DB_ZSTD_COMPRESSOR_H_

#include <cassert>

#include "table/compression/compressor.h"

namespace leveldb {

class ZstdCompressor : public Compressor {
 public:
  const int compressionLevel;

  virtual ~ZstdCompressor() {}

  ZstdCompressor(int compressionLevel = -1)
      : compressionLevel(compressionLevel) {
    assert(compressionLevel >= -1 && compressionLevel <= 9);
  }

  virtual void compress(const char* input, size_t length,
                            ::std::string& output) const override;

  virtual bool decompress(const char* input, size_t length,
                          ::std::string& output) const override;

 private:
};
}  // namespace leveldb

#endif