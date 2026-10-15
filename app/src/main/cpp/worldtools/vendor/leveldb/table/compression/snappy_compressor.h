#ifndef STORAGE_LEVELDB_INCLUDE_DB_SNAPPY_COMPRESSOR_H_
#define STORAGE_LEVELDB_INCLUDE_DB_SNAPPY_COMPRESSOR_H_

#include "table/compression/compressor.h"

namespace leveldb {
class SnappyCompressor : public Compressor {
 public:
  virtual ~SnappyCompressor() {}

  virtual void compress(const char* input, size_t length,
                            ::std::string& output) const override;

  virtual bool decompress(const char* input, size_t length,
                          ::std::string& output) const override;
};
}  // namespace leveldb

#endif