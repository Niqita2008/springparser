package com.vprave.niqitadev.parser.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("storage")
public class StorageProperties {
	public String getLocation() {
		return "upload-dir";
	}
}
