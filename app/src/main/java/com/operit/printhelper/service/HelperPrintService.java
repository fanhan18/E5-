package com.operit.printhelper.service;

import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.print.PrintAttributes;
import android.print.PrintJobInfo;
import android.print.PrinterCapabilitiesInfo;
import android.print.PrinterId;
import android.print.PrinterInfo;
import android.printservice.PrintService;
import android.printservice.PrinterDiscoverySession;
import android.printservice.PrintJob;
import android.util.Log;

import com.operit.printhelper.core.PrintCore;

import java.util.ArrayList;
import java.util.List;

public class HelperPrintService extends PrintService {
    private static final String TAG = "HelperPrintService";

    @Override
    protected PrinterDiscoverySession onCreatePrinterDiscoverySession() {
        return new SimpleDiscoverySession();
    }

    @Override
    protected void onRequestCancelPrintJob(PrintJob printJob) {
        printJob.cancel();
    }

    @Override
    protected void onPrintJobQueued(final PrintJob printJob) {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... p) {
                try {
                    ParcelFileDescriptor pfd = printJob.getDocument().getData();
                    if (pfd == null) return "No data";
                    PrintJobInfo info = printJob.getInfo();
                    int duplexMode = info.getAttributes() == null
                            ? PrintAttributes.DUPLEX_MODE_NONE
                            : info.getAttributes().getDuplexMode();
                    boolean duplex = duplexMode == PrintAttributes.DUPLEX_MODE_LONG_EDGE
                            || duplexMode == PrintAttributes.DUPLEX_MODE_SHORT_EDGE;
                    boolean shortEdge = duplexMode == PrintAttributes.DUPLEX_MODE_SHORT_EDGE;
                    PrintCore.printPdf(HelperPrintService.this, pfd, duplex, shortEdge, null);
                    return null;
                } catch (Exception e) {
                    Log.e(TAG, "print failed", e);
                    return e.getMessage() == null ? e.toString() : e.getMessage();
                }
            }

            @Override
            protected void onPostExecute(String err) {
                if (err == null) printJob.complete();
                else printJob.fail(err);
            }
        }.execute();
    }

    private class SimpleDiscoverySession extends PrinterDiscoverySession {
        private List<PrinterInfo> printers = new ArrayList<>();
        @Override
        public void onStartPrinterDiscovery(List<PrinterId> priorityList) {
            PrinterId pid = generatePrinterId("pantum_helper");
            PrinterCapabilitiesInfo.Builder caps = new PrinterCapabilitiesInfo.Builder(pid);
            caps.addMediaSize(PrintAttributes.MediaSize.ISO_A4, true);
            caps.addResolution(new PrintAttributes.Resolution("r300", "300dpi", 300, 300), true);
            caps.setColorModes(PrintAttributes.COLOR_MODE_MONOCHROME, PrintAttributes.COLOR_MODE_MONOCHROME);
            caps.setDuplexModes(PrintAttributes.DUPLEX_MODE_LONG_EDGE | PrintAttributes.DUPLEX_MODE_SHORT_EDGE, PrintAttributes.DUPLEX_MODE_LONG_EDGE);
            PrinterInfo pinfo = new PrinterInfo.Builder(pid, "Pantum Helper", PrinterInfo.STATUS_IDLE)
                    .setCapabilities(caps.build())
                    .build();
            printers.add(pinfo);
            addPrinters(printers);
        }
        @Override public void onStopPrinterDiscovery() {}
        @Override public void onValidatePrinters(List<PrinterId> printerIds) {}
        @Override public void onStartPrinterStateTracking(PrinterId printerId) {}
        @Override public void onStopPrinterStateTracking(PrinterId printerId) {}
        @Override public void onDestroy() { printers.clear(); }
    }
}
