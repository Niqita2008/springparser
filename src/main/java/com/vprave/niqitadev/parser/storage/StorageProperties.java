package com.vprave.niqitadev.parser.storage;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.text.SimpleDateFormat;

@ConfigurationProperties("storage")
public class StorageProperties {
	@Getter
	private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	@Getter
	private Path location = Path.of("upload");

	public void setLocation(String location) {
		this.location = Path.of(location);
	}
}
