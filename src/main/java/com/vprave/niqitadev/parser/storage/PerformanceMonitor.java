package com.vprave.niqitadev.parser.storage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.StringCharacterIterator;

@Service
public class PerformanceMonitor {
    private final DecimalFormat decimalFormat;
    private final Runtime runtime;
    private final Logger logger;
    private final StringCharacterIterator characterIterator;

    public PerformanceMonitor() {
        decimalFormat = new DecimalFormat("0.##");
        decimalFormat.setRoundingMode(RoundingMode.HALF_UP);
        runtime = Runtime.getRuntime();
        logger = LogManager.getLogger("performance-analyse");
        characterIterator = new StringCharacterIterator("KMGTPE", 0);
    }

    private double getProcessCpuLoad() throws Exception {
        AttributeList list = ManagementFactory.getPlatformMBeanServer().getAttributes(
                ObjectName.getInstance("java.lang:type=OperatingSystem"), new String[]{"ProcessCpuLoad"});
        if (list.isEmpty()) return 0;
        Double value = (Double) ((Attribute) list.get(0)).getValue();
        return value == -1. ? 0 : value * 100;
    }

    public void print() throws Exception {
        long bytes = runtime.totalMemory() - runtime.freeMemory();
        if (bytes > 1024) {
            long value = bytes;
            characterIterator.first();
            for (byte i = 40; i > -1 && bytes > 0xFFFCCCCCCCCCCCCL >> i; i -= 10) {
                value >>= 10;
                characterIterator.next();
            }
            logger.info(decimalFormat.format((value * Long.signum(bytes)) / 1024.) + " " + characterIterator.current() + "B memory using by parser");
        } else logger.info(bytes + "B memory using by parser");
        logger.info(decimalFormat.format(getProcessCpuLoad()) + "% of cpu usage");
    }
}
