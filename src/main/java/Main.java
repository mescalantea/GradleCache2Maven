import java.io.*;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

public class Main {

    private static final String GRADLE_ARG = "-gradle";
    private static final String M2_ARG = "-repo";

    public static void main(String[] args) {
        System.out.println("Gradle-Cache 2 Maven-Local Tool\nMichel Escalante Álvarez. 2017\n-----------------------------------------------\n");
        final long startMillis = Calendar.getInstance().getTimeInMillis();

        String repoPath = null, gradlePath = null;

        if(args != null && args.length > 0){
            // parse arguments
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];

                if(arg.equals(GRADLE_ARG) && ++i < args.length){
                    gradlePath = args[i];
                }
                else if(arg.equals(M2_ARG) && ++i < args.length){
                    repoPath = args[i];
                }
            }

        }

        if(gradlePath == null || repoPath == null) {
            String userProfile = System.getenv("USERPROFILE");
            if (userProfile == null) {
                System.err.println("[ERROR]\tUSERPROFILE system variable is not set.");
                return;
            }
            if(gradlePath == null) gradlePath = userProfile + File.separator + ".gradle";
            if(repoPath == null) repoPath = userProfile + File.separator + ".m2" + File.separator + "repository";
        }

        final File mavenLocalDir = new File(repoPath);
        if(!mavenLocalDir.exists() && !mavenLocalDir.mkdirs()) {
            System.err.println("[ERROR]\tError creating Maven repo directory.");
            return;
        }
        if(!(mavenLocalDir.isDirectory() && mavenLocalDir.canWrite())) {
            System.err.println("[ERROR]\tMaven repo directory does not exists, can't be written or is not a directory.");
            return;
        }
        System.out.println("[INFO]\tMaven local repo found at \"path\".".replace("path", repoPath));

        // current windows user directory
        final File gradleCacheDir = new File(gradlePath  + File.separator + "caches");
        if(!(gradleCacheDir.exists() && gradleCacheDir.isDirectory() && gradleCacheDir.canWrite())) {
            System.err.println("[ERROR]\tGradle cache directory does not exists, can't be writen or is not a directory.");
            return;
        }
        System.out.println("[INFO]\tGradle cache directory found at \"path\".".replace("path", gradleCacheDir.getAbsolutePath()));

        // gradle modules dirs
        final List<File> gradleModulesDirs = new LinkedList<>();
        File[] cacheFiles = gradleCacheDir.listFiles();
        if(cacheFiles == null){
            System.err.println("[ERROR]\t$caches directory read subdirectories operation throws null.".replace("$caches", gradleCacheDir.getName()));
            return;
        }
        for (File child : cacheFiles) {
            if(child.isDirectory() && child.getName().startsWith("modules-"))
                gradleModulesDirs.add(child);
        }

        // gradle files dirs
        final List<File> gradleFilesDirs = new LinkedList<>();
        for (File modules : gradleModulesDirs) {
            File[] modulesFiles = modules.listFiles();
            if(modulesFiles == null){
                System.err.println("[ERROR]\t$module directory read subdirectories operation throws null.".replace("$module", modules.getName()));
                return;
            }
            for (File child : modulesFiles) {
                if(child.isDirectory() && child.getName().startsWith("files-")) gradleFilesDirs.add(child);
            }
        }

        // loop artifacts at files dir and add it to repo
        int copyCount = 0;
        for (File files : gradleFilesDirs) {
            if(files.isFile()) continue;

            File[] filesFiles = files.listFiles();
            if(filesFiles == null){
                System.err.println("[ERROR]\t$files directory read subdirectories operation throws null.".replace("$files", files.getName()));
                return;
            }

            for (File group : filesFiles) {
                if(group.isFile()) continue;
                // the first level is the group-id
                MavenArtifact mavenArtifact = new MavenArtifact();
                mavenArtifact.setGroupId(group.getName());

                File[] groupFiles = group.listFiles();
                if(groupFiles == null){
                    System.err.println("[ERROR]\t$group directory read subdirectories operation throws null.".replace("$group", group.getName()));
                    return;
                }
                for (File artifact : groupFiles) {
                    if(artifact.isFile()) continue;
                    // the second level is the artifact-id
                    mavenArtifact.setArtifactId(artifact.getName());

                    File[] artifactFiles = artifact.listFiles();
                    if(artifactFiles == null){
                        System.err.println("[ERROR]\t$artifact directory read subdirectories operation throws null.".replace("$artifact", artifact.getName()));
                        return;
                    }
                    for (File version : artifactFiles) {
                        if(artifact.isFile()) continue;
                        // the third level is the artifact-version
                        mavenArtifact.setVersion(version.getName());

                        File[] versionFiles = version.listFiles();
                        if(versionFiles == null){
                            System.err.println("[ERROR]\t$version directory read subdirectories operation throws null.".replace("$version", version.getName()));
                            return;
                        }
                        for (File dir : versionFiles) {
                            if(group.isFile()) continue;
                            // the fourth level are folders containing the artifact's files (.pom, .jars, .aar)
                            File[] dirFiles = dir.listFiles();
                            if(dirFiles == null){
                                System.err.println("[ERROR]\t$dir directory read subdirectories operation throws null.".replace("$dir", dir.getName()));
                                return;
                            }
                            for (File file : dirFiles) {
                                // the fifth level is an artifact's file
                                if(mavenArtifact.copyToRepo(file, mavenLocalDir)){
                                    copyCount++;
                                    System.out.println("[INFO]\tCopied file in path"
                                            .replace("file", file.getName())
                                            .replace("path", mavenLocalDir.getAbsolutePath()));
                                }
                                else System.out.println("[INFO]\tFile skipped \"file\""
                                        .replace("file", file.getAbsolutePath()));
                            }
                        }
                    }
                }
            }
        }

        System.out.println("[INFO]\tDone!\nFiles copied: $files.\nTime: $seconds seconds"
                .replace("$seconds", String.valueOf((int)(Calendar.getInstance().getTimeInMillis() - startMillis) / 1000))
                .replace("$files", String.valueOf(copyCount)));

    }

    static private class MavenArtifact{
        private String groupId;
        private String artifactId;
        private String version;

        void setGroupId(String groupId) {
            this.groupId = groupId;
        }

        void setArtifactId(String artifactId) {
            this.artifactId = artifactId;
        }

        void setVersion(String version) {
            this.version = version;
        }

        boolean copyToRepo(File toInstall, File mavenRepo){
            boolean success = false;
            if(toInstall.isFile() && (toInstall.getName().endsWith(".aar") || toInstall.getName().endsWith(".jar") || toInstall.getName().endsWith(".war") || toInstall.getName().endsWith(".pom"))) {
                String path = groupId
                        .replace(".", File.separator) + File.separator + artifactId
                        .replace(".", File.separator) + File.separator + version;

                InputStream is = null;
                OutputStream os = null;
                try {
                    File destDir = new File(mavenRepo.getAbsolutePath() + File.separator + path);
                    if(!destDir.exists() && !destDir.mkdirs()){
                        throw new Exception("Directory creation failed");
                    }

                    File output = new File(destDir + File.separator + toInstall.getName());

                    if(!(output.exists() && output.isFile() && toInstall.length() == output.length())) {
                        if(output.exists() && !output.delete()) {
                            throw new Exception("File deletion failed");
                        }

                        is = new FileInputStream(toInstall);
                        os = new FileOutputStream(output);
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = is.read(buffer)) > 0) {
                            os.write(buffer, 0, length);
                        }
                        success = true;
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                    System.err.println("[ERROR]\tFail installing file at path"
                            .replace("file", toInstall.getName())
                            .replace("path", path));
                }
                finally {
                    try {
                        if(is != null) is.close();
                        if(os != null) os.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return success;

        }
    }
}