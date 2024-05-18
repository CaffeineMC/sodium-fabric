package net.caffeinemc.mods.sodium.client.platform.windows.api.d3dkmt;

// https://learn.microsoft.com/en-us/windows-hardware/drivers/ddi/d3dkmthk/ne-d3dkmthk-_kmtqueryadapterinfotype
public class D3DKMTQueryAdapterInfoType {
    // Windows Vista
    public static class WDDM10 {
        public static final int KMTQAITYPE_UMDRIVERPRIVATE         =  0;
        public static final int KMTQAITYPE_UMDRIVERNAME            =  1;
        public static final int KMTQAITYPE_UMOPENGLINFO            =  2;
        public static final int KMTQAITYPE_GETSEGMENTSIZE          =  3;
        public static final int KMTQAITYPE_ADAPTERGUID             =  4;
        public static final int KMTQAITYPE_FLIPQUEUEINFO           =  5;
        public static final int KMTQAITYPE_ADAPTERADDRESS          =  6;
        public static final int KMTQAITYPE_SETWORKINGSETINFO       =  7;
        public static final int KMTQAITYPE_ADAPTERREGISTRYINFO     =  8;
        public static final int KMTQAITYPE_CURRENTDISPLAYMODE      =  9;
        public static final int KMTQAITYPE_MODELIST                = 10;
        public static final int KMTQAITYPE_CHECKDRIVERUPDATESTATUS = 11;
        public static final int KMTQAITYPE_VIRTUALADDRESSINFO      = 12;
        public static final int KMTQAITYPE_DRIVERVERSION           = 13;
    }

    // Windows 7
    public static class WDDM11 extends WDDM10 {

    }

    // Windows 8
    public static class WDDM12 extends WDDM11 {
        public static final int KMTQAITYPE_ADAPTERTYPE             = 15;
        public static final int KMTQAITYPE_OUTPUTDUPLCONTEXTSCOUNT = 16;
        public static final int KMTQAITYPE_WDDM_1_2_CAPS           = 17;
        public static final int KMTQAITYPE_UMD_DRIVER_VERSION      = 18;
        public static final int KMTQAITYPE_DIRECTFLIP_SUPPORT      = 19;
    }

    // Windows 8.1
    public static class WDDM13 extends WDDM12 {
        public static final int KMTQAITYPE_MULTIPLANEOVERLAY_SUPPORT = 20;
        public static final int KMTQAITYPE_DLIST_DRIVER_NAME       = 21;
        public static final int KMTQAITYPE_WDDM_1_3_CAPS           = 22;
        public static final int KMTQAITYPE_MULTIPLANEOVERLAY_HUD_SUPPORT = 23;
    }

    // Windows 10 Version 1507
    public static class WDDM20 extends WDDM13 {
        public static final int KMTQAITYPE_WDDM_2_0_CAPS           = 24;
        public static final int KMTQAITYPE_NODEMETADATA            = 25;
        public static final int KMTQAITYPE_CPDRIVERNAME            = 26;
        public static final int KMTQAITYPE_XBOX                    = 27;
        public static final int KMTQAITYPE_INDEPENDENTFLIP_SUPPORT = 28;
        public static final int KMTQAITYPE_MIRACASTCOMPANIONDRIVERNAME = 29;
        public static final int KMTQAITYPE_PHYSICALADAPTERCOUNT    = 30;
        public static final int KMTQAITYPE_PHYSICALADAPTERDEVICEIDS = 31;
        public static final int KMTQAITYPE_DRIVERCAPS_EXT          = 32;
        public static final int KMTQAITYPE_QUERY_MIRACAST_DRIVER_TYPE = 33;
        public static final int KMTQAITYPE_QUERY_GPUMMU_CAPS       = 34;
        public static final int KMTQAITYPE_QUERY_MULTIPLANEOVERLAY_DECODE_SUPPORT = 35;
        public static final int KMTQAITYPE_QUERY_HW_PROTECTION_TEARDOWN_COUNT = 36;
        public static final int KMTQAITYPE_QUERY_ISBADDRIVERFORHWPROTECTIONDISABLED = 37;
        public static final int KMTQAITYPE_MULTIPLANEOVERLAY_SECONDARY_SUPPORT = 38;
        public static final int KMTQAITYPE_INDEPENDENTFLIP_SECONDARY_SUPPORT = 39;
    }

    // Windows 10 Version 1607
    public static class WDDM21 extends WDDM20 {
        public static final int KMTQAITYPE_PANELFITTER_SUPPORT     = 40;
    }

    // Windows 10 Version 1703
    public static class WDDM22 extends WDDM21 {
        public static final int KMTQAITYPE_PHYSICALADAPTERPNPKEY   = 41;
        public static final int KMTQAITYPE_GETSEGMENTGROUPSIZE     = 42;
        public static final int KMTQAITYPE_MPO3DDI_SUPPORT         = 43;
        public static final int KMTQAITYPE_HWDRM_SUPPORT           = 44;
        public static final int KMTQAITYPE_MPOKERNELCAPS_SUPPORT   = 45;
        public static final int KMTQAITYPE_MULTIPLANEOVERLAY_STRETCH_SUPPORT = 46;
        public static final int KMTQAITYPE_GET_DEVICE_VIDPN_OWNERSHIP_INFO = 47;
    }

    // Windows 10 Version 1709
    public static class WDDM23 extends WDDM22 {

    }

    // Windows 10 Version 1803
    public static class WDDM24 extends WDDM23 {
        public static final int KMTQAITYPE_QUERYREGISTRY                    = 48;
        public static final int KMTQAITYPE_KMD_DRIVER_VERSION               = 49;
        public static final int KMTQAITYPE_BLOCKLIST_KERNEL                 = 50;
        public static final int KMTQAITYPE_BLOCKLIST_RUNTIME                = 51;
        public static final int KMTQAITYPE_ADAPTERGUID_RENDER               = 52;
        public static final int KMTQAITYPE_ADAPTERADDRESS_RENDER            = 53;
        public static final int KMTQAITYPE_ADAPTERREGISTRYINFO_RENDER       = 54;
        public static final int KMTQAITYPE_CHECKDRIVERUPDATESTATUS_RENDER   = 55;
        public static final int KMTQAITYPE_DRIVERVERSION_RENDER             = 56;
        public static final int KMTQAITYPE_ADAPTERTYPE_RENDER               = 57;
        public static final int KMTQAITYPE_WDDM_1_2_CAPS_RENDER             = 58;
        public static final int KMTQAITYPE_WDDM_1_3_CAPS_RENDER             = 59;
        public static final int KMTQAITYPE_QUERY_ADAPTER_UNIQUE_GUID        = 60;
        public static final int KMTQAITYPE_NODEPERFDATA                     = 61;
        public static final int KMTQAITYPE_ADAPTERPERFDATA                  = 62;
        public static final int KMTQAITYPE_ADAPTERPERFDATA_CAPS             = 63;
        public static final int KMTQUITYPE_GPUVERSION                       = 64;
    }

    // Windows 10 Version 1903
    public static class WDDM26 extends WDDM24 {
        public static final int KMTQAITYPE_DRIVER_DESCRIPTION               = 65;
        public static final int KMTQAITYPE_DRIVER_DESCRIPTION_RENDER        = 66;
        public static final int KMTQAITYPE_SCANOUT_CAPS                     = 67;
        public static final int KMTQAITYPE_DISPLAY_UMDRIVERNAME             = 71; // Added in 19H2
        public static final int KMTQAITYPE_PARAVIRTUALIZATION_RENDER        = 68;
    }

    // Windows 10 Version 2004
    public static class WDDM27 extends WDDM26 {
        public static final int KMTQAITYPE_SERVICENAME                      = 69;
        public static final int KMTQAITYPE_WDDM_2_7_CAPS                    = 70;
        public static final int KMTQAITYPE_TRACKEDWORKLOAD_SUPPORT          = 72;
    }
}
