// Copyright © 2025 GlacieTeam. All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// SPDX-License-Identifier: MPL-2.0

#pragma once
#include <nbt-c/Macros.h>
#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

struct nbtio_buffer {
    uint8_t* data;
    size_t   size;
};
NBT_API void nbtio_buffer_free(nbtio_buffer* buffer);

enum NBT_Type {
    TAG_END       = 0,
    TAG_BYTE      = 1,
    TAG_SHORT     = 2,
    TAG_INT       = 3,
    TAG_LONG      = 4,
    TAG_FLOAT     = 5,
    TAG_DOUBLE    = 6,
    TAG_BYTEARRAY = 7,
    TAG_STRING    = 8,
    TAG_LIST      = 9,
    TAG_COMPOUND  = 10,
    TAG_INTARRAY  = 11,
    TAG_LONGARRAY = 12,
};

enum SNBT_Format {
    SNBT_MINIMIZE                      = 0,
    SNBT_COMPOUND_LINE_FEED            = 1,
    SNBT_LIST_ARRAY_LINE_FEED          = 2,
    SNBT_PRETTY_FILE_PRINT             = 3,
    SNBT_BINARY_ARRAY_LINE_FEED        = 4,
    SNBT_ARRAY_LINE_FEED               = 6,
    SNBT_ALWAYS_LINE_FEED              = 7,
    SNBT_FORCE_LINE_FEED_IGNORE_INDENT = 8,
    SNBT_FORCE_ASCII                   = 16,
    SNBT_FORCE_KEY_QUOTE               = 32,
    SNBT_FORCE_VALUE_QUOTE             = 64,
    SNBT_FORCE_QUOTE                   = 96,
    SNBT_CLASSIC                       = 99,
    SNBT_FORCE_UPPERCASE               = 128,
    SNBT_MARK_INT_TAG                  = 256,
    SNBT_MARK_DOUBLE_TAG               = 512,
    COMMENTS_MARK                      = 1024,
    SNBT_JSONIFY                       = 1127,
    MARK_SIGNED                        = 2048,
};

enum NBT_FileFormat {
    NBT_FORMAT_INVALID                   = -1,
    NBT_FORMAT_LITTLE_ENDIAN             = 0,
    NBT_FORMAT_LITTLE_ENDIAN_WITH_HEADER = 1,
    NBT_FORMAT_BIG_ENDIAN                = 2,
    NBT_FORMAT_BIG_ENDIAN_WITH_HEADER    = 3,
    NBT_FORMAT_BEDROCK_NETWORK           = 4,
};

enum NBT_CompressionType {
    NBT_COMPRESSION_NONE = 0,
    NBT_COMPRESSION_GZIP = 1,
    NBT_COMPRESSION_ZLIB = 2,
};

enum SNBT_NumberFormat {
    SNBT_NUMBER_DECIMAL           = 0,
    SNBT_NUMBER_LOWER_HEXADECIMAL = 1,
    SNBT_NUMBER_UPPER_HEXADECIMAL = 2,
    SNBT_NUMBER_BINARY            = 3,
};

// Any Tag
NBT_API NBT_Type nbt_any_tag_get_type(void* handle);
NBT_API bool     nbt_any_tag_equals(void* handle, void* other);
NBT_API void*    nbt_any_tag_copy(void* handle);
NBT_API size_t   nbt_any_tag_hash(void* handle);
NBT_API void     nbt_any_tag_write(void* handle, void* stream);
NBT_API void     nbt_any_tag_load(void* handle, void* stream);
NBT_API void     nbt_any_tag_free(void* handle);

NBT_API nbtio_buffer*
nbt_any_tag_to_snbt(void* handle, SNBT_Format format, uint8_t indent, SNBT_NumberFormat number_format);
NBT_API nbtio_buffer* nbt_any_tag_to_json(void* handle, uint8_t indent);

// EndTag
NBT_API void* nbt_end_tag_create();

// ByteTag
NBT_API void*   nbt_byte_tag_create(uint8_t value);
NBT_API void    nbt_byte_tag_set_value(void* handle, uint8_t value);
NBT_API uint8_t nbt_byte_tag_get_value(void* handle);

// ShortTag
NBT_API void* nbt_short_tag_create(short value);
NBT_API void  nbt_short_tag_set_value(void* handle, short value);
NBT_API short nbt_short_tag_get_value(void* handle);

// IntTag
NBT_API void* nbt_int_tag_create(int value);
NBT_API void  nbt_int_tag_set_value(void* handle, int value);
NBT_API int   nbt_int_tag_get_value(void* handle);

// LongTag
NBT_API void*   nbt_long_tag_create(int64_t value);
NBT_API void    nbt_long_tag_set_value(void* handle, int64_t value);
NBT_API int64_t nbt_long_tag_get_value(void* handle);

// FloatTag
NBT_API void* nbt_float_tag_create(float value);
NBT_API void  nbt_float_tag_set_value(void* handle, float value);
NBT_API float nbt_float_tag_get_value(void* handle);

// DoubleTag
NBT_API void*  nbt_double_tag_create(double value);
NBT_API void   nbt_double_tag_set_value(void* handle, double value);
NBT_API double nbt_double_tag_get_value(void* handle);

// ByteArrayTag
NBT_API void*         nbt_byte_array_tag_create(const uint8_t* data, size_t size);
NBT_API void          nbt_byte_array_tag_set_value(void* handle, const uint8_t* data, size_t size);
NBT_API nbtio_buffer* nbt_byte_array_tag_get_value(void* handle);

// StringTag
NBT_API void*         nbt_string_tag_create(const char* data, size_t size);
NBT_API void          nbt_string_tag_set_value(void* handle, const char* data, size_t size);
NBT_API nbtio_buffer* nbt_string_tag_get_value(void* handle);

// ListTag
NBT_API void*  nbt_list_tag_create();
NBT_API size_t nbt_list_tag_size(void* handle);
NBT_API void   nbt_list_tag_add_tag(void* handle, void* tag);
NBT_API void*  nbt_list_tag_get_tag(void* handle, size_t index);
NBT_API bool   nbt_list_tag_remove_tag(void* handle, size_t index);
NBT_API void   nbt_list_tag_clear(void* handle);
NBT_API bool   nbt_list_tag_set_tag(void* handle, size_t index, void* tag);

// CompoundTag
NBT_API void*         nbt_compound_tag_create();
NBT_API size_t        nbt_compound_tag_size(void* handle);
NBT_API void          nbt_compound_tag_set_tag(void* handle, const char* key, void* tag);
NBT_API void*         nbt_compound_tag_get_tag(void* handle, const char* key);
NBT_API nbtio_buffer* nbt_compound_tag_get_key_index(void* handle, size_t index);
NBT_API void*         nbt_compound_tag_get_tag_index(void* handle, size_t index);
NBT_API bool          nbt_compound_tag_has_tag(void* handle, const char* key);
NBT_API bool          nbt_compound_tag_remove_tag(void* handle, const char* key);
NBT_API void          nbt_compound_tag_clear(void* handle);

NBT_API nbtio_buffer* nbt_compound_tag_to_binary_nbt(void* handle, bool little_endian, bool write_header);
NBT_API nbtio_buffer* nbt_compound_tag_to_network_nbt(void* handle);

NBT_API void* nbt_compound_tag_from_binary_nbt(const uint8_t* data, size_t size, bool little_endian, bool read_header);
NBT_API void* nbt_compound_tag_from_network_nbt(const uint8_t* data, size_t size);
NBT_API void* nbt_compound_tag_from_snbt(const uint8_t* data, size_t size);
NBT_API void* nbt_compound_tag_from_json(const uint8_t* data, size_t size);

// IntArrayTag
NBT_API void*  nbt_int_array_tag_create();
NBT_API size_t nbt_int_array_tag_size(void* handle);
NBT_API void   nbt_int_array_tag_add_value(void* handle, int value);
NBT_API int    nbt_int_array_tag_get_value(void* handle, size_t index);
NBT_API bool   nbt_int_array_tag_remove_value(void* handle, size_t index);
NBT_API void   nbt_int_array_tag_clear(void* handle);
NBT_API bool   nbt_int_array_tag_set_value(void* handle, size_t index, int value);

// LongArrayTag
NBT_API void*   nbt_long_array_tag_create();
NBT_API size_t  nbt_long_array_tag_size(void* handle);
NBT_API void    nbt_long_array_tag_add_value(void* handle, int64_t value);
NBT_API int64_t nbt_long_array_tag_get_value(void* handle, size_t index);
NBT_API bool    nbt_long_array_tag_remove_value(void* handle, size_t index);
NBT_API void    nbt_long_array_tag_clear(void* handle);
NBT_API bool    nbt_long_array_tag_set_value(void* handle, size_t index, int64_t value);

// FileIO
NBT_API void* nbt_parse_from_file(const char* path, NBT_FileFormat format);
NBT_API bool  nbt_save_to_file(
     void*               handle,
     const char*         path,
     NBT_FileFormat      format,
     NBT_CompressionType compressionType,
     int                 compressionLevel
 );
NBT_API void* nbt_parse_snbt_from_file(const char* path);
NBT_API bool  nbt_save_snbt_to_file(void* handle, const char* path, SNBT_Format format, uint8_t indent);

NBT_API bool nbt_validate_file(const char* path, NBT_FileFormat format, bool fmmap, bool strictMatchSize);
NBT_API bool nbt_validate_content(const uint8_t* data, size_t size, NBT_FileFormat format, bool strictMatchSize);

NBT_API NBT_FileFormat nbt_detect_file_format(const char* path, bool fmmap);
NBT_API NBT_FileFormat nbt_detect_content_format(const uint8_t* data, size_t size);

NBT_API NBT_CompressionType nbt_detect_file_compression_type(const char* path, bool fmmap);
NBT_API NBT_CompressionType nbt_detect_content_compression_type(const uint8_t* data, size_t size);

#ifdef __cplusplus
}
#endif