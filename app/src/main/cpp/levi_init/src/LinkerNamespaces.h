#pragma once
#include <string>
#include <vector>
#include <unordered_set>

struct soinfo;

#define DISALLOW_COPY_AND_ASSIGN(TypeName) \
  TypeName(const TypeName&) = delete;      \
  void operator=(const TypeName&) = delete

struct android_namespace_t {
public:
};