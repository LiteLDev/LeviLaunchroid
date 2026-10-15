#include "leveldb/decompress_allocator.h"

#include <utility>

namespace leveldb {

DecompressAllocator::~DecompressAllocator() = default;

std::string DecompressAllocator::get() {
  std::lock_guard<std::mutex> lock(mutex);
  if (stack.empty()) {
    return std::string();
  }

  std::string string = std::move(stack.back());
  stack.pop_back();
  return string;
}

void DecompressAllocator::release(std::string&& string) {
  string.clear();
  std::lock_guard<std::mutex> lock(mutex);
  stack.push_back(std::move(string));
}

void DecompressAllocator::prune() {
  std::lock_guard<std::mutex> lock(mutex);
  stack.clear();
}

}  // namespace leveldb
