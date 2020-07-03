package controllers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import ninja.AssetsControllerHelper;
import ninja.Context;
import ninja.Renderable;
import ninja.Result;
import ninja.Results;
import ninja.utils.HttpCacheToolkit;
import ninja.utils.MimeTypes;
import ninja.utils.NinjaProperties;
import ninja.utils.ResponseStreams;

@Singleton
public class UUSAssetsController {

	public final static String ASSETS_DIR = "assets/uus";

	public final static String FILENAME_PATH_PARAM = "fileName";

	private final MimeTypes mimeTypes;

	private final HttpCacheToolkit httpCacheToolkit;

	private final NinjaProperties ninjaProperties;

	private final AssetsControllerHelper assetsControllerHelper;

	@Inject
	public UUSAssetsController(AssetsControllerHelper assetsControllerHelper, HttpCacheToolkit httpCacheToolkit,
			MimeTypes mimeTypes, NinjaProperties ninjaProperties) {
		this.assetsControllerHelper = assetsControllerHelper;
		this.httpCacheToolkit = httpCacheToolkit;
		this.mimeTypes = mimeTypes;
		this.ninjaProperties = ninjaProperties;
	}

	public Result serveStatic() {
		Object renderable = new Renderable() {
			@Override
			public void render(Context context, Result result) {
				String fileName = getFileNameFromPathOrReturnRequestPath(context);
				// "/assets/uus/" + fileName;//
				URL url = getStaticFileFromAssetsDir(fileName);
				streamOutUrlEntity(url, context, result);
			}
		};
		return Results.ok().render(renderable);
	}

	/**
	 * Loads files from assets directory. This is the default directory of Ninja
	 * where to store stuff. Usually in src/main/java/assets/.
	 */
	private URL getStaticFileFromAssetsDir(String fileName) {

		URL url = null;

		/*
		 * if (ninjaProperties.isDev() // Testing that the file exists is
		 * important because // on some dev environments we do not get the
		 * correct asset dir // via System.getPropery("user.dir"). // In that
		 * case we fall back to trying to load from classpath && new
		 * File(assetsDirInDevModeWithoutTrailingSlash()).exists()) { String
		 * finalNameWithoutLeadingSlash =
		 * assetsControllerHelper.normalizePathWithoutLeadingSlash(fileName,
		 * false); File possibleFile = new File(
		 * assetsDirInDevModeWithoutTrailingSlash() + File.separator +
		 * finalNameWithoutLeadingSlash); url = getUrlForFile(possibleFile); }
		 * else {
		 */
		String finalNameWithoutLeadingSlash = assetsControllerHelper.normalizePathWithoutLeadingSlash(fileName, true);
		url = this.getClass().getClassLoader().getResource(ASSETS_DIR + "/" + finalNameWithoutLeadingSlash);
		// }

		return url;
	}

	private URL getUrlForFile(File possibleFileInSrc) {
		if (possibleFileInSrc.exists() && !possibleFileInSrc.isDirectory()) {
			try {
				return possibleFileInSrc.toURI().toURL();
			} catch (MalformedURLException malformedURLException) {
				// logger.error("Error in dev mode while streaming files from
				// src dir. ", malformedURLException);
			}
		}
		return null;
	}

	private static String getFileNameFromPathOrReturnRequestPath(Context context) {

		String fileName = context.getPathParameter(FILENAME_PATH_PARAM);

		if (fileName == null) {
			fileName = context.getRequestPath();
		}
		return fileName;

	}

	private void streamOutUrlEntity(URL url, Context context, Result result) {
		// check if stream exists. if not print a notfound exception
		if (url == null) {
			context.finalizeHeadersWithoutFlashAndSessionCookie(Results.notFound());
		} else if (assetsControllerHelper.isDirectoryURL(url)) {
			// Disable listing of directory contents
			context.finalizeHeadersWithoutFlashAndSessionCookie(Results.notFound());
		} else {
			try {
				URLConnection urlConnection = url.openConnection();
				Long lastModified = urlConnection.getLastModified();
				httpCacheToolkit.addEtag(context, result, lastModified);

				if (result.getStatusCode() == Result.SC_304_NOT_MODIFIED) {
					// Do not stream anything out. Simply return 304
					context.finalizeHeadersWithoutFlashAndSessionCookie(result);
				} else {
					result.status(200);

					// Try to set the mimetype:
					String mimeType = mimeTypes.getContentType(context, url.getFile());

					if (mimeType != null && !mimeType.isEmpty()) {
						result.contentType(mimeType);
					}

					ResponseStreams responseStreams = context.finalizeHeadersWithoutFlashAndSessionCookie(result);

					try (InputStream inputStream = urlConnection.getInputStream();
							OutputStream outputStream = responseStreams.getOutputStream()) {
						ByteStreams.copy(inputStream, outputStream);
					}

				}

			} catch (IOException e) {
				// logger.error("error streaming file", e);
			}

		}

	}
}