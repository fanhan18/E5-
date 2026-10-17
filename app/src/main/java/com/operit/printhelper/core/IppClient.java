package com.operit.printhelper.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Minimal IPP 1.1 client over plain HTTP (RFC 8010).
 * Supports Get-Printer-Attributes (0x000B) and Print-Job (0x0002).
 */
public class IppClient {

    public static class IppResult {
        public int httpCode;
        public int statusCode = -1;
        public String statusText = "";
        public byte[] response;
    }

    // delimiter tags
    private static final int T_OP = 0x01;
    private static final int T_JOB = 0x02;
    private static final int T_END = 0x03;
    // value tags
    private static final int T_INT = 0x21;
    private static final int T_NAME = 0x42;
    private static final int T_KEYWORD = 0x44;
    private static final int T_URI = 0x45;
    private static final int T_CHARSET = 0x47;
    private static final int T_NATLANG = 0x48;
    private static final int T_MIME = 0x49;

    public static IppResult getPrinterAttributes(String httpUrl, String printerUri) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream(256);
        header(b, 0x000B);
        b.write(T_OP);
        strAttr(b, T_CHARSET, "attributes-charset", "utf-8");
        strAttr(b, T_NATLANG, "attributes-natural-language", "en");
        strAttr(b, T_URI, "printer-uri", printerUri);
        b.write(T_END);
        return post(httpUrl, b.toByteArray());
    }

    public static IppResult printDocument(String httpUrl, String printerUri, String user,
                                          String jobName, int copies, String media,
                                          byte[] document) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream(512);
        header(b, 0x0002); // Print-Job
        b.write(T_OP);
        strAttr(b, T_CHARSET, "attributes-charset", "utf-8");
        strAttr(b, T_NATLANG, "attributes-natural-language", "en");
        strAttr(b, T_URI, "printer-uri", printerUri);
        strAttr(b, T_NAME, "requesting-user-name", user);
        strAttr(b, T_NAME, "job-name", jobName);
        strAttr(b, T_MIME, "document-format", "image/pwg-raster");
        b.write(T_JOB);
        intAttr(b, "copies", copies);
        strAttr(b, T_KEYWORD, "media", media);
        b.write(T_END);
        byte[] head = b.toByteArray();
        byte[] body = new byte[head.length + document.length];
        System.arraycopy(head, 0, body, 0, head.length);
        System.arraycopy(document, 0, body, head.length, document.length);
        return post(httpUrl, body);
    }

    private static void header(ByteArrayOutputStream b, int op) throws IOException {
        b.write(0x01); b.write(0x01);                       // version 1.1
        b.write((op >> 8) & 0xFF); b.write(op & 0xFF);      // operation-id
        b.write(0); b.write(0); b.write(0); b.write(1);     // request-id = 1
    }

    private static void strAttr(ByteArrayOutputStream b, int tag, String name, String value) throws IOException {
        byte[] n = name.getBytes("UTF-8");
        byte[] v = value.getBytes("UTF-8");
        b.write(tag);
        putU16(b, n.length);
        b.write(n);
        putU16(b, v.length);
        b.write(v);
    }

    private static void intAttr(ByteArrayOutputStream b, String name, int value) throws IOException {
        byte[] n = name.getBytes("UTF-8");
        b.write(T_INT);
        putU16(b, n.length);
        b.write(n);
        putU16(b, 4);
        b.write((value >> 24) & 0xFF);
        b.write((value >> 16) & 0xFF);
        b.write((value >> 8) & 0xFF);
        b.write(value & 0xFF);
    }

    private static void putU16(ByteArrayOutputStream b, int v) throws IOException {
        b.write((v >> 8) & 0xFF);
        b.write(v & 0xFF);
    }

    private static IppResult post(String urlStr, byte[] body) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(urlStr).openConnection();
        c.setDoOutput(true);
        c.setRequestMethod("POST");
        c.setConnectTimeout(8000);
        c.setReadTimeout(90000);
        c.setRequestProperty("Content-Type", "application/ipp");
        c.setFixedLengthStreamingMode(body.length);
        OutputStream os = c.getOutputStream();
        os.write(body);
        os.flush();
        os.close();
        IppResult r = new IppResult();
        r.httpCode = c.getResponseCode();
        InputStream in = (r.httpCode >= 400) ? c.getErrorStream() : c.getInputStream();
        if (in == null) in = new ByteArrayInputStream(new byte[0]);
        r.response = readAll(in);
        c.disconnect();
        if (r.response.length >= 4) {
            r.statusCode = ((r.response[2] & 0xFF) << 8) | (r.response[3] & 0xFF);
            r.statusText = statusText(r.statusCode);
        }
        return r;
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        byte[] buf = new byte[16384];
        int n;
        while ((n = in.read(buf)) > 0) b.write(buf, 0, n);
        in.close();
        return b.toByteArray();
    }

    static String statusText(int s) {
        switch (s) {
            case 0x0000: return "successful-ok";
            case 0x0001: return "successful-ok-ignored-subscriptions";
            case 0x0002: return "successful-ok-ignored-or-substituted-attributes";
            case 0x0400: return "client-error-bad-request";
            case 0x0401: return "client-error-forbidden";
            case 0x0402: return "client-error-not-authenticated";
            case 0x0403: return "client-error-not-authorized";
            case 0x0404: return "client-error-not-found";
            case 0x0405: return "client-error-conflict";
            case 0x0406: return "client-error-not-acceptable";
            case 0x0408: return "client-error-request-timeout";
            case 0x040B: return "client-error-too-many-requests";
            case 0x040C: return "client-error-document-format-not-supported";
            case 0x040D: return "client-error-attributes-or-values-not-supported";
            case 0x0410: return "client-error-temporary-error";
            case 0x0413: return "client-error-document-access-error";
            case 0x0500: return "server-error-internal-error";
            case 0x0501: return "server-error-operation-not-supported";
            case 0x0502: return "server-error-service-unavailable";
            case 0x0503: return "server-error-device-offline";
            default: return "ipp-0x" + Integer.toHexString(s);
        }
    }
}
