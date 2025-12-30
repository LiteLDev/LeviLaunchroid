#include "transition.h"
#include <sys/time.h>

static int64_t getEpochTime() {
    struct timeval tp;
    gettimeofday(&tp, nullptr);
    return tp.tv_sec * 1000 + tp.tv_usec / 1000;
}

void Transition::startTransition(uint64_t startVal, uint64_t endVal, int durationMs) {
    if (transitionInProgress) {
        start = current;
    } else {
        start = startVal;
    }
    end = endVal;
    transitionInProgress = true;
    startTime = getEpochTime();
    endTime = startTime + durationMs;
}

void Transition::tick() {
    if (!transitionInProgress) return;
    
    int64_t time = getEpochTime();
    int64_t timeSinceStart = time - startTime;
    double currentProgress = static_cast<double>(timeSinceStart) / static_cast<double>(endTime - startTime);
    
    if (currentProgress >= 1.0) {
        current = end;
        transitionInProgress = false;
        return;
    }
    
    if (start > end) {
        uint64_t diff = start - end;
        current = start - static_cast<uint64_t>(diff * currentProgress);
    } else {
        uint64_t diff = end - start;
        current = start + static_cast<uint64_t>(diff * currentProgress);
    }
}
