package com.microsoft.applications.events;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public class Constants {
    public static final String COMMONFIELDS_APP_ENV = "AppInfo.Env";
    public static final String COMMONFIELDS_APP_EXPERIMENTETAG = "AppInfo.ETag";
    public static final String COMMONFIELDS_APP_EXPERIMENTIDS = "AppInfo.ExperimentIds";
    public static final String COMMONFIELDS_APP_ID = "AppInfo.Id";
    public static final String COMMONFIELDS_APP_LANGUAGE = "AppInfo.Language";
    public static final String COMMONFIELDS_APP_NAME = "AppInfo.Name";
    public static final String COMMONFIELDS_APP_VERSION = "AppInfo.Version";
    public static final String COMMONFIELDS_COMMERCIAL_ID = "M365aInfo.EnrolledTenantId";
    public static final String COMMONFIELDS_DEVICE_CLASS = "DeviceInfo.Class";
    public static final String COMMONFIELDS_DEVICE_ID = "DeviceInfo.Id";
    public static final String COMMONFIELDS_DEVICE_MAKE = "DeviceInfo.Make";
    public static final String COMMONFIELDS_DEVICE_MODEL = "DeviceInfo.Model";
    public static final String COMMONFIELDS_EVENT_CRC32 = "EventInfo.CRC32";
    public static final String COMMONFIELDS_EVENT_INITID = "EventInfo.InitId";
    public static final String COMMONFIELDS_EVENT_LATENCY = "EventInfo.Latency";
    public static final String COMMONFIELDS_EVENT_LEVEL = "EventInfo.Level";
    public static final String COMMONFIELDS_EVENT_NAME = "EventInfo.Name";
    public static final String COMMONFIELDS_EVENT_PERSISTENCE = "EventInfo.Persistence";
    public static final String COMMONFIELDS_EVENT_POLICYFLAGS = "EventInfo.PolicyFlags";
    public static final String COMMONFIELDS_EVENT_PRIORITY = "EventInfo.Priority";
    public static final String COMMONFIELDS_EVENT_PRIVTAGS = "EventInfo.PrivTags";
    public static final String COMMONFIELDS_EVENT_SDKVERSION = "EventInfo.SdkVersion";
    public static final String COMMONFIELDS_EVENT_SEQ = "EventInfo.Sequence";
    public static final String COMMONFIELDS_EVENT_SOURCE = "EventInfo.Source";
    public static final String COMMONFIELDS_EVENT_TIME = "EventInfo.Time";
    public static final String COMMONFIELDS_IKEY = "iKey";
    public static final String COMMONFIELDS_METADATA_VIEWINGCATEGORY = "MetaData.ViewingCategory";
    public static final String COMMONFIELDS_METADATA_VIEWINGEXTRA1 = "MetaData.ViewingExtra1";
    public static final String COMMONFIELDS_METADATA_VIEWINGEXTRA2 = "MetaData.ViewingExtra2";
    public static final String COMMONFIELDS_METADATA_VIEWINGEXTRA3 = "MetaData.ViewingExtra3";
    public static final String COMMONFIELDS_METADATA_VIEWINGPAYLOADDECODERPATH = "MetaData.ViewingPayloadDecoderPath";
    public static final String COMMONFIELDS_METADATA_VIEWINGPAYLOADENCODEDFIELDNAME = "MetaData.ViewingPayloadEncodedFieldName";
    public static final String COMMONFIELDS_METADATA_VIEWINGPRODUCERID = "MetaData.ViewingProducerId";
    public static final String COMMONFIELDS_NETWORK_COST = "DeviceInfo.NetworkCost";
    public static final String COMMONFIELDS_NETWORK_PROVIDER = "DeviceInfo.NetworkProvider";
    public static final String COMMONFIELDS_NETWORK_TYPE = "DeviceInfo.NetworkType";
    public static final String COMMONFIELDS_OS_BUILD = "DeviceInfo.OsBuild";
    public static final String COMMONFIELDS_OS_NAME = "DeviceInfo.OsName";
    public static final String COMMONFIELDS_OS_VERSION = "DeviceInfo.OsVersion";
    public static final String COMMONFIELDS_PIPELINEINFO_ACCOUNT = "PipelineInfo.AccountId";
    public static final String COMMONFIELDS_USER_ADVERTISINGID = "UserInfo.AdvertisingId";
    public static final String COMMONFIELDS_USER_ANID = "UserInfo.ANID";
    public static final String COMMONFIELDS_USER_ID = "UserInfo.Id";
    public static final String COMMONFIELDS_USER_LANGUAGE = "UserInfo.Language";
    public static final String COMMONFIELDS_USER_MSAID = "UserInfo.MsaId";
    public static final String COMMONFIELDS_USER_TIMEZONE = "UserInfo.TimeZone";
    public static final String CONTEXT_SCOPE_ALL = "*";
    public static final String CONTEXT_SCOPE_EMPTY = "";
    public static final String CONTEXT_SCOPE_NONE = "-";
    public static final String EVENTRECORD_TYPE_CUSTOM_EVENT = "custom";
    public static final String SESSION_DURATION = "Session.Duration";
    public static final String SESSION_DURATION_BUCKET = "Session.DurationBucket";
    public static final String SESSION_FIRST_TIME = "Session.FirstLaunchTime";
    public static final String SESSION_ID = "Session.Id";
    public static final String SESSION_ID_LEGACY = "act_session_id";
    public static final String SESSION_IMPRESSION_ID = "Session.ImpressionId";
    public static final String SESSION_STATE = "Session.State";

    private Constants() {
    }
}
