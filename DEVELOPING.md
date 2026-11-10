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

Creating a tag on the master branch runs the release workflow, which uploads
the signed jar, sources jar, javadoc jar and POM to
[Maven Central](https://central.sonatype.com/) as `dev.dit:command-executor` and releases them
automatically once Central has validated the upload.

A released version is permanent and cannot be changed or removed, so tag only
a commit that is ready to release. The new version resolves from Maven Central
some time after the workflow finishes, once Central has synced it; progress is
shown under [Deployments](https://central.sonatype.com/publishing/deployments)
in the Central Portal.

The workflow reads four repository secrets: `MAVEN_CENTRAL_USERNAME` and
`MAVEN_CENTRAL_PASSWORD` (a Central Portal user token), and `SIGNING_KEY` and
`SIGNING_PASSWORD` (the ASCII-armored GPG private key and its passphrase).

### Checking the publication locally

`scripts/verify-publication.sh command-executor` publishes to a throwaway
local repository and checks everything Maven Central requires: the sources and
javadoc jars and the POM name, description, URL, license, developer and SCM
entries. It also fails if a build file references the retired `dit-maven`
repository. It runs on every pull request. Signatures are checked as well
when `ORG_GRADLE_PROJECT_signingInMemoryKey` is set.
