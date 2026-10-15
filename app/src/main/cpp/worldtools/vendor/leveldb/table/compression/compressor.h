#ifndef STORAGE_LEVELDB_INCLUDE_DB_COMPRESSOR_H_
#define STORAGE_LEVELDB_INCLUDE_DB_COMPRESSOR_H_

#include <cstdint>
#include <string>

namespace leveldb {

class Compressor {
 public:
  virtual ~Compressor() {}

  virtual void compress(const char* input, size_t length,
                        ::std::string& output) const = 0;

  void compress(const std::string& in, std::string& out) const {
    compress(in.data(), in.length(), out);
  }

  virtual bool decompress(const char* input, size_t length,
                          ::std::string& output) const = 0;

  bool decompress(const std::string& input, ::std::string& output) const {
    return decompress(input.data(), input.length(), output);
  }

 protected:
 private:
};
}  // namespace leveldb

#endif