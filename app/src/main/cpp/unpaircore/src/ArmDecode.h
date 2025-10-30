#pragma once
#include <cstdint>

namespace ArmDecode {

    inline uintptr_t decodeADRP(uint32_t insn, uintptr_t pc) noexcept {
        const int64_t immlo = (insn >> 29) & 0x3;
        const int64_t immhi = (insn >> 5) & 0x7FFFF;
        int64_t imm21 = (immhi << 2) | immlo;
        if (imm21 & (1 << 20))
            imm21 |= ~((1LL << 21) - 1);
        const uintptr_t page_addr = (pc & ~0xFFFULL) + (imm21 << 12);
        return page_addr;
    }

    inline uint32_t decodeADD(uint32_t insn) noexcept {
        return (insn >> 10) & 0xFFF;
    }

} // namespace ArmDecode