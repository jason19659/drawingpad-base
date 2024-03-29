/*******************************************************************************
 * Copyright (c) 2011 MadRobot.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *  Elton Kent - initial API and implementation
 ******************************************************************************/
package com.madrobot.io.file;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.List;

import android.util.Log;

import com.madrobot.io.IOUtils;
import com.madrobot.text.StringUtils;

public class FileUtils {
	private static final String TAG = "Lib:FileUtil";

	public static String getDirectoryName(String filename) {
		int slashposition = filename.lastIndexOf("/");
		if (slashposition >= 0)
			filename = filename.substring(0, slashposition);
		return filename;
	}

	public static String getFileName(String filename) {
		int slashposition = filename.lastIndexOf("/");
		if (slashposition >= 0)
			filename = filename.substring(slashposition + 1, filename.length());
		return filename;
	}

	public static String getFileExtension(String filename) {
		int dotposition = filename.lastIndexOf(".");
		String ext = filename.substring(dotposition + 1, filename.length());
		return ext;
	}

	/**
	 * Check if the file exists on the given path
	 * 
	 * @param path
	 * @return True if the file exists
	 */
	public static boolean fileExists(String path) {
		boolean exists = new File(path).exists();
		Log.d(TAG, "File Exists for file " + path + " =" + exists);
		return exists;
	}

	/**
	 * Create a directory in the given <code>path</code>
	 */
	public static boolean makeDirectory(String path, String directoryName) {
		File f = new File(path, directoryName);
		if (!f.exists()) {
			return f.mkdir();
		}
		return false;
	}

	/**
	 * Finds the relative path from base to file. The file returned is such that
	 * file.getCanonicalPath().equals( new File( base, getRelativePath( base,
	 * file ).getPath() ).getCanonicalPath )
	 * 
	 * @param base
	 *            The base for the relative path
	 * @param file
	 *            The destination for the relative path
	 * @return The relative path betwixt base and file, or null if there was a
	 *         IOException
	 */
	public static File getRelativePath(File base, File file) {
		try {
			String absBase = base.getCanonicalPath();
			String absFile = file.getCanonicalPath();

			if (absBase.equals(absFile)) { // base and file are the same file
				return new File("");
			}

			// find divergence point
			int divergenceCharIndex = 0;
			int divergenceSeperatorIndex = 0;

			while ((divergenceCharIndex < absBase.length())
					&& (divergenceCharIndex < absFile.length())
					&& (absBase.charAt(divergenceCharIndex) == absFile
							.charAt(divergenceCharIndex))) {
				if ((absBase.charAt(divergenceCharIndex) == File.separatorChar)
						|| (absFile.charAt(divergenceCharIndex) == File.separatorChar)) {
					divergenceSeperatorIndex = divergenceCharIndex;
				}

				divergenceCharIndex++;
			}

			if (divergenceCharIndex == absBase.length()) { // simple case where
				// the base is an
				// ancestor of the
				// target
				// file
				return new File(absFile.substring(divergenceCharIndex + 1));
			} else { // we need to back up the tree a bit, before coming back
				// down.
				StringBuilder path = new StringBuilder();
				for (int i = divergenceSeperatorIndex; i < absBase.length(); i++) {
					if (absBase.charAt(i) == File.separatorChar) {
						path.append("..");
						path.append(File.separatorChar);
					}
				}

				path.append(absFile.substring(divergenceSeperatorIndex));

				return new File(path.toString());
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return null;
		}
	}

	/**
	 * Makes a directory, including any necessary but nonexistent parent
	 * directories. If a file already exists with specified name but it is not a
	 * directory then an IOException is thrown. If the directory cannot be
	 * created (or does not already exist) then an IOException is thrown.
	 * 
	 * @param directory
	 *            directory to create, must not be <code>null</code>
	 * @throws NullPointerException
	 *             if the directory is <code>null</code>
	 * @throws IOException
	 *             if the directory cannot be created or the file already exists
	 *             but is not a directory
	 */
	public static void forceMkdir(File directory) throws IOException {
		if (directory.exists()) {
			if (!directory.isDirectory()) {
				String message = "File " + directory + " exists and is "
						+ "not a directory. Unable to create directory.";
				throw new IOException(message);
			}
		} else {
			if (!directory.mkdirs()) {
				// Double-check that some other thread or process hasn't made
				// the directory in the background
				if (!directory.isDirectory()) {
					String message = "Unable to create directory " + directory;
					throw new IOException(message);
				}
			}
		}
	}

	/**
	 * Deletes a file. If file is a directory, delete it and all
	 * sub-directories.
	 * <p>
	 * The difference between File.delete() and this method are:
	 * <ul>
	 * <li>A directory to be deleted does not have to be empty.</li>
	 * <li>You get exceptions when a file or directory cannot be deleted.
	 * (java.io.File methods returns a boolean)</li>
	 * </ul>
	 * 
	 * @param file
	 *            file or directory to delete, must not be <code>null</code>
	 * @throws NullPointerException
	 *             if the directory is <code>null</code>
	 * @throws FileNotFoundException
	 *             if the file was not found
	 * @throws IOException
	 *             in case deletion is unsuccessful
	 */
	public static void forceDelete(File file) throws IOException {
		if (file.isDirectory()) {
			deleteDirectory(file);
		} else {
			boolean filePresent = file.exists();
			if (!file.delete()) {
				if (!filePresent) {
					throw new FileNotFoundException("File does not exist: "
							+ file);
				}
				String message = "Unable to delete file: " + file;
				throw new IOException(message);
			}
		}
	}

	/**
	 * Deletes a directory recursively.
	 * 
	 * @param directory
	 *            directory to delete
	 * @throws IOException
	 *             in case deletion is unsuccessful
	 */
	public static void deleteDirectory(File directory) throws IOException {
		if (!directory.exists()) {
			return;
		}
		if (!directory.delete()) {
			String message = "Unable to delete directory " + directory + ".";
			throw new IOException(message);
		}
	}

	/**
	 * Removes all files from the directory
	 * <p>
	 * If the <code>toKeep</code> is null or an empty array, then the directory
	 * is just deleted.
	 * </p>
	 * 
	 * @param directory
	 *            Directory containing the files
	 * @param toKeep
	 *            Files to keep
	 * @see FileUtils#deleteDirectory(File)
	 * @throws IOException
	 * @throws IOException
	 */
	public static void removeAllFilesExcept(File directory, File[] toKeep)
			throws IOException {
		File[] allfiles = directory.listFiles();
		if (toKeep == null || toKeep.length == 0) {
			// no files
			deleteDirectory(directory);
		} else {
			for (int i = 0; i < allfiles.length; i++) {
				if (!contains(toKeep, allfiles[i])) {
					allfiles[i].delete();
				}
			}
		}
	}

	/**
	 * Check if the given file is contained in the list of files
	 * 
	 * @param files
	 *            the list of files
	 * @param file
	 *            the file to search for
	 * @return true on the first occurance of the file.
	 */
	public static boolean contains(File[] files, File file) {
		if (files == null || file == null) {
			return false;
		}
		for (int i = 0; i < files.length; i++) {
			if (files[i].equals(file)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Waits for File System to propagate a file creation, imposing a timeout.
	 * <p>
	 * This method repeatedly tests {@link File#exists()} until it returns true
	 * up to the maximum time specified in seconds.
	 * 
	 * @param file
	 *            the file to check, must not be <code>null</code>
	 * @param seconds
	 *            the maximum time in seconds to wait
	 * @return true if file exists
	 * @throws NullPointerException
	 *             if the file is <code>null</code>
	 */
	public static boolean waitFor(File file, int seconds) {
		int timeout = 0;
		int tick = 0;
		while (!file.exists()) {
			if (tick++ >= 10) {
				tick = 0;
				if (timeout++ > seconds) {
					return false;
				}
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException ignore) {
				// ignore exception
			} catch (Exception ex) {
				break;
			}
		}
		return true;
	}

	/**
	 * Reads the contents of a file into a byte array. The file is always
	 * closed.
	 * 
	 * @param file
	 *            the file to read, must not be <code>null</code>
	 * @return the file contents, never <code>null</code>
	 * @throws IOException
	 *             in case of an I/O error
	 */
	public static byte[] readFileToByteArray(File file) throws IOException {
		InputStream in = null;
		try {
			in = openInputStream(file);
			return IOUtils.toByteArray(in);
		} finally {
			in.close();
		}
	}

	public static void writeByteArrayToFile(byte[] src, File file)
			throws IOException {
		ByteArrayInputStream stream = null;
		try {
			stream = new ByteArrayInputStream(src);
			inputStreamToFile(stream, file);
		} finally {
			try {
				if (stream != null)
					stream.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static final void inputStreamToFile(InputStream src, File file)
			throws IOException {
		FileOutputStream stream = null;
		try {
			if (file.getParentFile() != null)
				file.getParentFile().mkdirs();
			stream = new FileOutputStream(file);
			IOUtils.copy(src, stream);
		} finally {
			try {
				if (stream != null)
					stream.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Reads the contents of a file line by line to a List of Strings. The file
	 * is always closed.
	 * 
	 * @param file
	 *            the file to read, must not be <code>null</code>
	 * @param encoding
	 *            the encoding to use, <code>null</code> means platform default
	 * @return the list of Strings representing each line in the file, never
	 *         <code>null</code>
	 * @throws IOException
	 *             in case of an I/O error
	 * @throws java.io.UnsupportedEncodingException
	 *             if the encoding is not supported by the VM
	 */
	public static final List<String> readLines(File file, String encoding)
			throws IOException {
		InputStream in = null;
		try {
			in = openInputStream(file);
			return IOUtils.readLines(in, encoding);
		} finally {
			in.close();
		}
	}

	/**
	 * Reads the contents of a file line by line to a List of Strings using the
	 * default encoding for the VM. The file is always closed.
	 * 
	 * @param file
	 *            the file to read, must not be <code>null</code>
	 * @return the list of Strings representing each line in the file, never
	 *         <code>null</code>
	 * @throws IOException
	 *             in case of an I/O error
	 */
	public static final List<String> readLines(File file) throws IOException {
		return readLines(file, null);
	}

	/**
	 * Opens a {@link FileInputStream} for the specified file, providing better
	 * error messages than simply calling <code>new FileInputStream(file)</code>
	 * .
	 * <p>
	 * At the end of the method either the stream will be successfully opened,
	 * or an exception will have been thrown.
	 * <p>
	 * An exception is thrown if the file does not exist. An exception is thrown
	 * if the file object exists but is a directory. An exception is thrown if
	 * the file exists but cannot be read.
	 * 
	 * @param file
	 *            the file to open for input, must not be <code>null</code>
	 * @return a new {@link FileInputStream} for the specified file
	 * @throws FileNotFoundException
	 *             if the file does not exist
	 * @throws IOException
	 *             if the file object is a directory
	 * @throws IOException
	 *             if the file cannot be read
	 */
	public static final FileInputStream openInputStream(File file)
			throws IOException {
		if (file.exists()) {
			if (file.isDirectory()) {
				throw new IOException("File '" + file
						+ "' exists but is a directory");
			}
			if (file.canRead() == false) {
				throw new IOException("File '" + file + "' cannot be read");
			}
		} else {
			throw new FileNotFoundException("File '" + file
					+ "' does not exist");
		}
		return new FileInputStream(file);
	}

	/**
	 * Use NIO to copy a file
	 * 
	 * @param src
	 *            Source Path
	 * @param dst
	 *            Destination Path
	 * @return
	 * @throws IOException
	 */
	public static final boolean copyFileNio(File src, File dst)
			throws IOException {
		FileChannel srcChannel = null, dstChannel = null;
		try {
			// Create channel on the source
			srcChannel = new FileInputStream(src).getChannel();

			// Create channel on the destination
			dstChannel = new FileOutputStream(dst).getChannel();

			// // Copy file contents from source to destination
			// dstChannel.transferFrom(srcChannel, 0, srcChannel.size());

			{
				// long theoretical_max = (64 * 1024 * 1024) - (32 * 1024);
				int safe_max = (64 * 1024 * 1024) / 4;
				long size = srcChannel.size();
				long position = 0;
				while (position < size) {
					position += srcChannel.transferTo(position, safe_max,
							dstChannel);
				}
			}

			// Close the channels
			srcChannel.close();
			srcChannel = null;
			dstChannel.close();
			dstChannel = null;

			return true;
		} finally {
			try {
				if (srcChannel != null) {
					srcChannel.close();
				}
			} catch (IOException e) {

			}
			try {
				if (dstChannel != null) {
					dstChannel.close();
				}
			} catch (IOException e) {

			}
		}
	}

	/**
	 * Remove invalid FAT filesystem characters from the given string
	 * 
	 * @param str
	 * @return
	 */
	public static String removeInvalidFatChars(String str) {
		String[] invalidChars = new String[] { ":", "\\", "/", "<", ">", "?",
				"*", "|", "\"", ";" };
		return StringUtils.strip(str, invalidChars);
	}
}
