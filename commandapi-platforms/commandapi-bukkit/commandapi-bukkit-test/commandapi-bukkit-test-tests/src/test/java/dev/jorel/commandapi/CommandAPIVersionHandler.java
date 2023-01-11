package dev.jorel.commandapi;

import com.github.zafarkhaja.semver.Version;

import dev.jorel.commandapi.nms.NMS_1_18_R1;
import dev.jorel.commandapi.nms.NMS_1_19_1_R1;
import dev.jorel.commandapi.test.MockNMS;

/**
 * This file handles loading the correct platform implementation. The CommandAPIVersionHandler
 * file within the commandapi-core module is NOT used at run time. Instead, the platform modules
 * replace this class with their own version that handles loads the correct class for their version
 */
public interface CommandAPIVersionHandler {
	
	static CommandAPIPlatform<?, ?, ?> getPlatform() {
		return new MockNMS(switch(System.getProperty("profileId")) {
			case "Minecraft_1_19_2" -> new NMS_1_19_1_R1();
			case "Minecraft_1_18" -> new NMS_1_18_R1();
			default -> throw new IllegalArgumentException("Unexpected value: " + System.getProperty("profileId"));
		});
	}
	
	public static MCVersion getVersion() {
		return switch(System.getProperty("profileId")) {
			case "Minecraft_1_19_2" -> MCVersion.V1_19_2;
			case "Minecraft_1_18" -> MCVersion.V1_18;
			default -> throw new IllegalArgumentException("Unexpected value: " + System.getProperty("profileId"));
		};
	}
	
	public static enum MCVersion {
		V1_19_2(Version.valueOf("1.19.2")),
		V1_19_1(Version.valueOf("1.19.1")),
		V1_19(Version.valueOf("1.19.0")),
		V1_18(Version.valueOf("1.18.0"));
		
		private Version version;
		
		MCVersion(Version version) {
			this.version = version;
		}

		public boolean greaterThanOrEqualTo(MCVersion version) {
			return this.version.greaterThanOrEqualTo(version.version);
		}

		public boolean lessThanOrEqualTo(MCVersion version) {
			return this.version.lessThanOrEqualTo(version.version);
		}
	}
}