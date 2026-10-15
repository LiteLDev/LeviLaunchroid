#include <jni.h>
#include <nbt/NBT.hpp>
#include <nbt/detail/CompressionUtils.hpp>
#include <nbt/detail/SnbtDeserializer.hpp>
#include <cctype>
#include <leveldb/db.h>
#include <leveldb/cache.h>
#include <leveldb/write_batch.h>
#include <zlib.h>
#include <algorithm>
#include <cstdint>
#include <cstring>
#include <map>
#include <memory>
#include <mutex>
#include <set>
#include <string>
#include <vector>

namespace {
constexpr size_t MaxBytes = 64 * 1024 * 1024;
struct JavaError {};
void check(JNIEnv* e) { if (e->ExceptionCheck()) throw JavaError{}; }
void fail(JNIEnv* e, const std::exception& ex) {
    if (!e->ExceptionCheck()) e->ThrowNew(e->FindClass("java/io/IOException"), ex.what());
}
std::string bytes(JNIEnv* e, jbyteArray a) {
    if (!a) return {};
    size_t size = e->GetArrayLength(a);
    if (size > MaxBytes) throw std::runtime_error("Record exceeds 64 MiB");
    std::string s(size, '\0');
    e->GetByteArrayRegion(a, 0, size, reinterpret_cast<jbyte*>(s.data()));
    check(e);
    return s;
}
jbyteArray array(JNIEnv* e, std::string_view s) {
    auto a = e->NewByteArray(s.size());
    check(e);
    e->SetByteArrayRegion(a, 0, s.size(), reinterpret_cast<const jbyte*>(s.data()));
    check(e);
    return a;
}
std::string utf8(JNIEnv* e, jstring s) {
    if (!s) return {};
    auto a = e->GetStringChars(s, nullptr);
    check(e);
    std::string r;
    for (jsize i = 0, n = e->GetStringLength(s); i < n; ++i) {
        uint32_t c = a[i];
        if (c >= 0xd800 && c <= 0xdbff && i + 1 < n && a[i + 1] >= 0xdc00 && a[i + 1] <= 0xdfff)
            c = 0x10000 + ((c - 0xd800) << 10) + (a[++i] - 0xdc00);
        if (c < 0x80) r += char(c);
        else if (c < 0x800) { r += char(0xc0 | c >> 6); r += char(0x80 | (c & 63)); }
        else if (c < 0x10000) { r += char(0xe0 | c >> 12); r += char(0x80 | ((c >> 6) & 63)); r += char(0x80 | (c & 63)); }
        else { r += char(0xf0 | c >> 18); r += char(0x80 | ((c >> 12) & 63)); r += char(0x80 | ((c >> 6) & 63)); r += char(0x80 | (c & 63)); }
    }
    e->ReleaseStringChars(s, a);
    return r;
}
jstring string(JNIEnv* e, std::string_view s) {
    auto a = array(e, s);
    auto cls = e->FindClass("java/lang/String");
    auto charset = e->NewStringUTF("UTF-8");
    auto result = static_cast<jstring>(e->NewObject(cls, e->GetMethodID(cls, "<init>", "([BLjava/lang/String;)V"), a, charset));
    e->DeleteLocalRef(a); e->DeleteLocalRef(cls); e->DeleteLocalRef(charset);
    check(e);
    return result;
}
struct Guard {
    std::string_view data;
    size_t p = 0, nodes = 0;
    bool little = true;
    void skip(size_t n) {
        if (p > data.size() || n > data.size() - p) throw std::runtime_error("Truncated NBT");
        p += n;
    }
    uint64_t number(int n) {
        size_t start = p; skip(n); uint64_t v = 0;
        for (int i = 0; i < n; ++i) v |= uint64_t(uint8_t(data[start + i])) << (8 * (little ? i : n - 1 - i));
        return v;
    }
    void name() { skip(number(2)); }
    void payload(int type, int depth = 0) {
        if (depth > 128 || ++nodes > 2000000) throw std::runtime_error("NBT nesting or node limit exceeded");
        switch (type) {
            case 1: skip(1); break;
            case 2: skip(2); break;
            case 3: case 5: skip(4); break;
            case 4: case 6: skip(8); break;
            case 8: name(); break;
            case 7: case 11: case 12: {
                uint64_t count = number(4), width = type == 7 ? 1 : type == 11 ? 4 : 8;
                if (count > MaxBytes / width) throw std::runtime_error("NBT array is too large");
                skip(count * width); break;
            }
            case 9: {
                int subtype = number(1); uint64_t n = number(4);
                if (subtype > 12 || n > 2000000 || (subtype == 0 && n)) throw std::runtime_error("Invalid NBT list");
                for (uint64_t i = 0; i < n; ++i) payload(subtype, depth + 1);
                break;
            }
            case 10: {
                std::set<std::string> names;
                for (;;) {
                    int t = number(1); if (!t) break;
                    size_t size = number(2), pos = p; skip(size);
                    if (!names.emplace(data.substr(pos, size)).second) throw std::runtime_error("Duplicate compound tag name");
                    payload(t, depth + 1);
                }
                break;
            }
            default: throw std::runtime_error("Invalid NBT tag type");
        }
    }
    size_t root() { int t = number(1); if (!t || t > 12) throw std::runtime_error("Invalid NBT root"); name(); payload(t); return p; }
};
struct Bridge {
    JNIEnv* e;
    jclass node, list, map;
    jmethodID ctor, getType, getName, getValue, getListType, listCtor, listAdd, listSize, listGet, mapCtor, mapPut;
    explicit Bridge(JNIEnv* env) : e(env) {
        node=e->FindClass("org/levimc/launcher/core/content/nbt/NbtTag");
        list=e->FindClass("java/util/ArrayList"); map=e->FindClass("java/util/LinkedHashMap");
        ctor=e->GetMethodID(node,"<init>","(BLjava/lang/String;Ljava/lang/Object;B)V");
        getType=e->GetMethodID(node,"getType","()B"); getName=e->GetMethodID(node,"getName","()Ljava/lang/String;");
        getValue=e->GetMethodID(node,"getValue","()Ljava/lang/Object;"); getListType=e->GetMethodID(node,"getListType","()B");
        listCtor=e->GetMethodID(list,"<init>","()V"); listAdd=e->GetMethodID(list,"add","(Ljava/lang/Object;)Z");
        auto li=e->FindClass("java/util/List");
        listSize=e->GetMethodID(li,"size","()I"); listGet=e->GetMethodID(li,"get","(I)Ljava/lang/Object;"); e->DeleteLocalRef(li);
        mapCtor=e->GetMethodID(map,"<init>","()V"); mapPut=e->GetMethodID(map,"put","(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
        check(e);
    }
    ~Bridge() { e->DeleteLocalRef(node); e->DeleteLocalRef(list); e->DeleteLocalRef(map); }
    jobject box(const char* cls, const char* sig, jvalue value) {
        auto c=e->FindClass(cls); std::string signature="("; signature+=sig; signature+=")L"; signature+=cls; signature+=";";
        auto obj=e->CallStaticObjectMethodA(c,e->GetStaticMethodID(c,"valueOf",signature.c_str()),&value);
        e->DeleteLocalRef(c); check(e); return obj;
    }
    jobject to(const nbt::Tag& t, std::string_view name = {}, int depth = 0) {
        if (depth > 128) throw std::runtime_error("NBT nesting limit exceeded");
        jobject value=nullptr; jbyte subtype=0; jvalue n{};
        switch (t.getType()) {
            case nbt::Tag::Type::Byte: n.b=t.as<nbt::ByteTag>().mStorage; value=box("java/lang/Byte","B",n); break;
            case nbt::Tag::Type::Short: n.s=t.as<nbt::ShortTag>().mStorage; value=box("java/lang/Short","S",n); break;
            case nbt::Tag::Type::Int: n.i=t.as<nbt::IntTag>().mStorage; value=box("java/lang/Integer","I",n); break;
            case nbt::Tag::Type::Long: n.j=t.as<nbt::LongTag>().mStorage; value=box("java/lang/Long","J",n); break;
            case nbt::Tag::Type::Float: n.f=t.as<nbt::FloatTag>().mStorage; value=box("java/lang/Float","F",n); break;
            case nbt::Tag::Type::Double: n.d=t.as<nbt::DoubleTag>().mStorage; value=box("java/lang/Double","D",n); break;
            case nbt::Tag::Type::String: value=string(e,t.as<nbt::StringTag>().mStorage); break;
            case nbt::Tag::Type::ByteArray: {
                auto& v=t.as<nbt::ByteArrayTag>().mStorage;
                value=array(e,std::string_view(reinterpret_cast<const char*>(v.data()),v.size())); break;
            }
            case nbt::Tag::Type::IntArray: {
                auto& v=t.as<nbt::IntArrayTag>().mStorage; auto a=e->NewIntArray(v.size()); check(e);
                e->SetIntArrayRegion(a,0,v.size(),reinterpret_cast<const jint*>(v.data())); value=a; break;
            }
            case nbt::Tag::Type::LongArray: {
                auto& v=t.as<nbt::LongArrayTag>().mStorage; auto a=e->NewLongArray(v.size()); check(e);
                e->SetLongArrayRegion(a,0,v.size(),reinterpret_cast<const jlong*>(v.data())); value=a; break;
            }
            case nbt::Tag::Type::List: {
                value=e->NewObject(list,listCtor); subtype=jbyte(t.as<nbt::ListTag>().getElementType());
                for (auto& child:t.as<nbt::ListTag>()) { auto v=to(*child,{},depth+1); e->CallBooleanMethod(value,listAdd,v); e->DeleteLocalRef(v); check(e); }
                break;
            }
            case nbt::Tag::Type::Compound: {
                value=e->NewObject(map,mapCtor);
                for (auto& [key,child]:t.as<nbt::CompoundTag>()) {
                    auto k=string(e,key); auto v=to(*child,key,depth+1); auto old=e->CallObjectMethod(value,mapPut,k,v);
                    e->DeleteLocalRef(k); e->DeleteLocalRef(v); if(old)e->DeleteLocalRef(old); check(e);
                }
                break;
            }
            default: break;
        }
        auto label=string(e,name); auto result=e->NewObject(node,ctor,jbyte(t.getType()),label,value,subtype);
        e->DeleteLocalRef(label); if(value)e->DeleteLocalRef(value); check(e); return result;
    }
    std::unique_ptr<nbt::Tag> from(jobject obj, int depth = 0) {
        if (!obj || depth > 128) throw std::runtime_error("Invalid NBT tree");
        auto type=nbt::Tag::Type(e->CallByteMethod(obj,getType));
        auto value=e->CallObjectMethod(obj,getValue); check(e);
        auto t=nbt::Tag::newTag(type); if (!t) throw std::runtime_error("Invalid tag type");
        switch (type) {
            case nbt::Tag::Type::Byte: case nbt::Tag::Type::Short: case nbt::Tag::Type::Int:
            case nbt::Tag::Type::Long: case nbt::Tag::Type::Float: case nbt::Tag::Type::Double: {
                auto c=e->FindClass("java/lang/Number");
                if (!value || !e->IsInstanceOf(value,c)) { e->DeleteLocalRef(c); throw std::runtime_error("Numeric tag requires a number"); }
                if(type==nbt::Tag::Type::Byte)t->as<nbt::ByteTag>().mStorage=e->CallByteMethod(value,e->GetMethodID(c,"byteValue","()B"));
                if(type==nbt::Tag::Type::Short)t->as<nbt::ShortTag>().mStorage=e->CallShortMethod(value,e->GetMethodID(c,"shortValue","()S"));
                if(type==nbt::Tag::Type::Int)t->as<nbt::IntTag>().mStorage=e->CallIntMethod(value,e->GetMethodID(c,"intValue","()I"));
                if(type==nbt::Tag::Type::Long)t->as<nbt::LongTag>().mStorage=e->CallLongMethod(value,e->GetMethodID(c,"longValue","()J"));
                if(type==nbt::Tag::Type::Float)t->as<nbt::FloatTag>().mStorage=e->CallFloatMethod(value,e->GetMethodID(c,"floatValue","()F"));
                if(type==nbt::Tag::Type::Double)t->as<nbt::DoubleTag>().mStorage=e->CallDoubleMethod(value,e->GetMethodID(c,"doubleValue","()D"));
                e->DeleteLocalRef(c); break;
            }
            case nbt::Tag::Type::String: { auto text=utf8(e,static_cast<jstring>(value)); if(text.size()>65535)throw std::runtime_error("NBT string exceeds 65535 UTF-8 bytes"); t->as<nbt::StringTag>().mStorage=std::move(text); break; }
            case nbt::Tag::Type::ByteArray: {
                auto v=bytes(e,static_cast<jbyteArray>(value)); t->as<nbt::ByteArrayTag>().mStorage.assign(v.begin(),v.end()); break;
            }
            case nbt::Tag::Type::IntArray: {
                auto a=static_cast<jintArray>(value); auto& v=t->as<nbt::IntArrayTag>().mStorage; v.resize(e->GetArrayLength(a));
                e->GetIntArrayRegion(a,0,v.size(),reinterpret_cast<jint*>(v.data())); break;
            }
            case nbt::Tag::Type::LongArray: {
                auto a=static_cast<jlongArray>(value); auto& v=t->as<nbt::LongArrayTag>().mStorage; v.resize(e->GetArrayLength(a));
                e->GetLongArrayRegion(a,0,v.size(),reinterpret_cast<jlong*>(v.data())); break;
            }
            case nbt::Tag::Type::List: {
                auto& v=t->as<nbt::ListTag>(); v.mType=nbt::Tag::Type(e->CallByteMethod(obj,getListType));
                jint count=e->CallIntMethod(value,listSize);
                if(count>2000000)throw std::runtime_error("NBT list is too large");
                for(jint i=0;i<count;++i) {
                    auto child=e->CallObjectMethod(value,listGet,i); check(e); auto parsed=from(child,depth+1); e->DeleteLocalRef(child);
                    if(parsed->getType()!=v.mType)throw std::runtime_error("NBT list types must match");
                    v.push_back(std::move(parsed));
                }
                break;
            }
            case nbt::Tag::Type::Compound: {
                auto c=e->FindClass("java/util/Map"); auto collection=e->FindClass("java/util/Collection");
                auto values=e->CallObjectMethod(value,e->GetMethodID(c,"values","()Ljava/util/Collection;"));
                auto all=static_cast<jobjectArray>(e->CallObjectMethod(values,e->GetMethodID(collection,"toArray","()[Ljava/lang/Object;"))); check(e);
                for(jsize i=0,n=e->GetArrayLength(all);i<n;++i) {
                    auto child=e->GetObjectArrayElement(all,i); auto name=static_cast<jstring>(e->CallObjectMethod(child,getName)); check(e);
                    auto key=utf8(e,name); if(key.size()>65535)throw std::runtime_error("NBT name exceeds 65535 UTF-8 bytes");
                    if(t->as<nbt::CompoundTag>().contains(key))throw std::runtime_error("Duplicate tag name");
                    t->as<nbt::CompoundTag>().put(key,from(child,depth+1)); e->DeleteLocalRef(name); e->DeleteLocalRef(child);
                }
                e->DeleteLocalRef(c); e->DeleteLocalRef(collection); e->DeleteLocalRef(values); e->DeleteLocalRef(all); break;
            }
            default: break;
        }
        if(value)e->DeleteLocalRef(value); check(e); return t;
    }
};
std::string inflateData(std::string_view data) {
    z_stream z{};
    if(inflateInit2(&z,15+32)!=Z_OK)throw std::runtime_error("Cannot open compressed NBT");
    z.next_in=reinterpret_cast<Bytef*>(const_cast<char*>(data.data())); z.avail_in=data.size();
    std::string out; char buffer[16384]; int code;
    do {
        z.next_out=reinterpret_cast<Bytef*>(buffer); z.avail_out=sizeof(buffer);
        code=inflate(&z,Z_NO_FLUSH); out.append(buffer,sizeof(buffer)-z.avail_out);
        if(out.size()>MaxBytes){inflateEnd(&z);throw std::runtime_error("Uncompressed NBT exceeds 64 MiB");}
    }while(code==Z_OK);
    inflateEnd(&z); if(code!=Z_STREAM_END)throw std::runtime_error("Corrupt compressed NBT"); return out;
}
struct Database {
    std::unique_ptr<leveldb::Cache> cache;
    std::unique_ptr<leveldb::DB> db;
};
std::mutex dbMutex;
std::map<jlong,std::unique_ptr<Database>> databases;
jlong nextHandle=1;
Database& database(jlong id) {
    auto i=databases.find(id); if(i==databases.end())throw std::runtime_error("World database is closed"); return *i->second;
}
void status(const leveldb::Status& s) { if(!s.ok())throw std::runtime_error(s.ToString()); }
}

extern "C" JNIEXPORT jobject JNICALL
Java_org_levimc_launcher_core_content_nbt_NativeNbt_decode(JNIEnv* e,jclass,jbyteArray input,jint offset,jint format) {
    try {
        auto data=bytes(e,input); if(offset<0||size_t(offset)>=data.size())throw std::runtime_error("Invalid NBT offset");
        bool compressed=offset==0&&data.size()>2&&uint8_t(data[0])==0x1f&&uint8_t(data[1])==0x8b;
        if(compressed)data=inflateData(data);
        std::string_view content(data); content.remove_prefix(offset); int version=0; size_t prefix=0;
        if(format==1) {
            Guard h{content}; version=h.number(4); uint64_t length=h.number(4);
            if(length!=content.size()-8)throw std::runtime_error("level.dat header length does not match its payload");
            content.remove_prefix(8); prefix=8;
        }
        Guard guard{content}; guard.little=format!=2; size_t used=guard.root();
        nbt::io::BytesDataInput stream(content.substr(0,used),false,format!=2);
        auto type=nbt::Tag::Type(stream.getByte()); auto name=stream.getString();
        auto tag=nbt::Tag::newTag(type); tag->load(stream); Bridge b(e); auto root=b.to(*tag,name);
        auto cls=e->FindClass("org/levimc/launcher/core/content/nbt/NbtDocument");
        auto result=e->NewObject(cls,e->GetMethodID(cls,"<init>","(Lorg/levimc/launcher/core/content/nbt/NbtTag;IIIZ)V"),root,version,jint(used+prefix),format,jboolean(compressed));
        e->DeleteLocalRef(cls); e->DeleteLocalRef(root); check(e); return result;
    }catch(const JavaError&){}catch(const std::exception& ex){fail(e,ex);} return nullptr;
}
extern "C" JNIEXPORT jbyteArray JNICALL
Java_org_levimc_launcher_core_content_nbt_NativeNbt_encode(JNIEnv* e,jclass,jobject root,jint version,jint format,jboolean compressed) {
    try {
        Bridge b(e); auto tag=b.from(root); auto n=static_cast<jstring>(e->CallObjectMethod(root,b.getName)); check(e); auto name=utf8(e,n); e->DeleteLocalRef(n);
        if(name.size()>65535)throw std::runtime_error("NBT name is too long");
        nbt::io::BytesDataOutput stream(format!=2); stream.writeByte(uint8_t(tag->getType())); stream.writeString(name); tag->write(stream);
        std::string result=stream.getAndReleaseData();
        if(result.size()>MaxBytes)throw std::runtime_error("NBT exceeds 64 MiB");
        Guard g{result};g.little=format!=2;if(g.root()!=result.size())throw std::runtime_error("Encoded NBT has trailing data");
        if(format==1) {
            nbt::io::BytesDataOutput output; output.writeInt(version); output.writeLongString(result); result=output.getAndReleaseData();
        }
        if(compressed)result=nbt::detail::compress(result,Z_DEFAULT_COMPRESSION,15+16);
        return array(e,result);
    }catch(const JavaError&){}catch(const std::exception& ex){fail(e,ex);} return nullptr;
}
extern "C" JNIEXPORT jstring JNICALL
Java_org_levimc_launcher_core_content_nbt_NativeNbt_toSnbt(JNIEnv* e,jclass,jobject node) {
    try { Bridge b(e); return string(e,b.from(node)->toSnbt()); }catch(const JavaError&){}catch(const std::exception& ex){fail(e,ex);} return nullptr;
}
extern "C" JNIEXPORT jobject JNICALL
Java_org_levimc_launcher_core_content_nbt_NativeNbt_fromSnbt(JNIEnv* e,jclass,jstring input) {
    try {
        auto s=utf8(e,input); if(s.size()>MaxBytes)throw std::runtime_error("SNBT exceeds 64 MiB");
        int depth=0; bool quoted=false,escape=false; char quote=0;
        for(char c:s) {
            if(escape){escape=false;continue;} if(quoted&&c=='\\'){escape=true;continue;}
            if(quoted){if(c==quote)quoted=false;continue;} if(c=='\''||c=='"'){quoted=true;quote=c;continue;}
            if(c=='{'||c=='['){if(++depth>128)throw std::runtime_error("SNBT nesting exceeds 128");}else if(c=='}'||c==']')--depth;
        }
        std::string_view remaining=s;auto parsed=nbt::detail::parseSnbtValue(remaining,false);
        if(!parsed||std::any_of(remaining.begin(),remaining.end(),[](unsigned char c){return !std::isspace(c);}))throw std::runtime_error("Invalid SNBT or trailing text"); Bridge b(e);return b.to(**parsed);
    }catch(const JavaError&){}catch(const std::exception& ex){fail(e,ex);} return nullptr;
}
extern "C" JNIEXPORT jlong JNICALL
Java_org_levimc_launcher_core_content_leveldb_WorldDatabase_openNative(JNIEnv* e,jclass,jstring path) {
    try {
        auto d=std::make_unique<Database>();d->cache.reset(leveldb::NewLRUCache(8*1024*1024));
        leveldb::Options options;options.create_if_missing=false;options.paranoid_checks=true;options.block_cache=d->cache.get();
        options.write_buffer_size=2*1024*1024;options.max_open_files=48;options.reuse_logs=true;options.compression=leveldb::kZlibRawCompression;
        leveldb::DB* raw=nullptr;status(leveldb::DB::Open(options,utf8(e,path),&raw));d->db.reset(raw);
        std::lock_guard lock(dbMutex);auto id=nextHandle++;databases.emplace(id,std::move(d));return id;
    }catch(const JavaError&){}catch(const std::exception& ex){fail(e,ex);} return 0;
}
extern "C" JNIEXPORT void JNICALL
Java_org_levimc_launcher_core_content_leveldb_WorldDatabase_closeNative(JNIEnv*,jclass,jlong handle) { std::lock_guard lock(dbMutex);databases.erase(handle); }
extern "C" JNIEXPORT jbyteArray JNICALL
Java_org_levimc_launcher_core_content_leveldb_WorldDatabase_getNative(JNIEnv* e,jclass,jlong handle,jbyteArray key) {
    try {
        auto k=bytes(e,key);std::lock_guard lock(dbMutex);auto& d=database(handle);std::string value;
        leveldb::ReadOptions r;r.verify_checksums=true;r.fill_cache=true;auto s=d.db->Get(r,k,&value);
        if(s.IsNotFound())return nullptr;status(s);if(value.size()>MaxBytes)throw std::runtime_error("Record exceeds 64 MiB");return array(e,value);
    }catch(const JavaError&){}catch(const std::exception& ex){fail(e,ex);} return nullptr;
}
extern "C" JNIEXPORT jobjectArray JNICALL
Java_org_levimc_launcher_core_content_leveldb_WorldDatabase_scanNative(JNIEnv* e,jclass,jlong handle,jbyteArray prefix,jbyteArray after,jint limit,jstring contains) {
    try {
        auto p=bytes(e,prefix),a=bytes(e,after),filter=utf8(e,contains);limit=std::clamp(limit,1,512);
        std::lock_guard lock(dbMutex);auto& d=database(handle);leveldb::ReadOptions r;r.verify_checksums=true;r.fill_cache=false;
        std::unique_ptr<leveldb::Iterator> it(d.db->NewIterator(r));it->Seek(a.empty()?p:a);if(!a.empty()&&it->Valid()&&it->key()==leveldb::Slice(a))it->Next();
        std::vector<std::string> keys;std::string last;int scanned=0;
        while(it->Valid()&&it->key().starts_with(p)&&keys.size()<size_t(limit)&&scanned++<8192) {
            last=it->key().ToString();if(filter.empty()||std::search(last.begin(),last.end(),filter.begin(),filter.end(),[](unsigned char a,unsigned char b){return std::tolower(a)==std::tolower(b);})!=last.end())keys.push_back(last);it->Next();
        }
        status(it->status());bool more=it->Valid()&&it->key().starts_with(p);
        auto cls=e->FindClass("[B");auto result=e->NewObjectArray(keys.size()+1,cls,nullptr);e->DeleteLocalRef(cls);check(e);
        if(more){auto b=array(e,last);e->SetObjectArrayElement(result,0,b);e->DeleteLocalRef(b);}
        for(size_t i=0;i<keys.size();++i){auto b=array(e,keys[i]);e->SetObjectArrayElement(result,i+1,b);e->DeleteLocalRef(b);}
        return result;
    }catch(const JavaError&){}catch(const std::exception& ex){fail(e,ex);} return nullptr;
}
extern "C" JNIEXPORT void JNICALL
Java_org_levimc_launcher_core_content_leveldb_WorldDatabase_writeNative(JNIEnv* e,jclass,jlong handle,jobjectArray keys,jobjectArray values) {
    try {
        if(e->GetArrayLength(keys)!=e->GetArrayLength(values))throw std::runtime_error("Invalid database batch");
        leveldb::WriteBatch batch;size_t total=0;
        for(jsize i=0,n=e->GetArrayLength(keys);i<n;++i) {
            auto k=static_cast<jbyteArray>(e->GetObjectArrayElement(keys,i));auto v=static_cast<jbyteArray>(e->GetObjectArrayElement(values,i));
            auto key=bytes(e,k);auto val=bytes(e,v);total+=key.size()+val.size();if(total>128*1024*1024)throw std::runtime_error("Edit batch exceeds 128 MiB");
            if(v)batch.Put(key,val);else batch.Delete(key);e->DeleteLocalRef(k);if(v)e->DeleteLocalRef(v);
        }
        std::lock_guard lock(dbMutex);leveldb::WriteOptions w;w.sync=true;status(database(handle).db->Write(w,&batch));
    }catch(const JavaError&){}catch(const std::exception& ex){fail(e,ex);}
}
