package net.caffeinemc.mods.sodium.client.compatibility.environment.probe;

import com.google.common.base.Charsets;
import net.caffeinemc.mods.sodium.client.util.OsUtils;
import org.lwjgl.system.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.SystemInfo;
import oshi.util.ExecutingCommand;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.lwjgl.system.APIUtil.apiGetFunctionAddress;

public class GraphicsAdapterProbe {
    private static final Logger LOGGER = LoggerFactory.getLogger("Sodium-GraphicsAdapterProbe");

    private static List<GraphicsAdapterInfo> ADAPTERS;

    public static void findAdapters() {
        LOGGER.info("Searching for graphics cards...");

        // We rely on separate detection logic for Linux because Oshi fails to find GPUs without
        // display outputs, and we can also retrieve the driver version for NVIDIA GPUs this way.
        var results = switch (OsUtils.getOs()) {
            case LINUX -> findAdaptersLinux();
            case WIN -> findAdaptersWindowsD3DKMT();
            default -> findAdaptersCrossPlatform();
        };

        if (results.isEmpty()) {
            LOGGER.warn("No graphics cards were found. Either you have no hardware devices supporting 3D acceleration, or " +
                    "something has gone terribly wrong!");
        }

        ADAPTERS = results;
    }

    public static List<GraphicsAdapterInfo> findAdaptersCrossPlatform() {
        var systemInfo = new SystemInfo();
        var hardwareInfo = systemInfo.getHardware();

        var results = new ArrayList<GraphicsAdapterInfo>();

        for (var graphicsCard : hardwareInfo.getGraphicsCards()) {
            GraphicsAdapterVendor vendor = GraphicsAdapterVendor.identifyVendorFromString(graphicsCard.getVendor());
            String name = graphicsCard.getName();
            String versionInfo = graphicsCard.getVersionInfo();

            var info = new GraphicsAdapterInfo(vendor, name, versionInfo);
            results.add(info);

            LOGGER.info("Found graphics card: {}", info);
        }

        return results;
    }


    private static void queryAdapterInfo(long fptr, int handle, int type, ByteBuffer result) {
        try (var stack = MemoryStack.stackPush()) {
            var D3DKMT_QUERYADAPTERINFO = stack.calloc(0x18).order(ByteOrder.nativeOrder());
            D3DKMT_QUERYADAPTERINFO.putInt(0, handle);
            D3DKMT_QUERYADAPTERINFO.putInt(4, type);
            D3DKMT_QUERYADAPTERINFO.putLong(8, MemoryUtil.memAddress(result));
            D3DKMT_QUERYADAPTERINFO.putInt(16, result.remaining());

            int retStatus = JNI.callPI(MemoryUtil.memAddress0(D3DKMT_QUERYADAPTERINFO), fptr);
            if (retStatus != 0) {
                throw new RuntimeException("D3DKMTQueryAdapterInfo status code: " + retStatus);
            }
        }
    }

    public static List<GraphicsAdapterInfo> findAdaptersWindowsD3DKMT() {
        //(Its ok to leave open as it is only used once and used by other things)
        SharedLibrary gdi32 = Library.loadNative(GraphicsAdapterProbe.class, "org.lwjgl", "gdi32");
        long D3DKMTQueryAdapterInfo = apiGetFunctionAddress(gdi32, "D3DKMTQueryAdapterInfo");
        long D3DKMTCloseAdapter     = apiGetFunctionAddress(gdi32, "D3DKMTCloseAdapter");
        long D3DKMTEnumAdapters2    = apiGetFunctionAddress(gdi32, "D3DKMTEnumAdapters2");
        if (D3DKMTQueryAdapterInfo == 0 || D3DKMTCloseAdapter == 0 || D3DKMTEnumAdapters2 == 0) {
            //Could not load D3DKMTEnumAdapters2 meaning < windows 8, fallback to cross-platform
            LOGGER.warn("Unable to find D3DKMTEnumAdapters2. running windows 7 or earlier, using fallback probe");
            return findAdaptersCrossPlatform();
        }

        List<GraphicsAdapterInfo> adapterList = new ArrayList<>();
        try (var stack = MemoryStack.stackPush()) {
            //D3DKMT_ENUMADAPTERS2
            // 4 bytes - uint - NumAdapters
            // 4 bytes - pad/alignment
            // 8 bytes - pointer - D3DKMT_ADAPTERINFO *pAdapters
            var D3DKMT_ENUMADAPTERS2 = stack.calloc(16).order(ByteOrder.nativeOrder());

            int retStatus = JNI.callPI(MemoryUtil.memAddress0(D3DKMT_ENUMADAPTERS2), D3DKMTEnumAdapters2);
            if (retStatus != 0) {
                throw new RuntimeException("D3DKMTEnumAdapters2 status code: " + retStatus);
            }
            int adapters  = D3DKMT_ENUMADAPTERS2.getInt(0);
            var D3DKMT_ADAPTERINFO_array = stack.calloc(8, adapters * 0x14).order(ByteOrder.nativeOrder());
            D3DKMT_ENUMADAPTERS2.putLong(8, MemoryUtil.memAddress(D3DKMT_ADAPTERINFO_array));
            retStatus = JNI.callPI(MemoryUtil.memAddress0(D3DKMT_ENUMADAPTERS2), D3DKMTEnumAdapters2);
            if (retStatus != 0) {
                throw new RuntimeException("D3DKMTEnumAdapters2 status code: " + retStatus);
            }
            //Have an array of adapter info so we can query it
            adapters = D3DKMT_ENUMADAPTERS2.getInt(0);
            //System.err.println("Found " + adapters + " adapters");
            for (int i = 0; i < adapters; i++) {
                int handle = D3DKMT_ADAPTERINFO_array.getInt(0x14*i);
                try (var stack2 = MemoryStack.stackPush()) {
                    try {
                        var description = stack2.calloc(8, 4096 * 2).order(ByteOrder.nativeOrder());
                        queryAdapterInfo(D3DKMTQueryAdapterInfo, handle, 65, description);//KMTQAITYPE_DRIVER_DESCRIPTION

                        var nameB = new byte[4096 * 2];
                        description.get(0, nameB);
                        var name = new String(nameB, Charsets.UTF_16LE);
                        name = name.substring(0, name.indexOf(0));

                        var version = stack2.calloc(8, 8).order(ByteOrder.nativeOrder());
                        queryAdapterInfo(D3DKMTQueryAdapterInfo, handle, 18, version);//KMTQAITYPE_UMD_DRIVER_VERSION
                        int A = Short.toUnsignedInt(version.getShort(6));
                        int B = Short.toUnsignedInt(version.getShort(4));
                        int C = Short.toUnsignedInt(version.getShort(2));
                        int D = Short.toUnsignedInt(version.getShort(0));

                        //System.err.println("Description: " + name + " version: " + A + "." + B + "." + C + "." + D);

                        var info = new GraphicsAdapterInfo(GraphicsAdapterVendor.identifyVendorFromString(name),
                                name, A + "." + B + "." + C + "." + D);
                        adapterList.add(info);
                        LOGGER.info("Found graphics card: {}", info);
                    } finally {
                        retStatus = JNI.callPI(stack2.nint(handle), D3DKMTCloseAdapter);
                        if (retStatus != 0) {
                            throw new RuntimeException("D3DKMTCloseAdapter status code: " + retStatus);
                        }
                    }
                }
            }
        }
        return adapterList;
    }

    private static List<GraphicsAdapterInfo> findAdaptersLinux() {
        var results = new ArrayList<GraphicsAdapterInfo>();

        try (var devices = Files.list(Path.of("/sys/bus/pci/devices/"))) {
            Iterable<Path> devicesIter = devices::iterator;

            for (var devicePath : devicesIter) {
                // 0x030000 = VGA compatible controller
                // 0x030200 = 3D controller (GPUs with no inputs attached, e.g. hybrid graphics laptops)
                var deviceClass = Files.readString(devicePath.resolve("class")).trim();
                if (!deviceClass.equals("0x030000") && !deviceClass.equals("0x030200")) {
                    continue;
                }

                var deviceVendor = Files.readString(devicePath.resolve("vendor")).trim();
                GraphicsAdapterVendor vendor = GraphicsAdapterVendor.identifyVendorFromString(deviceVendor);

                // The Linux kernel doesn't provide a way to get the device name, so we need to use lspci,
                // since it comes with a list of known device names mapped to device IDs.
                var deviceId = Files.readString(devicePath.resolve("device")).trim();
                var name = ExecutingCommand // See `man lspci` for more information
                        .runNative("lspci -vmm -d " + deviceVendor.substring(2) + ":" + deviceId.substring(2))
                        .stream()
                        .filter(line -> line.startsWith("Device:"))
                        .map(line -> line.substring("Device:".length()).trim())
                        .findFirst()
                        .orElse("unknown");

                // This works for the NVIDIA driver, not for i915/amdgpu/etc. though (for obvious reasons).
                var versionInfo = "unknown";
                try {
                    versionInfo = Files.readString(devicePath.resolve("driver/module/version")).trim();
                } catch (IOException ignored) {}

                var info = new GraphicsAdapterInfo(vendor, name, versionInfo);
                results.add(info);

                LOGGER.info("Found graphics card: {}", info);
            }
        } catch (IOException ignored) {}

        return results;
    }

    public static Collection<GraphicsAdapterInfo> getAdapters() {
        if (ADAPTERS == null) {
            throw new RuntimeException("Graphics adapters not probed yet");
        }

        return ADAPTERS;
    }
}
