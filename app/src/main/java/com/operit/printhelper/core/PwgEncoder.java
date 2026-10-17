package com.operit.printhelper.core;

import java.io.IOException;
import java.io.OutputStream;

/**
 * PWG Raster writer (PWG 5102.4-2012, CUPS raster v2 page header).
 * Big-endian ("RaS2"), 8-bit grayscale (DeviceGray), uncompressed.
 * Header field offsets follow the CUPS page_header2_t layout, matching
 * Chromium's components/pwg_encoder implementation.
 */
public class PwgEncoder {

    /** DeviceGray color space code */
    public static final int CS_GRAY = 18;

    private static final int HDR_SIZE = 1796;

    // CUPS page header v2 offsets
    private static final int O_DUPLEX = 272;
    private static final int O_HWRES_X = 276;
    private static final int O_HWRES_Y = 280;
    private static final int O_TUMBLE = 368;
    private static final int O_WIDTH = 372;
    private static final int O_HEIGHT = 376;
    private static final int O_BITS_PER_COLOR = 384;
    private static final int O_BITS_PER_PIXEL = 388;
    private static final int O_BYTES_PER_LINE = 392;
    private static final int O_COLOR_ORDER = 396;
    private static final int O_COLOR_SPACE = 400;
    private static final int O_NUM_COLORS = 420;
    private static final int O_TOTAL_PAGES = 452;
    private static final int O_CROSS_FEED = 456;
    private static final int O_FEED = 460;

    public static void writeDocHeader(OutputStream out) throws IOException {
        out.write(new byte[]{'R', 'a', 'S', '2'});
    }

    /**
     * Write one page. gray[] holds width*height bytes, 0=black 255=white.
     * Each page is sent as its own single-page document, so duplex/tumble
     * header flags stay 0 and total page count is 1.
     */
    public static void writePage(OutputStream out, int width, int height,
                                 byte[] gray, int dpi) throws IOException {
        byte[] h = new byte[HDR_SIZE];
        put(h, O_DUPLEX, 0);
        put(h, O_HWRES_X, dpi);
        put(h, O_HWRES_Y, dpi);
        put(h, O_TUMBLE, 0);
        put(h, O_WIDTH, width);
        put(h, O_HEIGHT, height);
        put(h, O_BITS_PER_COLOR, 8);
        put(h, O_BITS_PER_PIXEL, 8);   // grayscale: 1 color * 8 bits
        put(h, O_BYTES_PER_LINE, width);
        put(h, O_COLOR_ORDER, 0);      // chunky
        put(h, O_COLOR_SPACE, CS_GRAY);
        put(h, O_NUM_COLORS, 1);
        put(h, O_TOTAL_PAGES, 1);
        put(h, O_CROSS_FEED, dpi);
        put(h, O_FEED, dpi);
        out.write(h);
        out.write(gray, 0, width * height);
    }

    private static void put(byte[] h, int off, int v) {
        h[off] = (byte) (v >>> 24);
        h[off + 1] = (byte) (v >>> 16);
        h[off + 2] = (byte) (v >>> 8);
        h[off + 3] = (byte) v;
    }
}
