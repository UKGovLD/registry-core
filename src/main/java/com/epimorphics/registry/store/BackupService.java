/******************************************************************
 * File:        BackupService.java
 * Created by:  Dave Reynolds
 * Created on:  31 Mar 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.store;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPOutputStream;

import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.util.FileUtil;

/**
 * Utility to perform backups on the data store.
 * This implementation is specific to the RDF-backed StoreBaseImpl.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class BackupService {
    static final Logger log = LoggerFactory.getLogger( BackupService.class );
    public static final ExecutorService service = Executors.newFixedThreadPool(1) ;
    
    protected Store store;
    protected String backupDir;
    protected String status = "";
    
    public BackupService(String backupDir, Store store) {
        this.backupDir = backupDir;
        this.store = store;
        FileUtil.ensureDir(backupDir);
        log.info("Backup directory set to: " + backupDir);
    }
    
    public void scheduleBackup() {
        String timestamp =  new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()) ;
        final String filename = backupDir + "/backup-" + timestamp + ".nq.gz";
        
            final Callable<Boolean> task = new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception
                {
                    setStatus("Backup in progress: " + filename);
                    log.info("Started  backup to " +filename);
                    OutputStream out = null ;
                    store.lock();
                    try {
                        out = new FileOutputStream(filename) ;
                        out = new GZIPOutputStream(out, 8*1024) ;
                        out = new BufferedOutputStream(out) ;
                        
                        RDFDataMgr.write(out, store.asDataset().asDatasetGraph(), Lang.NQUADS) ;
                        out.close() ;
                        out = null ;

                        setStatus("Last backup: " + filename);
                        log.info("Finished backup to " + filename);
                        return true;
                        
                    } catch (IOException e) {
                        log.warn("Problem writing backup to " + filename, e);
                        return false;
                        
                    } catch ( RuntimeException ex ) {
                        log.warn("Exception during backup: ", ex);
                        return false;
                        
                    } finally {
                        store.end();
                        try { if (out != null) out.close() ; }
                        catch (IOException e) { /* ignore */ }
                    }
                }};
            
                setStatus("Backup scheduled: " + filename);
            log.info("Scheduled backup to " + filename);                
            service.submit(task) ;
    }
    
    public synchronized void setStatus(String status) {
        this.status = status;
    }
    
    public synchronized String getStatus() {
        return status;
    }
}
