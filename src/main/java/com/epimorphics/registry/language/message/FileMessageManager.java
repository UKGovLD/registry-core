package com.epimorphics.registry.language.message;

import com.epimorphics.appbase.core.App;
import com.epimorphics.appbase.core.Startup;
import com.epimorphics.registry.core.RegistryDirBootstrap;
import com.epimorphics.registry.language.MessagesProperties;
import com.epimorphics.util.EpiException;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.jena.atlas.lib.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

public class FileMessageManager implements MessageManager, Startup {
    private final Logger log = LoggerFactory.getLogger(FileMessageManager.class);
    private File msgDir;
    private Map<String, Messages> messagesByLang;

    public FileMessageManager() {
        this(RegistryDirBootstrap.fileRoot + "config/language/messages");
    }

    FileMessageManager(String msgsLoc) {
        this.msgDir = new File(msgsLoc);
    }

    public void setMessageDir(String path) {
        this.msgDir = new File(path);
    }

    @Override public void startup(App app) {
        init();
    }

    public FileMessageManager init() {
        this.messagesByLang = getMessagesByLang();
        return this;
    }

    private Map<String, Messages> getMessagesByLang() {
        File[] msgFiles = msgDir.listFiles((FileFilter) new SuffixFileFilter("properties"));
        if (msgFiles == null) {
            log.warn("Messages directory does not exist: " + msgDir.getAbsolutePath());
            return new HashMap<>();
        }

        return Arrays.stream(msgFiles).map(msgFile -> {
            log.info("Configuring messages file at " + msgFile.getAbsolutePath());
            String lang = msgFile.getName().replace(".properties", "");
            try {
                Messages msgs = getMessages(lang, msgFile);
                return new Pair<>(lang, msgs);
            } catch (IOException ioe) {
                throw new EpiException("Unable to read message files.", ioe);
            }
        }).collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
    }

    private Messages getMessages(String lang, File msgFile) throws IOException {
        Properties props = getProperties(msgFile);
        return new MessagesProperties(lang, props);
    }

    private Properties getProperties(File msgFile) throws IOException {
        try (InputStream input = new FileInputStream(msgFile)) {
            Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8);
            Properties props = new Properties();
            props.load(reader);

            return props;
        }
    }

    @Override public Messages getMessages(String lang) {
        return messagesByLang.get(lang);
    }
}
