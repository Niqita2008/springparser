package com.vprave.niqitadev.parser.storage;

import com.sun.management.OperatingSystemMXBean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.management.ManagementFactory;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.StringCharacterIterator;

public class PerformanceMonitor {
    private final DecimalFormat decimalFormat;
    private final OperatingSystemMXBean platformMXBean;
    private final Runtime runtime;
    private final Logger logger;
    private final StringCharacterIterator ci;

    public PerformanceMonitor() {
        decimalFormat = new DecimalFormat("0.##");
        decimalFormat.setRoundingMode(RoundingMode.HALF_UP);
        platformMXBean = (OperatingSystemMXBean) ManagementFactory.getPlatformMXBean(java.lang.management.OperatingSystemMXBean.class);
        runtime = Runtime.getRuntime();
        logger = LogManager.getLogger("performance-analyse");
        ci = new StringCharacterIterator("KMGTPE", 0);
    }

    public void print() {
        long bytes = runtime.totalMemory() - runtime.freeMemory();
        if (bytes > 1024) {
            long value = bytes;
            ci.first();
            for (byte i = 40; i > -1 && bytes > 0xFFFCCCCCCCCCCCCL >> i; i -= 10) {
                value >>= 10;
                ci.next();
            }
            logger.info(decimalFormat.format((value * Long.signum(bytes)) / 1024.) + " " + ci.current() + "B memory using by parser");
        } else logger.info(bytes + " B memory using by parser");
        logger.info(decimalFormat.format(platformMXBean.getCpuLoad() * 100.) + "% of cpu usage");
    }
}
