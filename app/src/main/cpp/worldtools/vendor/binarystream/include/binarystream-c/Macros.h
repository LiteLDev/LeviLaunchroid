#pragma once

#ifdef _BINARY_STREAM_EXPORT
#ifdef _WIN32
#ifdef _BINARY_STREAM_DLL
#define BSAPI __declspec(dllimport)
#else
#define BSAPI __declspec(dllexport)
#endif
#else
#define BSAPI __attribute__((visibility("default"), used))
#endif
#else
#define BSAPI
#endif