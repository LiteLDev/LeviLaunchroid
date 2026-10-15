#include "nbt-c/NBT.h"
#include "nbt/NBT.hpp"

namespace {

inline nbt::Tag* toTag(void* handle) { return reinterpret_cast<nbt::Tag*>(handle); }

inline nbtio_buffer* make_nbtio_buffer(std::string_view buffer) {
    auto result  = new nbtio_buffer();
    result->data = new uint8_t[buffer.size()];
    std::copy_n(buffer.data(), buffer.size(), result->data);
    result->size = buffer.size();
    return result;
}

inline nbtio_buffer* make_nbtio_buffer(std::vector<uint8_t> const& buffer) {
    auto result  = new nbtio_buffer();
    result->data = new uint8_t[buffer.size()];
    std::copy_n(buffer.data(), buffer.size(), result->data);
    result->size = buffer.size();
    return result;
}

} // namespace

extern "C" {

void nbtio_buffer_free(nbtio_buffer* buffer) {
    if (buffer) {
        if (buffer->data) { delete[] buffer->data; }
        delete buffer;
    }
}

// Any Tag
NBT_Type nbt_any_tag_get_type(void* handle) {
    if (!handle) { return NBT_Type::TAG_END; }
    return static_cast<NBT_Type>(toTag(handle)->getType());
}

bool nbt_any_tag_equals(void* handle, void* other) {
    if (!handle) { return false; }
    return toTag(handle)->equals(*toTag(other));
}

void* nbt_any_tag_copy(void* handle) {
    if (!handle) { return nullptr; }
    return toTag(handle)->copy().release();
}

size_t nbt_any_tag_hash(void* handle) {
    if (!handle) { return 0; }
    return toTag(handle)->hash();
}

void nbt_any_tag_write(void* handle, void* stream) {
    if (handle) { toTag(handle)->write(*reinterpret_cast<bstream::BinaryStream*>(stream)); }
}

void nbt_any_tag_load(void* handle, void* stream) {
    if (handle) { return toTag(handle)->load(*reinterpret_cast<bstream::ReadOnlyBinaryStream*>(stream)); }
}

void nbt_any_tag_free(void* handle) {
    if (!handle) { return; }
    switch (nbt_any_tag_get_type(handle)) {
    case TAG_BYTE: {
        delete reinterpret_cast<nbt::ByteTag*>(handle);
        break;
    }
    case TAG_SHORT: {
        delete reinterpret_cast<nbt::ShortTag*>(handle);
        break;
    }
    case TAG_INT: {
        delete reinterpret_cast<nbt::IntTag*>(handle);
        break;
    }
    case TAG_LONG: {
        delete reinterpret_cast<nbt::LongTag*>(handle);
        break;
    }
    case TAG_FLOAT: {
        delete reinterpret_cast<nbt::FloatTag*>(handle);
        break;
    }
    case TAG_DOUBLE: {
        delete reinterpret_cast<nbt::DoubleTag*>(handle);
        break;
    }
    case TAG_BYTEARRAY: {
        delete reinterpret_cast<nbt::ByteArrayTag*>(handle);
        break;
    }
    case TAG_STRING: {
        delete reinterpret_cast<nbt::StringTag*>(handle);
        break;
    }
    case TAG_LIST: {
        delete reinterpret_cast<nbt::ListTag*>(handle);
        break;
    }
    case TAG_COMPOUND: {
        delete reinterpret_cast<nbt::CompoundTag*>(handle);
        break;
    }
    case TAG_INTARRAY: {
        delete reinterpret_cast<nbt::IntArrayTag*>(handle);
        break;
    }
    case TAG_LONGARRAY: {
        delete reinterpret_cast<nbt::LongArrayTag*>(handle);
        break;
    }
    default:
        delete reinterpret_cast<nbt::EndTag*>(handle);
        break;
    }
}

nbtio_buffer* nbt_any_tag_to_snbt(void* handle, SNBT_Format format, uint8_t indent, SNBT_NumberFormat number_format) {
    if (handle) {
        std::string value = toTag(handle)->as<nbt::Tag>().toSnbt(
            static_cast<nbt::SnbtFormat>(format),
            indent,
            static_cast<nbt::SnbtNumberFormat>(number_format)
        );
        return make_nbtio_buffer(value);
    }
    return nullptr;
}
nbtio_buffer* nbt_any_tag_to_json(void* handle, uint8_t indent) {
    if (handle) {
        std::string value = toTag(handle)->as<nbt::Tag>().toJson(indent);
        return make_nbtio_buffer(value);
    }
    return nullptr;
}

// EndTag
void* nbt_end_tag_create() { return new nbt::EndTag(); }

// ByteTag
void* nbt_byte_tag_create(uint8_t value) { return new nbt::ByteTag(value); }

void nbt_byte_tag_set_value(void* handle, uint8_t value) {
    if (handle) { toTag(handle)->as<nbt::ByteTag>() = value; }
}

uint8_t nbt_byte_tag_get_value(void* handle) {
    if (!handle) { return 0; }
    return toTag(handle)->as<nbt::ByteTag>();
}

// ShortTag
void* nbt_short_tag_create(short value) { return new nbt::ShortTag(value); }

void nbt_short_tag_set_value(void* handle, short value) {
    if (handle) { toTag(handle)->as<nbt::ShortTag>() = value; }
}

short nbt_short_tag_get_value(void* handle) {
    if (!handle) { return 0; }
    return toTag(handle)->as<nbt::ShortTag>();
}

// IntTag
void* nbt_int_tag_create(int value) { return new nbt::IntTag(value); }

void nbt_int_tag_set_value(void* handle, int value) {
    if (handle) { toTag(handle)->as<nbt::IntTag>() = value; }
}

int nbt_int_tag_get_value(void* handle) {
    if (!handle) { return 0; }
    return toTag(handle)->as<nbt::IntTag>();
}

// LongTag
void* nbt_long_tag_create(int64_t value) { return new nbt::LongTag(value); }

void nbt_long_tag_set_value(void* handle, int64_t value) {
    if (handle) { toTag(handle)->as<nbt::LongTag>() = value; }
}

int64_t nbt_long_tag_get_value(void* handle) {
    if (!handle) { return 0; }
    return toTag(handle)->as<nbt::LongTag>();
}

// FloatTag
void* nbt_float_tag_create(float value) { return new nbt::FloatTag(value); }

void nbt_float_tag_set_value(void* handle, float value) {
    if (handle) { toTag(handle)->as<nbt::FloatTag>() = value; }
}

float nbt_float_tag_get_value(void* handle) {
    if (!handle) { return 0; }
    return toTag(handle)->as<nbt::FloatTag>();
}

// DoubleTag
void* nbt_double_tag_create(double value) { return new nbt::DoubleTag(value); }

void nbt_double_tag_set_value(void* handle, double value) {
    if (handle) { toTag(handle)->as<nbt::DoubleTag>() = value; }
}

double nbt_double_tag_get_value(void* handle) {
    if (!handle) { return 0; }
    return toTag(handle)->as<nbt::DoubleTag>();
}

// ByteArrayTag
void* nbt_byte_array_tag_create(const uint8_t* data, size_t size) {
    if (data) { return new nbt::ByteArrayTag(data, size); }
    return new nbt::ByteArrayTag();
}

void nbt_byte_array_tag_set_value(void* handle, const uint8_t* data, size_t size) {
    if (handle) { toTag(handle)->as<nbt::ByteArrayTag>().reinit(data, size); }
}

nbtio_buffer* nbt_byte_array_tag_get_value(void* handle) {
    if (handle) {
        std::vector<uint8_t> value = toTag(handle)->as<nbt::ByteArrayTag>();
        return make_nbtio_buffer(value);
    }
    return nullptr;
}

// StringTag
void* nbt_string_tag_create(const char* data, size_t size) {
    if (data) { return new nbt::StringTag(data, size); }
    return new nbt::StringTag();
}

void nbt_string_tag_set_value(void* handle, const char* data, size_t size) {
    if (handle && data) { toTag(handle)->as<nbt::StringTag>() = std::string(data, size); }
}

nbtio_buffer* nbt_string_tag_get_value(void* handle) {
    if (handle) {
        std::string value = toTag(handle)->as<nbt::StringTag>();
        return make_nbtio_buffer(value);
    }
    return nullptr;
}

// ListTag
void* nbt_list_tag_create() { return new nbt::ListTag(); }

size_t nbt_list_tag_size(void* handle) {
    if (!handle) { return 0; }
    return toTag(handle)->as<nbt::ListTag>().size();
}

void nbt_list_tag_add_tag(void* handle, void* tag) {
    if (handle && tag) { toTag(handle)->as<nbt::ListTag>().push_back(toTag(tag)->copy()); }
}

void* nbt_list_tag_get_tag(void* handle, size_t index) {
    if (handle) {
        auto& tag = toTag(handle)->as<nbt::ListTag>();
        if (index < tag.size()) { return tag[index].toUniqueCopy().release(); }
    }
    return nullptr;
}

bool nbt_list_tag_remove_tag(void* handle, size_t index) {
    if (!handle) { return false; }
    return toTag(handle)->as<nbt::ListTag>().remove(index);
}

void nbt_list_tag_clear(void* handle) {
    if (handle) { return toTag(handle)->as<nbt::ListTag>().clear(); }
}

bool nbt_list_tag_set_tag(void* handle, size_t index, void* tag) {
    if (handle) {
        auto& listTag = toTag(handle)->as<nbt::ListTag>();
        if (index < listTag.size()) { return listTag.set(index, *toTag(tag)); }
    }
    return false;
}

// CompoundTag
void* nbt_compound_tag_create() { return new nbt::CompoundTag(); }

size_t nbt_compound_tag_size(void* handle) {
    if (!handle) { return 0; }
    return toTag(handle)->as<nbt::CompoundTag>().size();
}

void nbt_compound_tag_set_tag(void* handle, const char* key, void* tag) {
    if (!handle) { return; }
    toTag(handle)->as<nbt::CompoundTag>().put(key, toTag(tag)->copy());
}

bool nbt_compound_tag_has_tag(void* handle, const char* key) {
    if (handle) { return toTag(handle)->as<nbt::CompoundTag>().contains(key); }
    return false;
}

void* nbt_compound_tag_get_tag(void* handle, const char* key) {
    if (handle) {
        if (auto tag = toTag(handle)->as<nbt::CompoundTag>().get(key)) { return tag->copy().release(); }
    }
    return nullptr;
}

nbtio_buffer* nbt_compound_tag_get_key_index(void* handle, size_t index) {
    if (handle) {
        auto nbt = toTag(handle)->as<nbt::CompoundTag>();
        auto it  = std::next(nbt.begin(), static_cast<nbt::CompoundTag::TagMap::difference_type>(index));
        if (it != nbt.end()) {
            auto value = it->first;
            return make_nbtio_buffer(value);
        }
    }
    return nullptr;
}

void* nbt_compound_tag_get_tag_index(void* handle, size_t index) {
    if (handle) {
        auto nbt = toTag(handle)->as<nbt::CompoundTag>();
        auto it  = std::next(nbt.begin(), static_cast<nbt::CompoundTag::TagMap::difference_type>(index));
        if (it != nbt.end()) { return it->second.toUniqueCopy().release(); }
    }
    return nullptr;
}

bool nbt_compound_tag_remove_tag(void* handle, const char* key) {
    if (!handle) { return false; }
    return toTag(handle)->as<nbt::CompoundTag>().remove(key);
}

void nbt_compound_tag_clear(void* handle) {
    if (handle) { toTag(handle)->as<nbt::CompoundTag>().clear(); }
}

nbtio_buffer* nbt_compound_tag_to_binary_nbt(void* handle, bool little_endian, bool write_header) {
    if (handle) {
        std::string value = write_header ? toTag(handle)->as<nbt::CompoundTag>().toBinaryNbtWithHeader(little_endian)
                                         : toTag(handle)->as<nbt::CompoundTag>().toBinaryNbt(little_endian);
        return make_nbtio_buffer(value);
    }
    return nullptr;
}

nbtio_buffer* nbt_compound_tag_to_network_nbt(void* handle) {
    if (handle) {
        std::string value = toTag(handle)->as<nbt::CompoundTag>().toNetworkNbt();
        return make_nbtio_buffer(value);
    }
    return nullptr;
}

void* nbt_compound_tag_from_binary_nbt(const uint8_t* data, size_t size, bool little_endian, bool read_header) {
    std::string_view content(reinterpret_cast<const char*>(data), size);
    auto             result = read_header ? nbt::CompoundTag::fromBinaryNbtWithHeader(content, little_endian)
                                          : nbt::CompoundTag::fromBinaryNbt(content, little_endian);
    return new nbt::CompoundTag(result);
}

void* nbt_compound_tag_from_network_nbt(const uint8_t* data, size_t size) {
    std::string_view content(reinterpret_cast<const char*>(data), size);
    auto             result = nbt::CompoundTag::fromNetworkNbt(content);
    return new nbt::CompoundTag(result);
}

void* nbt_compound_tag_from_snbt(const uint8_t* data, size_t size) {
    std::string_view content(reinterpret_cast<const char*>(data), size);
    if (auto result = nbt::CompoundTag::fromSnbt(content)) { return new nbt::CompoundTag(*result); }
    return nullptr;
}

void* nbt_compound_tag_from_json(const uint8_t* data, size_t size) {
    std::string_view content(reinterpret_cast<const char*>(data), size);
    if (auto result = nbt::CompoundTag::fromJson(content)) { return new nbt::CompoundTag(*result); }
    return nullptr;
}


// IntArrayTag
void* nbt_int_array_tag_create() { return new nbt::IntArrayTag(); }

size_t nbt_int_array_tag_size(void* handle) {
    if (!handle) { return 0; }
    return toTag(handle)->as<nbt::IntArrayTag>().size();
}

void nbt_int_array_tag_add_value(void* handle, int value) { toTag(handle)->as<nbt::IntArrayTag>().push_back(value); }

int nbt_int_array_tag_get_value(void* handle, size_t index) {
    if (handle) {
        auto& tag = toTag(handle)->as<nbt::IntArrayTag>();
        if (index < tag.size()) { return tag[index]; }
    }
    return 0;
}

bool nbt_int_array_tag_remove_value(void* handle, size_t index) {
    if (!handle) { return false; }
    return toTag(handle)->as<nbt::IntArrayTag>().remove(index);
}

void nbt_int_array_tag_clear(void* handle) {
    if (handle) { return toTag(handle)->as<nbt::IntArrayTag>().clear(); }
}

bool nbt_int_array_tag_set_value(void* handle, size_t index, int value) {
    if (handle) {
        auto& intArrayTag = toTag(handle)->as<nbt::IntArrayTag>();
        if (index < intArrayTag.size()) {
            intArrayTag[index] = value;
            return true;
        }
    }
    return false;
}

// LongArrayTag
void* nbt_long_array_tag_create() { return new nbt::LongArrayTag(); }

size_t nbt_long_array_tag_size(void* handle) {
    if (!handle) { return 0; }
    return toTag(handle)->as<nbt::LongArrayTag>().size();
}

void nbt_long_array_tag_add_value(void* handle, int64_t value) {
    toTag(handle)->as<nbt::LongArrayTag>().push_back(value);
}

int64_t nbt_long_array_tag_get_value(void* handle, size_t index) {
    if (handle) {
        auto& tag = toTag(handle)->as<nbt::LongArrayTag>();
        if (index < tag.size()) { return tag[index]; }
    }
    return 0;
}

bool nbt_long_array_tag_remove_value(void* handle, size_t index) {
    if (!handle) { return false; }
    return toTag(handle)->as<nbt::LongArrayTag>().remove(index);
}

void nbt_long_array_tag_clear(void* handle) {
    if (handle) { return toTag(handle)->as<nbt::LongArrayTag>().clear(); }
}

bool nbt_long_array_tag_set_value(void* handle, size_t index, int64_t value) {
    if (handle) {
        auto& longArrayTag = toTag(handle)->as<nbt::LongArrayTag>();
        if (index < longArrayTag.size()) {
            longArrayTag[index] = value;
            return true;
        }
    }
    return false;
}

// File IO
void* nbt_parse_from_file(const char* path, NBT_FileFormat format) {
    if (auto tag = nbt::io::parseFromFile(path, static_cast<nbt::NbtFileFormat>(format))) {
        return tag->copy().release();
    }
    return nullptr;
}

bool nbt_save_to_file(
    void*               handle,
    const char*         path,
    NBT_FileFormat      format,
    NBT_CompressionType compressionType,
    int                 compressionLevel
) {
    if (handle) {
        return nbt::io::saveToFile(
            toTag(handle)->as<nbt::CompoundTag>(),
            path,
            static_cast<nbt::NbtFileFormat>(format),
            static_cast<nbt::NbtCompressionType>(compressionType),
            static_cast<nbt::NbtCompressionLevel>(compressionLevel)
        );
    }
    return false;
}

void* nbt_parse_snbt_from_file(const char* path) {
    if (auto tag = nbt::io::parseSnbtFromFile(path)) { return tag->copy().release(); }
    return nullptr;
}

bool nbt_save_snbt_to_file(void* handle, const char* path, SNBT_Format format, uint8_t indent) {
    if (handle) {
        return nbt::io::saveSnbtToFile(
            toTag(handle)->as<nbt::CompoundTag>(),
            path,
            static_cast<nbt::SnbtFormat>(format),
            indent
        );
    }
    return false;
}

bool nbt_validate_file(const char* path, NBT_FileFormat format, bool fmmap, bool strictMatchSize) {
    return nbt::io::validateFile(path, static_cast<nbt::NbtFileFormat>(format), fmmap, strictMatchSize);
}

bool nbt_validate_content(const uint8_t* data, size_t size, NBT_FileFormat format, bool strictMatchSize) {
    return nbt::io::validateContent(
        std::string_view(reinterpret_cast<const char*>(data), size),
        static_cast<nbt::NbtFileFormat>(format),
        strictMatchSize
    );
}

NBT_FileFormat nbt_detect_file_format(const char* path, bool fmmap) {
    if (auto result = nbt::io::detectFileFormat(path, fmmap)) {
        return static_cast<NBT_FileFormat>(*result);
    } else {
        return NBT_FileFormat::NBT_FORMAT_INVALID;
    }
}
NBT_FileFormat nbt_detect_content_format(const uint8_t* data, size_t size) {
    if (auto result = nbt::io::detectContentFormat(std::string_view(reinterpret_cast<const char*>(data), size))) {
        return static_cast<NBT_FileFormat>(*result);
    } else {
        return NBT_FileFormat::NBT_FORMAT_INVALID;
    }
}

NBT_CompressionType nbt_detect_file_compression_type(const char* path, bool fmmap) {
    return static_cast<NBT_CompressionType>(nbt::io::detectFileCompressionType(path, fmmap));
}

NBT_CompressionType nbt_detect_content_compression_type(const uint8_t* data, size_t size) {
    return static_cast<NBT_CompressionType>(
        nbt::io::detectContentCompressionType(std::string_view(reinterpret_cast<const char*>(data), size))
    );
}
}