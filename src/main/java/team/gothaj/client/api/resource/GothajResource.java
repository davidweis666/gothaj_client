package team.gothaj.client.api.resource;

import lombok.val;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.IMetadataSerializer;
import net.minecraft.util.ResourceLocation;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

public class GothajResource implements IResourcePack {
	@Override
	public InputStream getInputStream(ResourceLocation location) throws IOException {
		String path = "/assets/" +
				location.getResourceDomain() +
				"/" +
				location.getResourcePath();
		return GothajResource.class.getResourceAsStream(path);
	}

	@Override
	public boolean resourceExists(ResourceLocation location) {
		String path = "/assets/" +
				location.getResourceDomain() +
				"/" +
				location.getResourcePath();
		return GothajResource.class.getResource(path) != null;
	}

	@Override
	public Set<String> getResourceDomains() {
		return Set.of("gothaj");
	}

	@Override
	public <T extends IMetadataSection> T getPackMetadata(IMetadataSerializer metadataSerializer, String metadataSectionName) throws IOException {
		return null;
	}

	@Override
	public BufferedImage getPackImage() throws IOException {
		return null;
	}

	@Override
	public String getPackName() {
		return "Gothaj";
	}
}
