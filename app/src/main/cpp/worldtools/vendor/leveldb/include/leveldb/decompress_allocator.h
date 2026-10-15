#ifndef STORAGE_LEVELDB_INCLUDE_DECOMPRESS_ALLOCATOR_H_
#define STORAGE_LEVELDB_INCLUDE_DECOMPRESS_ALLOCATOR_H_

#include <mutex>
#include <string>
#include <vector>

#include "leveldb/export.h"

namespace leveldb {

class LEVELDB_EXPORT DecompressAllocator {
 public:
  virtual ~DecompressAllocator();

  virtual std::string get();

  virtual void release(std::string&& string);

  virtual void prune();

 protected:
  std::mutex mutex;
  std::vector<std::string> stack;
};

}  // namespace leveldb

#endif
