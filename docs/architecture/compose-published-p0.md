# P0: independent Compose library consumer (staged)

Default behavior **has not changed**: normal Puanla builds still use the verified git SHA in `.github/compose-design-system.sha` and the adjacent `:design-system` source module. Existing scoring, Room and OCR behavior is untouched.

After the Compose repository publishes a real Maven version, validate it separately with:

```sh
./gradlew :app:assembleDebug test :app:assembleDebugAndroidTest -PcomposePublishedVersion=0.1.0
```

Before a public release, test the locally staged Compose Maven repository with -PcomposePublishedVersion=0.1.0-SNAPSHOT and -PcomposePublishedRepositoryDir=/absolute/path/to/compose-design-system/build/p0-maven-repository. Once published, the public Maven repository is credential-free:
`https://raw.githubusercontent.com/knigdelioglu/compose-design-system/maven-repository/`.
The property switches the consuming build to `com.knigdelioglu:compose-design-system:<version>`, with POM/Gradle metadata and **without** including the shared project source. It is an explicit opt-in until acceptance tests on a real device and in CI are green.

Do not use the existing `compose-design-system.sha` auto-promotion job to update the Maven version; it manages the legacy source checkout only. Migration to automatic version updates is a separate release-gated change. If the published artifact is unavailable or incompatible, remove the `composePublishedVersion` property to restore the prior known-working path.

Acceptance: compile both debug and release, run unit and emulator UI tests, check public API resolution and evaluate scoring, OCR review, group evaluation and report flows. A source-code-only build is not proof of Maven compatibility.
