task checkForDuplicateManifest {
    doLast {
        def manifestFiles = []

        configurations.all { configuration ->
            // Skip configurations that cannot be resolved
            if (!configuration.canBeResolved) {
                return
            }
            try {
                configuration.resolvedConfiguration.resolvedArtifacts.each { artifact ->
                    def file = artifact.file
                    if (file.name.endsWith('.jar')) {
                        def zipFile = new ZipFile(file)
                        zipFile.entries().each {
                            if (it.name.equals('META-INF/MANIFEST.MF')) {
                                manifestFiles << "$file!/${it.name}"
                            }
                        }
                    }
                }
            } catch (Exception e) {
                println "Could not resolve configuration: ${configuration.name}"
            }
        }

        if (manifestFiles.size() > 1) {
            println 'WARNING: More than one MANIFEST.MF file was found:'
            manifestFiles.each { println it}
        }
    }
}