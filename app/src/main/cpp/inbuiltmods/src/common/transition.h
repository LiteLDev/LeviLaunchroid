#pragma once
#include <cstdint>

class Transition {
private:
    uint64_t start = 0;
    uint64_t end = 0;
    int64_t startTime = 0;
    int64_t endTime = 0;
    uint64_t current = 0;
    bool transitionInProgress = false;

public:
    void startTransition(uint64_t startVal, uint64_t endVal, int durationMs);
    void tick();
    uint64_t getCurrent() const { return current; }
    bool inProgress() const { return transitionInProgress; }
};
