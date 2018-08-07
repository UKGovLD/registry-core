package com.epimorphics.registry.webapi.oauth2;

import org.apache.commons.io.filefilter.SuffixFileFilter;

import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exposes OAuth2 configuration.
 */
public class OAuth2Service {
	private static final String CONFIG_LOC = "/opt/ldregistry/config/oauth.conf";
	private static final String PROVIDERS_DIR = "/opt/ldregistry/config/oauth2/provider";

	private final Logger log = LoggerFactory.getLogger(OAuth2Service.class);
	private final OAuth2Config config = createConfig();

	private OAuth2Config createConfig() {
		try {
			FileReader reader = new FileReader(CONFIG_LOC);
			try {
				Properties props = readProperties(reader);
				Collection<OAuth2Provider> providers = getProviders();

				return new OAuth2ConfigProperties(props, providers);
			} catch (IOException ioe) {
				throw new RuntimeException("Unable to read OAuth2 configuration", ioe);
			}
		} catch (IOException ioe) {
			log.info("OAuth configuration file not found at " + CONFIG_LOC + ". OAuth2 login will not be available: " + ioe.getMessage());
			return null; // TODO empty impl?
		}
	}

	private Properties readProperties(FileReader reader) throws IOException {
		Properties props = new Properties();
		try {
			props.load(reader);
		} finally {
			reader.close();
		}

		return props;
	}

	private Collection<OAuth2Provider> getProviders() {
		File providersDir = new File(PROVIDERS_DIR);
		File[] providerFiles = providersDir.listFiles((FileFilter)new SuffixFileFilter("properties"));

		return Arrays.asList(providerFiles).stream().map(file -> {
			String path = file.getAbsolutePath();
			try {
				FileReader reader = new FileReader(path);
				log.info("Configuring OAuth2 provider at " + path);
				Properties props = readProperties(reader);

				return new OAuth2ProviderProperties(props);
			} catch (IOException ioe) {
				throw new RuntimeException("Unable to read OAuth2 provider specs at " + path, ioe);
			}
		}).collect(Collectors.toList());
	}

	public OAuth2Config getConfig() {
		return config;
	}
}
