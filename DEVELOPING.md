# Project Development

For general information about contributing changes, see the
[Contributor Guidelines](https://github.dev/dit/.github/blob/master/CONTRIBUTING.md).

## How it Works
 
It's a very lightweight wrapper around the java process commands.

## Building

Run `gradle build`.

## Testing

Tests are run automatically as part of `gradle build`, but can also be
explicitly run via `gradle `.

## Releasing

Creating a tag on the master branch runs the release workflow, which:

1. Publishes the jar to the Dit maven bucket (`https://maven.dit.dev`), kept
   while the ecosystem migrates to Maven Central.
2. Uploads the signed jar, sources jar, javadoc jar and POM to
   [Maven Central](https://central.sonatype.com/) as `dev.dit:command-executor`.

The Maven Central upload is not released automatically. Open
[Deployments](https://central.sonatype.com/publishing/deployments) in the
Central Portal, check the validated deployment, and click **Publish**. A
published version is permanent and cannot be changed or removed.

The workflow reads four repository secrets: `MAVEN_CENTRAL_USERNAME` and
`MAVEN_CENTRAL_PASSWORD` (a Central Portal user token), and `SIGNING_KEY` and
`SIGNING_PASSWORD` (the ASCII-armored GPG private key and its passphrase).

### Checking the publication locally

`scripts/verify-publication.sh command-executor` publishes to a throwaway
local repository and checks everything Maven Central requires: the sources and
javadoc jars and the POM name, description, URL, license, developer and SCM
entries. It runs on every pull request. Signatures are checked as well when
`ORG_GRADLE_PROJECT_signingInMemoryKey` is set.
