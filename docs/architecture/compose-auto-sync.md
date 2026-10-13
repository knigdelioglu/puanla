# Compose Design System → Puanla: versioned release contract

Puanla no longer compiles arbitrary upstream `main` source commits or automatically promotes a source SHA every hour.

The reproducible Android dependency is pinned in `gradle.properties` as `composeDesignSystemVersion=0.1.0`. The AAR, POM and Gradle metadata come from the public Compose `maven-repository` branch. `settings.gradle.kts` includes an external `:design-system` source project **only for an explicit emergency rollback**.

## Controlled update
1. Compose publishes an immutable version only after its own tests **and Plan/Puanla emulator tests using the exact staged AAR** succeed.
2. A Puanla version-bump PR changes `composeDesignSystemVersion`, not the old source SHA.
3. Puanla CI compiles debug and release APKs, runs unit and tablet emulator UI tests.
4. Only accepted `main` builds are published as debug preview APKs.

## Manual verification
In Actions → Android CI → Run workflow, provide an optional `compose_published_version` to exercise a candidate already available in public Maven. It does not promote the dependency on its own.

## Emergency rollback
To use the previous pinned source checkout, explicitly run:

```sh
./gradlew :app:assembleDebug -PcomposeLegacySources=true -PcomposeDesignSystemDir=/absolute/path/to/compose-design-system/design-system
```

The `.github/compose-design-system.sha` value remains for this fallback and is not advanced automatically. Normal binary rollbacks should instead pin the last known-good Maven version. Neither path modifies rubrics, Room schema, student data or OCR rules.
