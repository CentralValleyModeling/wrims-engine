package wrimsv2.wreslparser.elements;

public class PathUtils {

	private PathUtils() {

	}

	public static String findRelativePath(String referencePath, String targetPath) {

		// Thanks to Peter Morris
		StringBuilder relativePath = null;

		referencePath = referencePath.replaceAll("\\\\", "/");
		targetPath = targetPath.replaceAll("\\\\", "/");

		if (referencePath.equalsIgnoreCase(targetPath)) {

		}
		else {
			String[] absoluteDirectories = referencePath.split("/");
			String[] relativeDirectories = targetPath.split("/");

			// Get the shortest of the two paths
			int length = absoluteDirectories.length < relativeDirectories.length ? absoluteDirectories.length
					: relativeDirectories.length;

			// Use to determine where in the loop we exited
			int lastCommonRoot = -1;
			int index;

			// Find common root
			for (index = 0; index < length; index++) {
				if (absoluteDirectories[index].equalsIgnoreCase(relativeDirectories[index])) {
					lastCommonRoot = index;
				}
				else {
					break;
					// If we didn't find a common prefix then throw
				}
			}
			if (lastCommonRoot != -1) {
				// Build up the relative path
				relativePath = new StringBuilder();
				// Add on the ..
				for (index = lastCommonRoot + 1; index < absoluteDirectories.length; index++) {
					if (absoluteDirectories[index].length() > 0) {
						relativePath.append("../");
					}
				}
				for (index = lastCommonRoot + 1; index < relativeDirectories.length - 1; index++) {
					relativePath.append(relativeDirectories[index] + "/");
				}
				relativePath.append(relativeDirectories[relativeDirectories.length - 1]);
			}
		}

		if (relativePath == null) {
			return null;
		}
		else {
			String result = relativePath.toString();
			
			// convert to windows style separator
			result = result.replaceAll("/", "\\\\"); 
														
			return result;
		}

	}

}