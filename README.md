
# update_fastlane

default_platform(:android)

lane :generate_changelog do
  changelog_file_path = '../CHANGELOG.md'

  branch_name = `git rev-parse --abbrev-ref HEAD`.strip
  head_commit_hash = `git rev-parse HEAD`.strip

  changelog_content = File.read(changelog_file_path)

  section_keywords = {
    'Added' => ['add', 'added'],
    'Changed' => ['change', 'changed'],
    'Fixed' => ['fix', 'fixed']
  }

  branch_section = nil
  section_keywords.each do |section, keywords|
    if keywords.any? { |keyword| branch_name.downcase.include?(keyword) }
      branch_section = section
      break
    end
  end

  UI.error("Branch section #{branch_section}")

  if branch_section
    branch_name_without_prefix = branch_name.sub(/^feature\//i, '').gsub('_', ' ')

    if branch_section == 'Fixed' && branch_name_without_prefix.start_with?("bugfix")
      branch_name_without_prefix = branch_name_without_prefix.sub(/^bugfix\//i, '')
    end

    if branch_section == 'Fixed' && branch_name_without_prefix.start_with?("hotfix")
          branch_name_without_prefix = branch_name_without_prefix.sub(/^hotfix\//i, '')
    end

    section_marker = "### #{branch_section}"
    section_start = changelog_content.downcase.index(section_marker.downcase)
    hotfix = ""
    if branch_name.downcase.start_with?('hotfix')
        hotfix = "[Hotfix]"
    end
    if section_start
      section_end = changelog_content.downcase.index("\n##", section_start + section_marker.length)
      section_content = changelog_content[section_start...section_end]
      unless section_content.downcase.include?(branch_name_without_prefix.downcase)
        updated_section_content = "#{section_content}\n- #{hotfix} **_#{branch_name_without_prefix}_**."
        changelog_content[section_start...section_end] = updated_section_content
      end
    else
      UI.error("Section '#{branch_section}' not found in changelog")
    end
  else
    UI.error("No matching section for branch name '#{branch_name}'")
  end

  File.write(changelog_file_path, changelog_content)

  UI.success("Changelog generated at #{changelog_file_path}")
end

lane :reset_changelog do
  changelog_file_path = '../CHANGELOG.md'

  sections_to_add = ['### Added', '### Fixed', '### Changed']

  cleaned_changelog = sections_to_add.join("\n")

  File.write(changelog_file_path, cleaned_changelog)

  UI.success("Changelog cleaned and sections added at #{changelog_file_path}")
end

lane :generate_release_section do |options|
  # Retrieve parameters from options
  release_name = options[:release_name] || ''  # Use provided release name or empty string
  today_date = Time.now.strftime('%Y-%m-%d')

  # Define the path to your changelog file
  changelog_file_path = '../CHANGELOG.md'  # Specify your desired path

  # Retrieve the last commit hash
  head_commit_hash = `git rev-parse HEAD`.strip

  # Read the existing changelog content
  changelog_content = File.read(changelog_file_path)

  # Remove existing release section(s) from the beginning
  changelog_content.gsub!(/^## .+\n\n.*\nHash [0-9a-f]+/, '') if changelog_content.match?(/^## .+\n\n.*\nHash [0-9a-f]+/)

  # Create or update the release section
  release_section = "## #{release_name}\n\n" if release_name != ''
  build_info = "[Build #{head_commit_hash}] - #{today_date}\nHash #{head_commit_hash}"

  # Add release section at the top
  changelog_content = "#{release_section}#{changelog_content}"

  # Update or add build info at the bottom
  if changelog_content.match(/\[Build [^\]]+\] - \d{4}-\d{2}-\d{2}\nHash [0-9a-f]+/)
    changelog_content.gsub!(/\[Build [^\]]+\] - \d{4}-\d{2}-\d{2}\nHash [0-9a-f]+/, build_info)
  else
    changelog_content += "\n\n#{build_info}\n\n"
  end

  # Write the updated changelog content back to the file
  File.write(changelog_file_path, changelog_content)

  UI.success("Release section updated at #{changelog_file_path}")
end


task generateChangelog {
    doLast {
        def homeDir = System.getProperty("user.home")
        def flagFile = new File("${homeDir}/.changelog_generated.flag")

        def currentTimeMillis = System.currentTimeMillis()
        def expirationTime = 4 * 60 * 60 * 1000  // 4 hours in milliseconds

        if (!flagFile.exists() || (currentTimeMillis - flagFile.lastModified()) >= expirationTime) {
            def fastlaneCommand = 'fastlane generate_changelog'
            def process = fastlaneCommand.execute()
            process.waitFor()

            // Renew the flag file
            if (flagFile.exists()) {
                flagFile.delete()
            }

            // Set a new expiration time
            flagFile.createNewFile()
            flagFile.setLastModified(currentTimeMillis)
        }
    }
}

// Run the generateChangelog task before the build task
preBuild.dependsOn generateChangelog


















