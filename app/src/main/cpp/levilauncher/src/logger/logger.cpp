#include "logger.h"
#include <cstdarg>
#include <android/log.h>

static constexpr const char* LOG_TAG = "LeviLogger";

Logger::Logger(const std::string &name) : loggerName(name) {}

void Logger::log(int level, const char *fmt, va_list args) {
    std::string format = "[" + loggerName + "] ";
    format += fmt;

    __android_log_vprint(level, LOG_TAG, format.c_str(), args);
}

void Logger::info(const char *fmt, ...) {
    va_list args;
    va_start(args, fmt);
    log(ANDROID_LOG_INFO, fmt, args);
    va_end(args);
}

void Logger::debug(const char *fmt, ...) {
    va_list args;
    va_start(args, fmt);
    log(ANDROID_LOG_DEBUG, fmt, args);
    va_end(args);
}

void Logger::warn(const char *fmt, ...) {
    va_list args;
    va_start(args, fmt);
    log(ANDROID_LOG_WARN, fmt, args);
    va_end(args);
}

void Logger::error(const char *fmt, ...) {
    va_list args;
    va_start(args, fmt);
    log(ANDROID_LOG_ERROR, fmt, args);
    va_end(args);
}