package com.epimorphics.registry.language.message;

import com.epimorphics.appbase.core.App;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

public class FileMessageManagerTest {
    private final FileMessageManager file;

    public FileMessageManagerTest() throws IOException {
        File configDir = Files.createTempDirectory("FileMessageManagerTest").toFile();
        configDir.deleteOnExit();

        File enProps = new File(configDir, "en.properties");

        new FileWriter(enProps)
                .append("name.label = Name\n")
                .append("surname.label = Surname\n")
                .append("welcome.heading = Welcome {0} {1}!")
                .close();

        File frProps = new File(configDir, "fr.properties");

        new FileWriter(frProps)
                .append("name.label = Prenom\n")
                .append("surname.label = Nom\n")
                .append("welcome.heading = Bienvenue {0} {1}!")
                .close();

        this.file = new FileMessageManager(configDir.getAbsolutePath());
        file.startup(Mockito.mock(App.class));
    }

    @Test
    public void test() {
        Messages en = file.getMessages("en");
        assertNotNull(en);
        assertEquals("Name", en.get("name.label"));
        assertEquals("Surname", en.get("surname.label"));
        assertEquals("Welcome John Smith!", en.get("welcome.heading", "John", "Smith"));
        assertEquals("Welcome John {1}!", en.get("welcome.heading", "John"));
        assertEquals("Welcome John Andrew!", en.get("welcome.heading", "John", "Andrew", "Smith"));
        assertEquals("", en.get("footer.email.field"));

        Messages fr = file.getMessages("fr");
        assertNotNull(fr);
        assertEquals("Prenom", fr.get("name.label"));
        assertEquals("Nom", fr.get("surname.label"));
        assertEquals("Bienvenue Céline Renée!", fr.get("welcome.heading", "Céline", "Renée"));
        assertEquals("", fr.get("footer.email.field"));

        Messages gr = file.getMessages("gr");
        assertNull(gr);
    }
}
