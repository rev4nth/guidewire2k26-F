package com.guidewire.in.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

	private final Cloudinary cloudinary;

	public CloudinaryService(Cloudinary cloudinary) {
		this.cloudinary = cloudinary;
	}

	/**
	 * Uploads to Cloudinary and returns the HTTPS {@code secure_url} (public delivery URL). That value is what we store on users/claims.
	 */
	public String uploadImage(MultipartFile file, String folder) throws IOException {
		if (file == null || file.isEmpty()) {
			return null;
		}
		String contentType = file.getContentType();
		if (contentType == null || !contentType.startsWith("image/")) {
			throw new IllegalArgumentException("Only image files are allowed");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> params = ObjectUtils.asMap(
				"folder", folder,
				"resource_type", "image",
				"use_filename", true,
				"unique_filename", true);
		@SuppressWarnings("unchecked")
		Map<String, Object> result = cloudinary.uploader().upload(file.getBytes(), params);
		Object url = result.get("secure_url");
		return url != null ? url.toString() : null;
	}
}
