package com.vprave.niqitadev.parser.storage;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties("storage")
public class StorageProperties {

	@Getter
	private Path location = Path.of("upload");

	public void setLocation(String location) {
		this.location = Path.of(location);
	}
}
