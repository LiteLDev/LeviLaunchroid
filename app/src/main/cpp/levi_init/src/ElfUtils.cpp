
#include <dlfcn.h>
#include <linux/elf.h>
#include <stdio.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <fcntl.h>
#include <string.h>
#include <dlfcn.h>
#include <sys/mman.h>
#include <fstream>
#include "vector"
#include <elf.h>
#include <dlfcn.h>
#include <link.h>
#include <sys/mman.h>
#include <string>
#include <string.h>
#include <vector>
#include <algorithm>


#ifdef __LP64__
#define __PRI_64_prefix "l"
#define __PRI_PTR_prefix "l"
#else
#define __PRI_64_prefix "ll"
#define __PRI_PTR_prefix
#endif

#define PRIxPTR __PRI_PTR_prefix "x"

namespace elf_util {
    typedef struct elfctx {
        void *header;

        uintptr_t load_bias;

        ElfW(Shdr) *sym_sh_;
        ElfW(Shdr) *dynsym_sh_;

        const char *strtab_;
        ElfW(Sym) *symtab_;

        const char *dynstrtab_;
        ElfW(Sym) *dynsymtab_;

        size_t nbucket_;
        size_t nchain_;
        uint32_t *bucket_;
        uint32_t *chain_;

        size_t gnu_nbucket_;
        uint32_t *gnu_bucket_;
        uint32_t *gnu_chain_;
        uint32_t gnu_maskwords_;
        uint32_t gnu_shift2_;
        ElfW(Addr) *gnu_bloom_filter_;
    } elf_ctx_t;


    typedef struct _RuntimeModule {
        char path[1024];
        void *load_address;
    } RuntimeModule;

    typedef uintptr_t addr_t;

    static std::vector<RuntimeModule> *modules;

    static std::vector<RuntimeModule> &getProcessMapWithProcMaps() {
        if (modules == nullptr) {
            modules = new std::vector<RuntimeModule>();
        }
        FILE *fp = fopen("/proc/self/maps", "r");
        if (fp == nullptr)
            return *modules;

        while (!feof(fp)) {
            char line_buffer[LINE_MAX + 1];
            fgets(line_buffer, LINE_MAX, fp);

            if (strlen(line_buffer) == LINE_MAX && line_buffer[LINE_MAX] != '\n') {
                int c;
                do {
                    c = getc(fp);
                } while ((c != EOF) && (c != '\n'));
                if (c == EOF)
                    break;
            }

            addr_t region_start, region_end;
            addr_t region_offset;
            char permissions[5] = {'\0'};
            uint8_t dev_major = 0;
            uint8_t dev_minor = 0;
            long inode = 0;
            int path_index = 0;

            if (sscanf(line_buffer,
                       "%" PRIxPTR "-%" PRIxPTR " %4c "
                       "%" PRIxPTR " %hhx:%hhx %ld %n",
                       &region_start, &region_end, permissions, &region_offset, &dev_major,
                       &dev_minor, &inode,
                       &path_index) < 7) {
                printf("/proc/self/maps parse failed!");
                fclose(fp);
                return *modules;
            }

            if (strcmp(permissions, "r--p") != 0 && strcmp(permissions, "r-xp") != 0)
                continue;
            ElfW(Ehdr) *header = (ElfW(Ehdr) *) region_start;
            if (memcmp(header->e_ident, ELFMAG, SELFMAG) != 0) {
                continue;
            }

            char *path_buffer = line_buffer + path_index;
            if (*path_buffer == 0 || *path_buffer == '\n' || *path_buffer == '[')
                continue;
            RuntimeModule module;

            if (path_buffer[strlen(path_buffer) - 1] == '\n') {
                path_buffer[strlen(path_buffer) - 1] = 0;
            }
            strncpy(module.path, path_buffer, sizeof(module.path) - 1);
            module.load_address = (void *) region_start;
            modules->push_back(module);

            printf("module: %s", module.path);
        }

        fclose(fp);
        return *modules;
    }

    RuntimeModule GetProcessMaps(const char *name) {
        auto modules = getProcessMapWithProcMaps();
        for (auto module: modules) {
            if (strstr(module.path, name) != 0) {
                return module;
            }
        }
        return RuntimeModule{0};
    }


    int linkerElfCtxInit(elf_ctx_t *ctx, void *header_) {
        ElfW(Ehdr) *ehdr = (ElfW(Ehdr) *) header_;
        ctx->header = ehdr;

        ElfW(Addr) ehdr_addr = (ElfW(Addr)) ehdr;

        {
            ElfW(Addr) addr = 0;
            ElfW(Dyn) *dyn = NULL;
            ElfW(Phdr) *phdr = reinterpret_cast<ElfW(Phdr) *>(ehdr_addr + ehdr->e_phoff);
            for (size_t i = 0; i < ehdr->e_phnum; i++) {
                if (phdr[i].p_type == PT_DYNAMIC) {
                    dyn = reinterpret_cast<ElfW(Dyn) *>(ehdr_addr + phdr[i].p_offset);
                } else if (phdr[i].p_type == PT_LOAD) {
                    addr = ehdr_addr + phdr[i].p_offset - phdr[i].p_vaddr;
                    if (ctx->load_bias == 0)
                        ctx->load_bias = ehdr_addr - (phdr[i].p_vaddr - phdr[i].p_offset);
                } else if (phdr[i].p_type == PT_PHDR) {
                    ctx->load_bias = (ElfW(Addr)) phdr - phdr[i].p_vaddr;
                }
            }
        }

        {
            ElfW(Shdr) *dynsym_sh, *dynstr_sh;
            ElfW(Shdr) *sym_sh, *str_sh;

            ElfW(Shdr) *shdr = reinterpret_cast<ElfW(Shdr) *>(ehdr_addr + ehdr->e_shoff);

            ElfW(Shdr) *shstr_sh = NULL;
            shstr_sh = &shdr[ehdr->e_shstrndx];
            char *shstrtab = NULL;
            shstrtab = (char *) ((addr_t) ehdr_addr + shstr_sh->sh_offset);

            for (size_t i = 0; i < ehdr->e_shnum; i++) {
                if (shdr[i].sh_type == SHT_SYMTAB) {
                    sym_sh = &shdr[i];
                    ctx->sym_sh_ = sym_sh;
                    ctx->symtab_ = (ElfW(Sym) *) (ehdr_addr + shdr[i].sh_offset);
                } else if (shdr[i].sh_type == SHT_STRTAB &&
                           strcmp(shstrtab + shdr[i].sh_name, ".strtab") == 0) {
                    str_sh = &shdr[i];
                    ctx->strtab_ = (const char *) (ehdr_addr + shdr[i].sh_offset);
                } else if (shdr[i].sh_type == SHT_DYNSYM) {
                    dynsym_sh = &shdr[i];
                    ctx->dynsym_sh_ = dynsym_sh;
                    ctx->dynsymtab_ = (ElfW(Sym) *) (ehdr_addr + shdr[i].sh_offset);
                } else if (shdr[i].sh_type == SHT_STRTAB &&
                           strcmp(shstrtab + shdr[i].sh_name, ".dynstr") == 0) {
                    dynstr_sh = &shdr[i];
                    ctx->dynstrtab_ = (const char *) (ehdr_addr + shdr[i].sh_offset);
                }
            }
        }

        return 0;
    }

    static void *
    iterateSymbolTableImpl(const char *symbol_name, ElfW(Sym) *symtab, const char *strtab,
                           int count) {
        for (int i = 0; i < count; ++i) {
            ElfW(Sym) *sym = symtab + i;
            const char *symbol_name_ = strtab + sym->st_name;
            if (strcmp(symbol_name_, symbol_name) == 0) {
                return (void *) sym->st_value;
            }
        }
        return NULL;
    }

    void *linkerElfCtxIterateSymbolTable(elf_ctx_t *ctx, const char *symbol_name) {
        void *result = NULL;
        if (ctx->symtab_ && ctx->strtab_) {
            size_t count = ctx->sym_sh_->sh_size / sizeof(ElfW(Sym));
            result = iterateSymbolTableImpl(symbol_name, ctx->symtab_, ctx->strtab_, count);
            if (result)
                return result;
        }

        if (ctx->dynsymtab_ && ctx->dynstrtab_) {
            size_t count = ctx->dynsym_sh_->sh_size / sizeof(ElfW(Sym));
            result = iterateSymbolTableImpl(symbol_name, ctx->dynsymtab_, ctx->dynstrtab_, count);
            if (result)
                return result;
        }
        return NULL;
    }

    void *resolveElfSymbol(const char *library_name, const char *symbol_name) {
        void *result = NULL;

        elf_ctx_t ctx;
        memset(&ctx, 0, sizeof(elf_ctx_t));
        RuntimeModule module = GetProcessMaps(library_name);
        if (module.load_address) {
            size_t file_size = 0;
            {
                struct stat s;
                int rt = stat(module.path, &s);
                if (rt != 0) {
                    return NULL;
                }
                file_size = s.st_size;
            }
            int fd = open(module.path, O_RDONLY, 0);
            if (fd < 0) {
                return NULL;
            }

            auto mmap_buffer = (uint8_t *) mmap(0, file_size, PROT_READ | PROT_WRITE,
                                                MAP_FILE | MAP_PRIVATE, fd, 0);
            if (mmap_buffer == MAP_FAILED) {
                return NULL;
            }


            linkerElfCtxInit(&ctx, mmap_buffer);
            result = linkerElfCtxIterateSymbolTable(&ctx, symbol_name);

            if (result)
                result = (void *) ((addr_t) result + (addr_t) module.load_address -
                                   ((addr_t) mmap_buffer - (addr_t) ctx.load_bias));
            munmap(mmap_buffer, file_size);
            close(fd);

        }
        return result;
    }

    int getAndroidSystemVersion() {
        char os_version_str[100];
        __system_property_get("ro.build.version.sdk", os_version_str);
        int os_version_int = atoi(os_version_str);
        return os_version_int;
    }


    const char *getAndroidLinkerPath() {
#if __LP64__
        if (getAndroidSystemVersion() >= 29) {
            return (const char *) "/apex/com.android.runtime/bin/linker64";
        } else {
            return (const char *) "/system/bin/linker64";
        }
#else
        if (getAndroidSystemVersion() >= 29) {
            return (const char *)"/apex/com.android.runtime/bin/linker";
        } else {
            return (const char *)"/system/bin/linker";
        }
#endif
    }
}