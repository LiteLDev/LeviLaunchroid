#ifndef STORAGE_LEVELDB_DB_COMPRESSOR_FACTORY_H_
#define STORAGE_LEVELDB_DB_COMPRESSOR_FACTORY_H_

#include <string>

#include "leveldb/options.h"
#include "table/compression/compressor.h"

namespace leveldb {

class CompressorFactory {
 public:
  static Compressor* GetCompressor(CompressionType type);

  static std::string GetCompressorName(CompressionType type);
};

}  // namespace leveldb

#endif
