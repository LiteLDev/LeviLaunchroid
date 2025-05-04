#pragma once

#ifdef __cplusplus
#define PRELOADER_MAYBE_UNUSED [[maybe_unused]]
#else
#define PRELOADER_MAYBE_UNUSED
#endif

#ifdef PRELOADER_EXPORT
#define PLAPI [[maybe_unused]] __attribute__((visibility("default")))
#else
#define PLAPI [[maybe_unused]]
#endif

#ifdef __cplusplus
#define PLCAPI extern "C" PLAPI
#else
#define PLCAPI extern PLAPI
#endif